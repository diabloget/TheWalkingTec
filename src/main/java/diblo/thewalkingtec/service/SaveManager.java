package diblo.thewalkingtec.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import diblo.thewalkingtec.model.Component;
import diblo.thewalkingtec.model.Defense;
import diblo.thewalkingtec.model.Position;
import diblo.thewalkingtec.model.Zombie;
import diblo.thewalkingtec.model.config.DefenseConfig;
import diblo.thewalkingtec.model.config.EnemyConfig;
import diblo.thewalkingtec.model.config.GameConfig;
import diblo.thewalkingtec.util.Logger;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor para guardar y cargar partidas en formato JSON
 */
public class SaveManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean saveGame(Game game, String filePath) {
        try {
            GameSaveData saveData = new GameSaveData(game);
            try (FileWriter writer = new FileWriter(filePath)) {
                GSON.toJson(saveData, writer);
            }
            Logger.info("Partida guardada en: " + filePath);
            return true;
        } catch (Exception e) {
            Logger.error("Error al guardar la partida", e);
            return false;
        }
    }

    public static Game loadGame(String filePath) {
        try {
            // Validar que la configuración esté cargada
            if (ConfigurationManager.getConfig() == null) {
                Logger.error("Configuración no cargada. No se puede restaurar la partida.");
                return null;
            }

            GameSaveData saveData;
            try (FileReader reader = new FileReader(filePath)) {
                saveData = GSON.fromJson(reader, GameSaveData.class);
            }

            if (saveData == null || !saveData.isValid()) {
                Logger.error("El archivo de guardado está corrupto o incompleto");
                return null;
            }

            Game game = saveData.toGame();
            Logger.info("Partida cargada desde: " + filePath);
            return game;

        } catch (JsonSyntaxException e) {
            Logger.error("Error de sintaxis en el JSON: " + e.getMessage());
            return null;
        } catch (Exception e) {
            Logger.error("Error al cargar la partida", e);
            return null;
        }
    }

    /**
     * Estructura de datos para serializar/deserializar partidas
     */
    private static class GameSaveData {
        String playerName;
        int playerLevel;
        int playerCoins;
        int playerScore;
        int playerCapacityBase;
        int currentLevelIndex;
        int relicLife;
        List<ComponentData> activeComponents;

        public GameSaveData(Game game) {
            this.playerName = game.getPlayer().getName();
            this.playerLevel = game.getPlayer().getLevel();
            this.playerCoins = game.getPlayer().getCoins();
            this.playerScore = game.getPlayer().getScore();
            this.playerCapacityBase = game.getPlayer().getCapacityBase();
            this.currentLevelIndex = game.getCurrentLevelIndex();
            this.relicLife = game.getRelicLife();
            this.activeComponents = new ArrayList<>();

            // Guardar defensas
            game.getBoard().getActiveDefenses().forEach(c ->
                    activeComponents.add(new ComponentData(c))
            );

            // Guardar zombies
            game.getBoard().getActiveZombies().forEach(c ->
                    activeComponents.add(new ComponentData(c))
            );

            Logger.info("GameSaveData creado: " + activeComponents.size() + " componentes guardados");
        }

        public boolean isValid() {
            return playerName != null &&
                    !playerName.isEmpty() &&
                    activeComponents != null &&
                    currentLevelIndex >= 0;
        }

        public Game toGame() {
            // Crear nuevo juego con los datos guardados
            Game game = new Game(playerName);

            // Restaurar estado del jugador
            game.getPlayer().setLevel(playerLevel);
            game.getPlayer().setCoins(playerCoins);
            game.getPlayer().setScore(playerScore);
            game.setCurrentLevelIndex(currentLevelIndex);
            game.setRelicLife(relicLife);


            // --- LÍNEAS FALTANTES (AÑADIR ESTAS DOS) ---
            game.getPlayer().getArmy().setMaxCapacity(playerCapacityBase);
            game.getPlayer().setCapacityBase(playerCapacityBase);
            // --- FIN ---

            GameConfig config = game.getGameConfig();

            // Restaurar componentes
            for (ComponentData data : activeComponents) {
                Component component = createComponentFromData(data, config, game, 0.0);

                if (component != null && data.position != null) {
                    component.setPosition(data.position);

                    // Aplicar daño para restaurar vida actual
                    component.setMaxLife(data.maxLife);
                    component.setCurrentLife(data.currentLife);

                    // Colocar en el tablero
                    if (game.getBoard().placeComponent(component, data.position)) {
                        if (component instanceof Defense) {
                            game.getPlayer().getArmy().addDefense((Defense) component);
                        }
                    } else {
                        Logger.warning("No se pudo colocar componente: " + data.id + " en " + data.position);
                    }
                }
            }

            Logger.info("Game restaurado: " + activeComponents.size() + " componentes restaurados");
            return game;
        }

        // En SaveManager.java -> GameSaveData
// Cambia la firma del método:
        private Component createComponentFromData(ComponentData data, GameConfig config, Game game, double customBoost) {

            // Buscar primero en defensas
            DefenseConfig defenseConfig = config.getDefenses().stream()
                    .filter(dc -> dc.getId().equals(data.id))
                    .findFirst()
                    .orElse(null);

            if (defenseConfig != null) {
                // Usamos el customBoost (0.0) en lugar de recalcularlo
                return new Defense(defenseConfig, customBoost);
            }

            // Buscar en enemigos
            EnemyConfig enemyConfig = config.getEnemies().stream()
                    .filter(ec -> ec.getId().equals(data.id))
                    .findFirst()
                    .orElse(null);

            if (enemyConfig != null) {
                // Usamos el customBoost (0.0) en lugar de recalcularlo
                return new Zombie(enemyConfig, customBoost);
            }

            Logger.warning("No se encontró configuración para componente: " + data.id);
            return null;
        }
    }

    /**
     * Información mínima necesaria para reconstruir un componente
     */
    private static class ComponentData {
        String id;
        Position position;
        int currentLife;
        int maxLife;

        public ComponentData(Component c) {
            this.id = c.getId();
            this.position = c.getPosition();
            this.currentLife = c.getCurrentLife();
            this.maxLife = c.getMaxLife();
        }
    }
}