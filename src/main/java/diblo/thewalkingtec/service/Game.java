package diblo.thewalkingtec.service;

import diblo.thewalkingtec.model.*;
import diblo.thewalkingtec.model.config.EnemyConfig;
import diblo.thewalkingtec.model.config.GameConfig;
import diblo.thewalkingtec.model.config.LevelConfig;
import diblo.thewalkingtec.model.config.WaveConfig;
import diblo.thewalkingtec.util.Logger;
import diblo.thewalkingtec.util.RandomUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

/**
 * Motor principal del juego
 */
public class Game implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int TICKS_PER_SECOND = 10;
    private static final int TICK_INTERVAL_MS = 1000 / TICKS_PER_SECOND;
    public static final int RELIC_BASE_LIFE = 1000;
    private static final int THREAD_POOL_SIZE = 10;

    private Board board;
    private Player player;
    private transient GameContext context;
    private transient PathfindingService pathfindingService;
    private transient GameConfig gameConfig;
    private int currentLevelIndex;
    private Position relicPosition;
    private int relicLife;

    // Crecimiento aleatorio por nivel
    private double currentDefenseBoost;
    private double currentEnemyBoost;

    // Concurrencia
    private transient ScheduledExecutorService gameLoopExecutor;
    private transient ExecutorService componentExecutor;
    private boolean running;
    private boolean paused;

    // Spawning de zombies
    private int zombiesToSpawnInWave;
    private int zombiesSpawnedInWave;
    private int currentWaveIndex;
    private long lastSpawnTime;
    private long waveDelayTimer;

    // Listeners
    private transient List<GameEventListener> listeners;

    public Game(String playerName) {
        // VALIDACIÓN CRÍTICA
        this.gameConfig = ConfigurationManager.getConfig();
        if (this.gameConfig == null) {
            throw new IllegalStateException("GameConfig no está inicializado. Asegúrate de cargar la configuración antes de crear Game.");
        }

        this.board = new Board();
        this.player = new Player(playerName, 20, 500); // Inicia con 20 espacios
        this.pathfindingService = new PathfindingService();
        this.relicPosition = new Position(Board.SIZE / 2, Board.SIZE / 2);
        this.relicLife = RELIC_BASE_LIFE;
        this.context = new GameContext(board, player, relicPosition, pathfindingService, this);
        this.currentLevelIndex = 0;
        this.running = false;
        this.paused = true;
        this.listeners = new ArrayList<>();
        this.currentDefenseBoost = 0;
        this.currentEnemyBoost = 0;

        Logger.info("Game creado para jugador: " + playerName);
    }

    public void start(boolean isNewGame) {
        if (running) {
            Logger.warning("Intento de iniciar un juego que ya está corriendo");
            return;
        }

        running = true;
        paused = false;

        if (gameLoopExecutor == null) gameLoopExecutor = Executors.newSingleThreadScheduledExecutor();
        if (componentExecutor == null) componentExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        if (listeners == null) listeners = new ArrayList<>();

        if (isNewGame) {
            startLevel(currentLevelIndex);
        }

        gameLoopExecutor.scheduleAtFixedRate(this::gameTick, 0, TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);
        notifyGameStarted();
        Logger.info("Juego iniciado (nuevo: " + isNewGame + ")");
    }

    private void gameTick() {
        if (!running || paused) return;

        try {
            spawnZombies();

            List<Component> activeComponents = new ArrayList<>();
            activeComponents.addAll(board.getActiveDefenses());
            activeComponents.addAll(board.getActiveZombies());

            for (Component component : activeComponents) {
                component.setContext(context);
            }

            List<Callable<Void>> tasks = new ArrayList<>();
            for (Component component : activeComponents) {
                tasks.add(() -> {
                    component.run();
                    return null;
                });
            }

            componentExecutor.invokeAll(tasks);

            board.cleanupDestroyedComponents();
            player.getArmy().cleanupDestroyed();

            checkLevelConditions();

        } catch (Exception e) {
            Logger.error("Error en game tick", e);
        }
    }

    private void spawnZombies() {
        long currentTime = System.currentTimeMillis();
        LevelConfig currentLevel = getCurrentLevel();

        if (waveDelayTimer > 0) {
            waveDelayTimer -= TICK_INTERVAL_MS;
            return;
        }

        if (zombiesSpawnedInWave < zombiesToSpawnInWave) {
            if (currentTime - lastSpawnTime >= 1000) {
                WaveConfig wave = currentLevel.getEnemyWaves().get(currentWaveIndex);
                Zombie zombie = createZombie(wave.getZombieId());
                if (zombie != null) {
                    Position spawnPos = board.getRandomEdgePosition();
                    if (board.placeComponent(zombie, spawnPos)) {
                        zombiesSpawnedInWave++;
                        lastSpawnTime = currentTime;
                        notifyZombieSpawned(zombie);
                    }
                }
            }
        }
    }

    private void checkLevelConditions() {
        if (isRelicDestroyed()) {
            gameLost();
            return;
        }

        if (zombiesSpawnedInWave >= zombiesToSpawnInWave && board.getActiveZombies().isEmpty()) {
            if (currentWaveIndex + 1 < getCurrentLevel().getEnemyWaves().size()) {
                startNextWave();
            } else {
                levelCompleted();
            }
        }
    }

    public void startLevel(int levelIndex) {
        if (levelIndex >= gameConfig.getLevels().size()) {
            // Si no hay más niveles configurados, preguntar si crear más
            notifyMaxLevelReached();
            return;
        }

        currentLevelIndex = levelIndex;
        LevelConfig level = getCurrentLevel();

        // CRECIMIENTO ALEATORIO 5-20% según requisitos
        currentDefenseBoost = level.getDefenseBoostPercent() / 100.0 + RandomUtils.randomDouble(0.05, 0.20);
        currentEnemyBoost = level.getEnemyBoostPercent() / 100.0 + RandomUtils.randomDouble(0.05, 0.20);

        Logger.info(String.format("Nivel %d - Boost Defensas: %.2f%%, Boost Enemigos: %.2f%%",
                level.getLevelNumber(), currentDefenseBoost * 100, currentEnemyBoost * 100));

        player.getArmy().setMaxCapacity(level.getPlayerArmySize());
        player.setCoins(level.getStartingMoney());

        board.clear();
        healRelic();

        currentWaveIndex = -1;
        startNextWave();

        notifyLevelStarted(level);
        Logger.info("Nivel " + level.getLevelNumber() + " iniciado");
    }

    private void startNextWave() {
        currentWaveIndex++;
        WaveConfig wave = getCurrentLevel().getEnemyWaves().get(currentWaveIndex);
        zombiesToSpawnInWave = wave.getQuantity();
        zombiesSpawnedInWave = 0;
        waveDelayTimer = wave.getDelaySeconds() * 1000L;
        Logger.info("Oleada " + (currentWaveIndex + 1) + " iniciada: " + zombiesToSpawnInWave + " zombies");
    }

    public Zombie createZombie(String zombieId) {
        EnemyConfig enemyConfig = gameConfig.getEnemies().stream()
                .filter(ec -> ec.getId().equals(zombieId))
                .findFirst()
                .orElse(null);
        if (enemyConfig != null) {
            return new Zombie(enemyConfig, currentEnemyBoost);
        }
        Logger.warning("No se encontró enemigo con ID: " + zombieId);
        return null;
    }

    public Defense createDefense(String defenseId) {
        var defenseConfig = gameConfig.getDefenses().stream()
                .filter(dc -> dc.getId().equals(defenseId))
                .findFirst()
                .orElse(null);
        if (defenseConfig != null) {
            return new Defense(defenseConfig, currentDefenseBoost);
        }
        Logger.warning("No se encontró defensa con ID: " + defenseId);
        return null;
    }

    private void levelCompleted() {
        Logger.info("Nivel " + getCurrentLevel().getLevelNumber() + " completado");

        // Dar puntos por nivel completado
        int levelBonus = (currentLevelIndex + 1) * 100;
        player.addScore(levelBonus);

        notifyLevelCompleted(getCurrentLevel());
        pause();
    }

    public void nextLevel() {
        if (currentLevelIndex + 1 < gameConfig.getLevels().size()) {
            startLevel(currentLevelIndex + 1);
            resume();
        } else {
            gameWon();
        }
    }

    public boolean placeDefense(Defense defense, Position position) {
        if (player.getCoins() < defense.getCost()) {
            Logger.warning("No hay suficientes monedas para colocar defensa");
            return false;
        }

        if (board.placeComponent(defense, position)) {
            player.spendCoins(defense.getCost());
            player.getArmy().addDefense(defense);
            notifyDefensePlaced(defense, position);
            Logger.info("Defensa colocada: " + defense.getName() + " en " + position);
            return true;
        }

        return false;
    }

    public void removeDefense(Defense defense) {
        board.removeComponent(defense);
        player.getArmy().removeDefense(defense);
        player.addCoins(defense.getCost() / 2);
        notifyDefenseRemoved(defense);
        Logger.info("Defensa removida: " + defense.getName());
    }

    private void gameWon() {
        Logger.info("¡Juego ganado!");
        stop();
        notifyGameWon();
    }

    private void gameLost() {
        Logger.info("Juego perdido - Reliquia destruida");
        stop();
        notifyGameLost();
    }

    public void stop() {
        this.running = false;
        if (this.gameLoopExecutor != null) {
            this.gameLoopExecutor.shutdownNow();
            this.gameLoopExecutor = null;
        }
        if (this.componentExecutor != null) {
            this.componentExecutor.shutdownNow();
            this.componentExecutor = null;
        }
        Logger.info("Juego detenido");
    }

    public void pause() {
        this.paused = true;
        notifyGamePaused();
        Logger.info("Juego pausado");
    }

    public void resume() {
        this.paused = false;
        notifyGameResumed();
        Logger.info("Juego reanudado");
    }

    public void addGameEventListener(GameEventListener listener) {
        if (listeners == null) listeners = new ArrayList<>();
        listeners.add(listener);
    }

    // Getters
    public Board getBoard() { return board; }
    public Player getPlayer() { return player; }
    public GameContext getContext() { return context; }
    public LevelConfig getCurrentLevel() { return gameConfig.getLevels().get(currentLevelIndex); }
    public GameConfig getGameConfig() { return gameConfig; }
    public int getCurrentLevelIndex() { return currentLevelIndex; }
    public boolean isRunning() { return running; }
    public boolean isPaused() { return paused; }
    public Position getRelicPosition() { return relicPosition; }
    public int getRelicLife() { return relicLife; }
    public int getMaxRelicLife() { return RELIC_BASE_LIFE; }
    public double getCurrentDefenseBoost() { return currentDefenseBoost; }
    public double getCurrentEnemyBoost() { return currentEnemyBoost; }

    // Setters
    public void setRelicLife(int relicLife) { this.relicLife = relicLife; }
    public void setCurrentLevelIndex(int currentLevelIndex) { this.currentLevelIndex = currentLevelIndex; }

    public synchronized void damageRelic(int damage) {
        this.relicLife = Math.max(0, this.relicLife - damage);
    }

    public void healRelic() {
        this.relicLife = RELIC_BASE_LIFE;
    }

    public boolean isRelicDestroyed() {
        return this.relicLife <= 0;
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();

        // Reconstruir componentes transient
        this.pathfindingService = new PathfindingService();
        this.context = new GameContext(this.board, this.player, this.relicPosition, this.pathfindingService, this);
        this.listeners = new ArrayList<>();
        this.gameConfig = ConfigurationManager.getConfig();

        if (this.gameConfig == null) {
            throw new IOException("No se pudo restaurar GameConfig después de deserializar");
        }

        Logger.info("Game deserializado correctamente");
    }

    // Event notifications
    private void notifyGameStarted() {
        if (listeners == null) return;
        for (GameEventListener listener : listeners) listener.onGameStarted();
    }

    private void notifyLevelStarted(LevelConfig level) {
        if (listeners == null) return;
        for (GameEventListener listener : listeners) listener.onLevelStarted(level);
    }

    private void notifyLevelCompleted(LevelConfig level) {
        if (listeners == null) return;
        for (GameEventListener listener : listeners) listener.onLevelCompleted(level);
    }

    private void notifyGameWon() {
        if (listeners == null) return;
        for (GameEventListener listener : listeners) listener.onGameWon();
    }

    private void notifyGameLost() {
        if (listeners == null) return;
        for (GameEventListener listener : listeners) listener.onGameLost();
    }

    private void notifyGamePaused() {
        if (listeners == null) return;
        for (GameEventListener listener : listeners) listener.onGamePaused();
    }

    private void notifyGameResumed() {
        if (listeners == null) return;
        for (GameEventListener listener : listeners) listener.onGameResumed();
    }

    private void notifyZombieSpawned(Zombie zombie) {
        if (listeners == null) return;
        for (GameEventListener listener : listeners) listener.onZombieSpawned(zombie);
    }

    private void notifyDefensePlaced(Defense defense, Position position) {
        if (listeners == null) return;
        for (GameEventListener listener : listeners) listener.onDefensePlaced(defense, position);
    }

    private void notifyDefenseRemoved(Defense defense) {
        if (listeners == null) return;
        for (GameEventListener listener : listeners) listener.onDefenseRemoved(defense);
    }

    private void notifyMaxLevelReached() {
        if (listeners == null) return;
        for (GameEventListener listener : listeners) listener.onMaxLevelReached();
    }

    @Override
    public String toString() {
        return String.format("Game [Nivel: %d, Jugador: %s, Reliquia: %d/%d]",
                currentLevelIndex + 1, player.getName(), relicLife, RELIC_BASE_LIFE);
    }

    public interface GameEventListener extends Serializable {
        default void onGameStarted() {}
        default void onLevelStarted(LevelConfig level) {}
        default void onLevelCompleted(LevelConfig level) {}
        default void onGameWon() {}
        default void onGameLost() {}
        default void onGamePaused() {}
        default void onGameResumed() {}
        default void onZombieSpawned(Zombie zombie) {}
        default void onDefensePlaced(Defense defense, Position position) {}
        default void onDefenseRemoved(Defense defense) {}
        default void onMaxLevelReached() {}
    }
}