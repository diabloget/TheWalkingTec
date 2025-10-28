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
 * Gestor estático para guardar y cargar el estado de una partida (Game)
 * en formato JSON (serialización manual).
 */
public class SaveManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Guarda el estado actual del juego en un archivo JSON.
     *
     * @param game El objeto Game a guardar.
     * @param filePath La ruta del archivo (ej. "partida.json").
     * @return true si se guardó con éxito.
     */
    public static boolean saveGame(Game game, String filePath) {
        try {
            // 1. Convierte el objeto Game a un objeto simple (GameSaveData)
            GameSaveData saveData = new GameSaveData(game);
            // 2. Serializa GameSaveData usando Gson
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

    /**
     * Carga un estado de juego desde un archivo JSON.
     *
     * @param filePath La ruta del archivo (ej. "partida.json").
     * @return El objeto Game restaurado, o null si falló.
     */
    public static Game loadGame(String filePath) {
        try {
            // Validación: La configuración global DEBE estar cargada antes de cargar una partida
            if (ConfigurationManager.getConfig() == null) {
                Logger.error("Configuración no cargada. No se puede restaurar la partida.");
                return null;
            }

            GameSaveData saveData;
            // 1. Deserializa el JSON a un objeto GameSaveData
            try (FileReader reader = new FileReader(filePath)) {
                saveData = GSON.fromJson(reader, GameSaveData.class);
            }

            if (saveData == null || !saveData.isValid()) {
                Logger.error("El archivo de guardado está corrupto o incompleto");
                return null;
            }

            // 2. Convierte el objeto simple (GameSaveData) de vuelta a un objeto Game complejo
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
     * Clase interna privada que representa la estructura de datos simple
     * que se guarda/carga en el archivo JSON.
     */
    private static class GameSaveData {
        // Datos del Jugador y Nivel
        String playerName;
        int playerLevel;
        int playerCoins;
        int playerScore;
        int playerCapacityBase;
        int currentLevelIndex;
        int relicLife;
        // Lista de todos los componentes (Zombies y Defensas) en el tablero
        List<ComponentData> activeComponents;

        /**
         * Constructor que "aplana" un objeto Game en esta estructura simple.
         */
        public GameSaveData(Game game) {
            this.playerName = game.getPlayer().getName();
            this.playerLevel = game.getPlayer().getLevel();
            this.playerCoins = game.getPlayer().getCoins();
            this.playerScore = game.getPlayer().getScore();
            this.playerCapacityBase = game.getPlayer().getCapacityBase();
            this.currentLevelIndex = game.getCurrentLevelIndex();
            this.relicLife = game.getRelicLife();
            this.activeComponents = new ArrayList<>();

            // Guardar defensas (vivas o muertas)
            game.getBoard().getActiveDefenses().forEach(c ->
                    activeComponents.add(new ComponentData(c))
            );

            // Guardar zombies (vivos o muertos)
            game.getBoard().getActiveZombies().forEach(c ->
                    activeComponents.add(new ComponentData(c))
            );

            Logger.info("GameSaveData creado: " + activeComponents.size() + " componentes guardados");
        }

        /**
         * Validador simple para el archivo cargado.
         */
        public boolean isValid() {
            return playerName != null &&
                    !playerName.isEmpty() &&
                    activeComponents != null &&
                    currentLevelIndex >= 0;
        }

        /**
         * "Infla" este objeto de datos simple en un objeto Game funcional.
         * @return El objeto Game restaurado.
         */
        public Game toGame() {
            // 1. Crear nuevo juego (esto carga el config.json)
            Game game = new Game(playerName);

            // 2. Restaurar estado del jugador y juego
            game.getPlayer().setLevel(playerLevel);
            game.getPlayer().setCoins(playerCoins);
            game.getPlayer().setScore(playerScore);
            game.setCurrentLevelIndex(currentLevelIndex);
            game.setRelicLife(relicLife);
            game.getPlayer().getArmy().setMaxCapacity(playerCapacityBase); // Restaura capacidad
            game.getPlayer().setCapacityBase(playerCapacityBase); // Restaura capacidad base

            GameConfig config = game.getGameConfig();

            // 3. Restaurar componentes
            for (ComponentData data : activeComponents) {
                // Re-crea el componente (Zombie o Defense) usando el config.json
                // Se usa 0.0 para el boost, ya que la vida (max y current) se restaura manualmente.
                Component component = createComponentFromData(data, config, game, 0.0);

                if (component != null && data.position != null) {
                    // Restaura estado (vida, posición)
                    component.setPosition(data.position);
                    component.setMaxLife(data.maxLife);
                    component.setCurrentLife(data.currentLife);

                    // Coloca en el tablero
                    if (game.getBoard().placeComponent(component, data.position)) {
                        // Si es defensa, también se añade al Army del jugador
                        if (component instanceof Defense) {
                            game.getPlayer().getArmy().addDefense((Defense) component);
                        }
                    } else {
                        Logger.warning("No se pudo colocar componente restaurado: " + data.id + " en " + data.position);
                    }
                }
            }

            Logger.info("Game restaurado: " + activeComponents.size() + " componentes restaurados");
            return game;
        }

        /**
         * Factory method para recrear un Component (Zombie o Defense)
         * usando su ID y el GameConfig cargado.
         */
        private Component createComponentFromData(ComponentData data, GameConfig config, Game game, double customBoost) {

            // Buscar primero en defensas
            DefenseConfig defenseConfig = config.getDefenses().stream()
                    .filter(dc -> dc.getId().equals(data.id))
                    .findFirst()
                    .orElse(null);

            if (defenseConfig != null) {
                // Se usa customBoost (0.0) porque la vida se restaura manualmente
                return new Defense(defenseConfig, customBoost);
            }

            // Buscar en enemigos
            EnemyConfig enemyConfig = config.getEnemies().stream()
                    .filter(ec -> ec.getId().equals(data.id))
                    .findFirst()
                    .orElse(null);

            if (enemyConfig != null) {
                return new Zombie(enemyConfig, customBoost);
            }

            Logger.warning("No se encontró configuración para componente: " + data.id);
            return null;
        }
    }

    /**
     * Información mínima necesaria para reconstruir un componente.
     */
    private static class ComponentData {
        String id; // ID base (ej. "turret", "zombie_basic")
        Position position;
        int currentLife;
        int maxLife;

        // Constructor "aplanador"
        public ComponentData(Component c) {
            this.id = c.getId();
            this.position = c.getPosition();
            this.currentLife = c.getCurrentLife();
            this.maxLife = c.getMaxLife();
        }
    }
}