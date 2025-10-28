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
 * Gestor centralizado de configuración del juego
 */
public class ConfigurationManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static GameConfig gameConfig;

    public static GameConfig getConfig() {
        return gameConfig;
    }

    public static void loadConfig(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            throw new IOException("Archivo de configuración no encontrado: " + path);
        }

        try (FileReader reader = new FileReader(path)) {
            gameConfig = GSON.fromJson(reader, GameConfig.class);

            if (gameConfig == null) {
                throw new IOException("El archivo de configuración está vacío o corrupto");
            }

            validateConfig();
            Logger.info("Configuración cargada desde: " + path);
        }
    }

    public static void createDefaultConfig(String path) throws IOException {
        gameConfig = new GameConfig();
        gameConfig.setDefenses(createDefaultDefenses());
        gameConfig.setEnemies(createDefaultEnemies());
        gameConfig.setLevels(createDefaultLevels());

        try (FileWriter writer = new FileWriter(path)) {
            GSON.toJson(gameConfig, writer);
            Logger.info("Configuración por defecto creada en: " + path);
        }
    }

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
     * CORREGIDO: Valores y IDs actualizados para coincidir con tu config.json
     * y se añadió .setFields()
     */
    private static List<DefenseConfig> createDefaultDefenses() {
        List<DefenseConfig> defenses = new ArrayList<>();
        String basePath = "diblo/thewalkingtec/images/";

        // Muro con alambre (barbed_wire)
        DefenseConfig barbedWire = new DefenseConfig();
        barbedWire.setId("barbed_wire"); // <-- CORREGIDO
        barbedWire.setName("Muro con alambre"); // <-- CORREGIDO
        barbedWire.setType("Block");
        barbedWire.setBaseHealth(300); // <-- CORREGIDO
        barbedWire.setBaseDamage(0); // Tu JSON dice 0, pero tu post anterior 5. Ajusta si es necesario.
        barbedWire.setRange(0);
        barbedWire.setCost(25); // <-- CORREGIDO
        barbedWire.setFields(1); // <-- AÑADIDO
        barbedWire.setUnlockLevel(1);
        barbedWire.setImagePath(basePath + "barbed_wire.png");
        defenses.add(barbedWire);

        // Muro simple
        DefenseConfig simpleWall = new DefenseConfig();
        simpleWall.setId("wall"); // <-- CORREGIDO
        simpleWall.setName("Muro Simple");
        simpleWall.setType("Block");
        simpleWall.setBaseHealth(600);
        simpleWall.setBaseDamage(0);
        simpleWall.setRange(0);
        simpleWall.setCost(40); // <-- CORREGIDO
        simpleWall.setFields(2); // <-- AÑADIDO
        simpleWall.setUnlockLevel(1);
        simpleWall.setImagePath(basePath + "wall.png");
        defenses.add(simpleWall);

        // Torreta básica
        DefenseConfig basicTurret = new DefenseConfig();
        basicTurret.setId("turret"); // <-- CORREGIDO
        basicTurret.setName("Torreta"); // <-- CORREGIDO
        basicTurret.setType("Ranged");
        basicTurret.setBaseHealth(200);
        basicTurret.setBaseDamage(30);
        basicTurret.setRange(3);
        basicTurret.setCost(50); // <-- CORREGIDO
        basicTurret.setFields(1); // <-- AÑADIDO
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
        mediumTurret.setCost(75); // <-- CORREGIDO
        mediumTurret.setFields(1); // <-- AÑADIDO
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
        drone.setRange(0); // El Dron ataca por contacto/sobre la celda
        drone.setCost(50); // <-- CORREGIDO
        drone.setFields(1); // <-- AÑADIDO
        drone.setUnlockLevel(2);
        drone.setImagePath(basePath + "drone.png");
        defenses.add(drone);

        return defenses;
    }

    /**
     * CORREGIDO: Se añadió .setFields()
     */
    private static List<EnemyConfig> createDefaultEnemies() {
        List<EnemyConfig> enemies = new ArrayList<>();
        String basePath = "diblo/thewalkingtec/images/";

        // Zombie básico
        EnemyConfig basicZombie = new EnemyConfig();
        basicZombie.setId("zombie_basic");
        basicZombie.setName("Caminante");
        basicZombie.setType("Contact");
        basicZombie.setAiType("SEEK_NEAREST");
        basicZombie.setBaseHealth(120);
        basicZombie.setBaseDamage(15);
        basicZombie.setSpeed(0.5);
        basicZombie.setCost(1);
        basicZombie.setFields(1); // <-- AÑADIDO
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
        runner.setFields(1); // <-- AÑADIDO
        runner.setUnlockLevel(3);
        runner.setImagePath(basePath + "runner.png");
        enemies.add(runner);

        // Zombie volador (aéreo)
        EnemyConfig flyer = new EnemyConfig();
        flyer.setId("zombie_flyer");
        flyer.setName("Volador");
        flyer.setType("Aerial");
        flyer.setAiType("CRASH");
        flyer.setBaseHealth(60);
        flyer.setBaseDamage(20);
        flyer.setSpeed(1.5);
        flyer.setCost(2);
        flyer.setFields(1); // <-- AÑADIDO
        flyer.setUnlockLevel(5);
        flyer.setImagePath(basePath + "zombie_flyer.png");
        enemies.add(flyer);

        return enemies;
    }

    private static List<LevelConfig> createDefaultLevels() {
        List<LevelConfig> levels = new ArrayList<>();

        // MÍNIMO 10 NIVELES según requisitos
        for (int i = 1; i <= 10; i++) {
            LevelConfig level = new LevelConfig();
            level.setLevelNumber(i);

            // El ejército inicia con 20 y crece 5 por nivel
            level.setPlayerArmySize(20 + ((i - 1) * 5));

            // Dinero inicial aumenta progresivamente
            level.setStartingMoney(500 + (i * 150)); // Tu JSON usa valores diferentes, ajusta si prefieres

            // Crecimiento de stats - será aleatorio en runtime
            level.setDefenseBoostPercent((i - 1) * 3.0);
            level.setEnemyBoostPercent((i - 1) * 4.0);

            List<WaveConfig> waves = new ArrayList<>();

            // Primera oleada - zombies básicos (siempre)
            WaveConfig wave1 = new WaveConfig();
            wave1.setZombieId("zombie_basic");
            wave1.setQuantity(8 + (i * 3));
            wave1.setDelaySeconds(0);
            waves.add(wave1);

            // Segunda oleada - corredores (a partir del nivel 3)
            if (i >= 3) {
                WaveConfig wave2 = new WaveConfig();
                wave2.setZombieId("zombie_runner");
                wave2.setQuantity(4 + (i - 2) * 2);
                wave2.setDelaySeconds(20);
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

            // Oleada final masiva (niveles 7+)
            if (i >= 7) {
                WaveConfig wave4 = new WaveConfig();
                wave4.setZombieId("zombie_basic");
                wave4.setQuantity(10 + (i - 6) * 2);
                wave4.setDelaySeconds(50);
                waves.add(wave4);
            }

            level.setEnemyWaves(waves);
            levels.add(level);
        }

        return levels;
    }
}