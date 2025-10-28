package diblo.thewalkingtec.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import diblo.thewalkingtec.model.config.*;
import diblo.thewalkingtec.util.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor estático para cargar, mantener y proveer la configuración del juego (config.json).
 * Esta clase debe ser cargada al inicio de la aplicación, antes de crear
 * una instancia de Game.
 */
public class ConfigurationManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static GameConfig gameConfig; // La configuración cargada en memoria

    /**
     * Obtiene la configuración global del juego.
     * @return El objeto GameConfig cargado.
     */
    public static GameConfig getConfig() {
        return gameConfig;
    }

    /**
     * Carga el archivo de configuración desde una ruta específica.
     * @param path La ruta al archivo config.json.
     * @throws IOException Si el archivo no se encuentra o está corrupto.
     */
    public static void loadConfig(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            throw new IOException("Archivo de configuración no encontrado: " + path);
        }

        try (FileReader reader = new FileReader(path)) {
            // Deserializa el JSON a las clases Config
            gameConfig = GSON.fromJson(reader, GameConfig.class);

            if (gameConfig == null) {
                throw new IOException("El archivo de configuración está vacío o corrupto");
            }

            validateConfig(); // Valida que las listas principales no estén vacías
            Logger.info("Configuración cargada desde: " + path);
        }
    }

    /**
     * Crea un archivo config.json por defecto en la ruta especificada.
     * Útil para la primera ejecución si no se encuentra un config.
     *
     * @param path La ruta donde se guardará el nuevo config.json.
     * @throws IOException Si hay un error de escritura.
     */
    public static void createDefaultConfig(String path) throws IOException {
        gameConfig = new GameConfig();
        gameConfig.setDefenses(createDefaultDefenses());
        gameConfig.setEnemies(createDefaultEnemies());
        gameConfig.setLevels(createDefaultLevels());

        try (FileWriter writer = new FileWriter(path)) {
            // Serializa el objeto GameConfig por defecto a JSON
            GSON.toJson(gameConfig, writer);
            Logger.info("Configuración por defecto creada en: " + path);
        }
    }

    /**
     * Valida que la configuración cargada contenga las listas esenciales.
     */
    private static void validateConfig() throws IOException {
        if (gameConfig.getDefenses() == null || gameConfig.getDefenses().isEmpty()) {
            throw new IOException("La configuración no tiene defensas definidas");
        }
        if (gameConfig.getEnemies() == null || gameConfig.getEnemies().isEmpty()) {
            throw new IOException("La configuración no tiene enemigos definidos");
        }
        if (gameConfig.getLevels() == null || gameConfig.getLevels().isEmpty()) {
            throw new IOException("La configuración no tiene niveles definidos");
        }
    }

    /**
     * Genera una lista de Defensas por defecto.
     */
    private static List<DefenseConfig> createDefaultDefenses() {
        List<DefenseConfig> defenses = new ArrayList<>();
        String basePath = "diblo/thewalkingtec/images/"; // Ruta base de imágenes

        // Muro con alambre (barbed_wire)
        DefenseConfig barbedWire = new DefenseConfig();
        barbedWire.setId("barbed_wire");
        barbedWire.setName("Muro con alambre");
        barbedWire.setType("Block");
        barbedWire.setBaseHealth(300);
        barbedWire.setBaseDamage(0);
        barbedWire.setRange(0);
        barbedWire.setCost(25);
        barbedWire.setFields(1); // Importante: define los espacios que ocupa
        barbedWire.setUnlockLevel(1);
        barbedWire.setImagePath(basePath + "barbed_wire.png");
        defenses.add(barbedWire);

        // Muro simple
        DefenseConfig simpleWall = new DefenseConfig();
        simpleWall.setId("wall");
        simpleWall.setName("Muro Simple");
        simpleWall.setType("Block");
        simpleWall.setBaseHealth(600);
        simpleWall.setBaseDamage(0);
        simpleWall.setRange(0);
        simpleWall.setCost(40);
        simpleWall.setFields(2); // Ocupa 2 espacios
        simpleWall.setUnlockLevel(1);
        simpleWall.setImagePath(basePath + "wall.png");
        defenses.add(simpleWall);

        // Torreta básica
        DefenseConfig basicTurret = new DefenseConfig();
        basicTurret.setId("turret");
        basicTurret.setName("Torreta");
        basicTurret.setType("Ranged");
        basicTurret.setBaseHealth(200);
        basicTurret.setBaseDamage(30);
        basicTurret.setRange(3);
        basicTurret.setCost(50);
        basicTurret.setFields(1);
        basicTurret.setUnlockLevel(1);
        basicTurret.setImagePath(basePath + "turret.png");
        defenses.add(basicTurret);

        // Torreta media
        DefenseConfig mediumTurret = new DefenseConfig();
        mediumTurret.setId("turret_medium");
        mediumTurret.setName("Torreta Mejorada");
        mediumTurret.setType("Ranged");
        mediumTurret.setBaseHealth(250);
        mediumTurret.setBaseDamage(45);
        mediumTurret.setRange(4);
        mediumTurret.setCost(75);
        mediumTurret.setFields(1);
        mediumTurret.setUnlockLevel(3);
        mediumTurret.setImagePath(basePath + "turret_medium.png");
        defenses.add(mediumTurret);

        // Dron atacante (aéreo)
        DefenseConfig drone = new DefenseConfig();
        drone.setId("drone");
        drone.setName("Dron Atacante");
        drone.setType("Aerial");
        drone.setBaseHealth(100);
        drone.setBaseDamage(25);
        drone.setRange(0); // Ataca en su misma celda
        drone.setCost(50);
        drone.setFields(1);
        drone.setUnlockLevel(2);
        drone.setImagePath(basePath + "drone.png");
        defenses.add(drone);

        return defenses;
    }

    /**
     * Genera una lista de Enemigos por defecto.
     */
    private static List<EnemyConfig> createDefaultEnemies() {
        List<EnemyConfig> enemies = new ArrayList<>();
        String basePath = "diblo/thewalkingtec/images/";

        // Zombie básico
        EnemyConfig basicZombie = new EnemyConfig();
        basicZombie.setId("zombie_basic");
        basicZombie.setName("Caminante");
        basicZombie.setType("Contact"); // Tipo de componente (para pathfinding)
        basicZombie.setAiType("SEEK_NEAREST"); // Tipo de IA
        basicZombie.setBaseHealth(120);
        basicZombie.setBaseDamage(15);
        basicZombie.setSpeed(0.5); // Movimientos por segundo
        basicZombie.setCost(1); // Recompensa en monedas (no usado actualmente)
        basicZombie.setFields(1); // Espacios (no usado para zombies)
        basicZombie.setUnlockLevel(1);
        basicZombie.setImagePath(basePath + "zombie_basic.png");
        enemies.add(basicZombie);

        // Zombie corredor
        EnemyConfig runner = new EnemyConfig();
        runner.setId("zombie_runner");
        runner.setName("Corredor");
        runner.setType("Contact");
        runner.setAiType("RANDOM");
        runner.setBaseHealth(80);
        runner.setBaseDamage(10);
        runner.setSpeed(1.2);
        runner.setCost(1);
        runner.setFields(1);
        runner.setUnlockLevel(3);
        runner.setImagePath(basePath + "runner.png");
        enemies.add(runner);

        // Zombie volador (aéreo)
        EnemyConfig flyer = new EnemyConfig();
        flyer.setId("zombie_flyer");
        flyer.setName("Volador");
        flyer.setType("Aerial");
        flyer.setAiType("CRASH"); // Va directo a la reliquia
        flyer.setBaseHealth(60);
        flyer.setBaseDamage(20);
        flyer.setSpeed(1.5);
        flyer.setCost(2);
        flyer.setFields(1);
        flyer.setUnlockLevel(5);
        flyer.setImagePath(basePath + "zombie_flyer.png");
        enemies.add(flyer);

        return enemies;
    }

    /**
     * Genera una lista de 10 Niveles por defecto.
     */
    private static List<LevelConfig> createDefaultLevels() {
        List<LevelConfig> levels = new ArrayList<>();

        // Genera 10 niveles por defecto (requisito PDF)
        for (int i = 1; i <= 10; i++) {
            LevelConfig level = new LevelConfig();
            level.setLevelNumber(i);

            // El ejército inicia con 20 y crece 5 por nivel
            level.setPlayerArmySize(20 + ((i - 1) * 5));
            // Dinero inicial aumenta progresivamente
            level.setStartingMoney(500 + (i * 150));
            // Boost base (se suma a un aleatorio en Game.java)
            level.setDefenseBoostPercent((i - 1) * 3.0);
            level.setEnemyBoostPercent((i - 1) * 4.0);

            List<WaveConfig> waves = new ArrayList<>();

            // Primera oleada - zombies básicos (siempre)
            WaveConfig wave1 = new WaveConfig();
            wave1.setZombieId("zombie_basic");
            wave1.setQuantity(8 + (i * 3)); // Cantidad incremental
            wave1.setDelaySeconds(0); // Inicia inmediatamente
            waves.add(wave1);

            // Segunda oleada - corredores (a partir del nivel 3)
            if (i >= 3) {
                WaveConfig wave2 = new WaveConfig();
                wave2.setZombieId("zombie_runner");
                wave2.setQuantity(4 + (i - 2) * 2);
                wave2.setDelaySeconds(20); // Inicia 20s después de la oleada anterior
                waves.add(wave2);
            }

            // Tercera oleada - voladores (a partir del nivel 5)
            if (i >= 5) {
                WaveConfig wave3 = new WaveConfig();
                wave3.setZombieId("zombie_flyer");
                wave3.setQuantity(2 + (i - 4));
                wave3.setDelaySeconds(35);
                waves.add(wave3);
            }

            level.setEnemyWaves(waves);
            levels.add(level);
        }

        return levels;
    }
}