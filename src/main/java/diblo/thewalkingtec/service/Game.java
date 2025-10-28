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
 * Motor principal del juego.
 * Gestiona el estado de la partida (Player, Board, Nivel), el bucle de juego (gameTick)
 * y la lógica de concurrencia para el movimiento de componentes.
 * Implementa Serializable para guardar y cargar la partida.
 */
public class Game implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- Constantes del Juego ---
    private static final int TICKS_PER_SECOND = 10; // 10 actualizaciones de lógica por segundo
    private static final int TICK_INTERVAL_MS = 1000 / TICKS_PER_SECOND; // (100ms)
    public static final int RELIC_BASE_LIFE = 1000;
    private static final int THREAD_POOL_SIZE = 10; // Hilos para mover componentes

    // --- Estado Principal (Serializado) ---
    private Board board;
    private Player player;
    private int currentLevelIndex;
    private Position relicPosition;
    private int relicLife;
    private double currentDefenseBoost; // Boost de este nivel
    private double currentEnemyBoost;   // Boost de este nivel
    private boolean running;
    private boolean paused;
    private int zombiesToSpawnInWave; // Cuántos zombies faltan en la oleada
    private int zombiesSpawnedInWave; // Cuántos ya se han generado
    private int currentWaveIndex;
    private long lastSpawnTime;
    private long waveDelayTimer; // Temporizador para retraso entre oleadas

    // --- Componentes Transitorios (No serializados, se recrean) ---
    private transient GameContext context;
    private transient PathfindingService pathfindingService;
    private transient GameConfig gameConfig; // Se obtiene de ConfigurationManager
    private transient ScheduledExecutorService gameLoopExecutor; // Ejecuta el gameTick
    private transient ExecutorService componentExecutor; // Ejecuta el .run() de cada componente
    private transient List<GameEventListener> listeners; // Para notificar a la UI

    /**
     * Crea una nueva instancia del juego.
     * @param playerName El nombre del jugador.
     * @throws IllegalStateException Si ConfigurationManager no se ha cargado primero.
     */
    public Game(String playerName) {
        // Validación crítica: El config debe estar cargado ANTES de crear un Game
        this.gameConfig = ConfigurationManager.getConfig();
        if (this.gameConfig == null) {
            throw new IllegalStateException("GameConfig no está inicializado. Asegúrate de cargar la configuración antes de crear Game.");
        }

        this.board = new Board();
        this.player = new Player(playerName, 20, 500); // Valores por defecto
        this.pathfindingService = new PathfindingService();
        this.relicPosition = new Position(Board.SIZE / 2, Board.SIZE / 2); // Centro
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

    /**
     * Inicia (o reanuda) el bucle principal del juego.
     * @param isNewGame true si es una partida nueva, false si se carga de un guardado.
     */
    public void start(boolean isNewGame) {
        if (running) {
            Logger.warning("Intento de iniciar un juego que ya está corriendo");
            return;
        }

        running = true;
        paused = false;

        // Reconstruye los componentes transitorios
        if (gameLoopExecutor == null) gameLoopExecutor = Executors.newSingleThreadScheduledExecutor();
        if (componentExecutor == null) componentExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        if (listeners == null) listeners = new ArrayList<>();

        if (isNewGame) {
            startLevel(currentLevelIndex); // Inicia el nivel 0
        }

        // Inicia el bucle de juego
        gameLoopExecutor.scheduleAtFixedRate(this::gameTick, 0, TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);
        notifyGameStarted();
        Logger.info("Juego iniciado (nuevo: " + isNewGame + ")");
    }

    /**
     * El corazón del juego. Se ejecuta cada TICK_INTERVAL_MS (100ms).
     * Gestiona el spawn, la ejecución de la IA de componentes y la limpieza.
     */
    private void gameTick() {
        if (!running || paused) return; // No hace nada si está pausado o detenido

        try {
            // 1. Lógica de Spawning
            spawnZombies();

            // 2. Lógica de Componentes (Concurrente)
            List<Component> activeComponents = new ArrayList<>();
            activeComponents.addAll(board.getActiveDefenses());
            activeComponents.addAll(board.getActiveZombies());

            // Inyecta el contexto actual a todos
            for (Component component : activeComponents) {
                component.setContext(context);
            }

            // Crea una tarea (Callable) para el .run() de cada componente
            List<Callable<Void>> tasks = new ArrayList<>();
            for (Component component : activeComponents) {
                tasks.add(() -> {
                    component.run(); // Llama a onTick() -> move() y attack()
                    return null;
                });
            }

            // Ejecuta todas las tareas en el pool de hilos y espera a que terminen
            componentExecutor.invokeAll(tasks);

            // 3. Limpieza y Verificación
            board.cleanupDestroyedComponents(); // Remueve muertos del tablero
            player.getArmy().cleanupDestroyed(); // Remueve defensas muertas del ejército

            // 4. Comprueba condiciones de victoria/derrota
            checkLevelConditions();

        } catch (Exception e) {
            Logger.error("Error en game tick", e);
        }
    }

    /**
     * Gestiona la lógica de generación de zombies para la oleada actual.
     */
    private void spawnZombies() {
        long currentTime = System.currentTimeMillis();
        LevelConfig currentLevel = getCurrentLevel();

        // Si hay un delay entre oleadas, solo descuenta el timer y retorna
        if (waveDelayTimer > 0) {
            waveDelayTimer -= TICK_INTERVAL_MS;
            return;
        }

        // Si aún faltan zombies por spawnear en esta oleada
        if (zombiesSpawnedInWave < zombiesToSpawnInWave) {
            // Spawnea un zombie cada segundo
            if (currentTime - lastSpawnTime >= 1000) {
                WaveConfig wave = currentLevel.getEnemyWaves().get(currentWaveIndex);
                Zombie zombie = createZombie(wave.getZombieId());
                if (zombie != null) {
                    Position spawnPos = board.getRandomEdgePosition(); // Posición aleatoria en el borde
                    if (board.placeComponent(zombie, spawnPos)) {
                        zombiesSpawnedInWave++;
                        lastSpawnTime = currentTime;
                        notifyZombieSpawned(zombie); // Notifica a la UI
                    }
                }
            }
        }
    }

    /**
     * Verifica si se ha perdido (reliquia destruida) o si se ha ganado la oleada/nivel.
     */
    private void checkLevelConditions() {
        // Condición de Derrota
        if (isRelicDestroyed()) {
            gameLost();
            return;
        }

        // Condición de Victoria de Oleada
        // Si ya se spawnearon todos Y no quedan zombies activos
        if (zombiesSpawnedInWave >= zombiesToSpawnInWave && board.getActiveZombies().isEmpty()) {
            // Si quedan más oleadas en este nivel
            if (currentWaveIndex + 1 < getCurrentLevel().getEnemyWaves().size()) {
                startNextWave();
            } else {
                // Si era la última oleada
                levelCompleted();
            }
        }
    }

    /**
     * Configura el estado del juego para un nivel específico.
     * @param levelIndex El índice del nivel (de la lista en config.json).
     */
    public void startLevel(int levelIndex) {
        if (levelIndex >= gameConfig.getLevels().size()) {
            // Si se acabaron los 10 niveles
            notifyMaxLevelReached();
            return;
        }

        currentLevelIndex = levelIndex;
        LevelConfig level = getCurrentLevel();

        // Crecimiento aleatorio 5-20% (requisito PDF)
        currentDefenseBoost = level.getDefenseBoostPercent() / 100.0 + RandomUtils.randomDouble(0.05, 0.20);
        currentEnemyBoost = level.getEnemyBoostPercent() / 100.0 + RandomUtils.randomDouble(0.05, 0.20);

        Logger.info(String.format("Nivel %d - Boost Defensas: %.2f%%, Boost Enemigos: %.2f%%",
                level.getLevelNumber(), currentDefenseBoost * 100, currentEnemyBoost * 100));

        // Configura al jugador según el nivel
        player.getArmy().setMaxCapacity(level.getPlayerArmySize());
        player.setCoins(level.getStartingMoney());

        board.clear(); // Limpia el tablero
        healRelic(); // Restaura la vida de la reliquia
        player.getArmy().clear(); // Limpia el ejército

        currentWaveIndex = -1; // Resetea el contador de oleadas
        startNextWave(); // Inicia la primera oleada (o la siguiente)

        notifyLevelStarted(level);
        Logger.info("Nivel " + level.getLevelNumber() + " iniciado");
    }

    /**
     * Avanza al siguiente índice de oleada y configura los contadores de spawn.
     */
    private void startNextWave() {
        currentWaveIndex++;
        WaveConfig wave = getCurrentLevel().getEnemyWaves().get(currentWaveIndex);
        zombiesToSpawnInWave = wave.getQuantity();
        zombiesSpawnedInWave = 0;
        waveDelayTimer = wave.getDelaySeconds() * 1000L; // Asigna el delay (si lo hay)
        Logger.info("Oleada " + (currentWaveIndex + 1) + " iniciada: " + zombiesToSpawnInWave + " zombies");
    }

    /**
     * Factory method para crear un Zombie basado en su ID del config.
     * @param zombieId El ID (ej. "zombie_basic").
     * @return Una instancia de Zombie con el boost del nivel aplicado.
     */
    public Zombie createZombie(String zombieId) {
        EnemyConfig enemyConfig = gameConfig.getEnemies().stream()
                .filter(ec -> ec.getId().equals(zombieId))
                .findFirst()
                .orElse(null);
        if (enemyConfig != null) {
            return new Zombie(enemyConfig, currentEnemyBoost); // Aplica boost de enemigo
        }
        Logger.warning("No se encontró enemigo con ID: " + zombieId);
        return null;
    }

    /**
     * Factory method para crear una Defensa basada en su ID del config.
     * @param defenseId El ID (ej. "turret").
     * @return Una instancia de Defense con el boost del nivel aplicado.
     */
    public Defense createDefense(String defenseId) {
        var defenseConfig = gameConfig.getDefenses().stream()
                .filter(dc -> dc.getId().equals(defenseId))
                .findFirst()
                .orElse(null);
        if (defenseConfig != null) {
            return new Defense(defenseConfig, currentDefenseBoost); // Aplica boost de defensa
        }
        Logger.warning("No se encontró defensa con ID: " + defenseId);
        return null;
    }

    /**
     * Se llama cuando se completa la última oleada.
     */
    private void levelCompleted() {
        Logger.info("Nivel " + getCurrentLevel().getLevelNumber() + " completado");

        // Da puntos por nivel completado
        int levelBonus = (currentLevelIndex + 1) * 100;
        player.addScore(levelBonus);

        notifyLevelCompleted(getCurrentLevel());
        pause(); // Pausa el juego para mostrar la pantalla de victoria
    }

    /**
     * Inicia el siguiente nivel. Llamado por la UI después de que el jugador
     * presiona "Siguiente Nivel" en la pantalla de victoria.
     */
    public void nextLevel() {
        if (currentLevelIndex + 1 < gameConfig.getLevels().size()) {
            startLevel(currentLevelIndex + 1);
            resume();
        } else {
            // Si era el último nivel (ej. Nivel 10)
            gameWon();
        }
    }

    /**
     * Lógica para que el jugador coloque una defensa en el tablero.
     * Valida monedas y espacio.
     */
    public boolean placeDefense(Defense defense, Position position) {
        // Esta validación es solo para la UI, la lógica real está en Player
        if (player.getCoins() < defense.getCost()) {
            Logger.warning("No hay suficientes monedas para colocar defensa");
            return false;
        }

        if (board.placeComponent(defense, position)) {
            // Cobra al jugador y añade al ejército (lógica en Player)
            player.placeDefense(defense);
            notifyDefensePlaced(defense, position);
            Logger.info("Defensa colocada: " + defense.getName() + " en " + position);
            return true;
        }

        return false;
    }

    /**
     * Lógica para que el jugador "venda" una defensa.
     */
    public void removeDefense(Defense defense) {
        board.removeComponent(defense);
        player.removeDefense(defense); // El jugador recupera 50% del costo
        notifyDefenseRemoved(defense);
        Logger.info("Defensa removida: " + defense.getName());
    }

    /**
     * Se llama cuando el jugador gana el último nivel.
     */
    private void gameWon() {
        Logger.info("¡Juego ganado!");
        stop();
        notifyGameWon();
    }

    /**
     * Se llama cuando la reliquia es destruida.
     */
    private void gameLost() {
        Logger.info("Juego perdido - Reliquia destruida");
        stop();
        notifyGameLost();
    }

    /**
     * Detiene permanentemente los hilos del bucle de juego.
     */
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

    // --- Getters ---
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

    // --- Setters ---
    public void setRelicLife(int relicLife) { this.relicLife = relicLife; }
    public void setCurrentLevelIndex(int currentLevelIndex) { this.currentLevelIndex = currentLevelIndex; }

    /**
     * Aplica daño a la reliquia (sincronizado).
     */
    public synchronized void damageRelic(int damage) {
        this.relicLife = Math.max(0, this.relicLife - damage);
    }

    public void healRelic() {
        this.relicLife = RELIC_BASE_LIFE;
    }

    public boolean isRelicDestroyed() {
        return this.relicLife <= 0;
    }

    /**
     * Método especial de deserialización.
     * Reconstruye todos los campos 'transient' que se perdieron al guardar.
     */
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();

        // Reconstruir componentes transitorios
        this.pathfindingService = new PathfindingService();
        this.gameConfig = ConfigurationManager.getConfig(); // Obtiene el config global
        this.context = new GameContext(this.board, this.player, this.relicPosition, this.pathfindingService, this);
        this.listeners = new ArrayList<>();

        if (this.gameConfig == null) {
            throw new IOException("No se pudo restaurar GameConfig después de deserializar. Asegúrate de que ConfigurationManager esté cargado.");
        }

        Logger.info("Game deserializado correctamente");
    }

    // --- Notificaciones de Eventos (para la UI) ---

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

    /**
     * Interfaz de listener para que la UI (GameRenderer) reaccione
     * a eventos del motor del juego (Game).
     */
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
        default void onMaxLevelReached() {} // Evento para cuando se acaban los niveles
    }
}