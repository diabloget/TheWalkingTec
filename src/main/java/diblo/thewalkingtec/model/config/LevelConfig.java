package diblo.thewalkingtec.model.config;

import java.util.List;

public class LevelConfig {
    private int levelNumber;
    private int playerArmySize;
    private int startingMoney;
    private List<WaveConfig> enemyWaves;
    private double defenseBoostPercent;
    private double enemyBoostPercent;

    // Getters and Setters

    public int getLevelNumber() {
        return levelNumber;
    }

    public void setLevelNumber(int levelNumber) {
        this.levelNumber = levelNumber;
    }

    public int getPlayerArmySize() {
        return playerArmySize;
    }

    public void setPlayerArmySize(int playerArmySize) {
        this.playerArmySize = playerArmySize;
    }

    public int getStartingMoney() {
        return startingMoney;
    }

    public void setStartingMoney(int startingMoney) {
        this.startingMoney = startingMoney;
    }

    public List<WaveConfig> getEnemyWaves() {
        return enemyWaves;
    }

    public void setEnemyWaves(List<WaveConfig> enemyWaves) {
        this.enemyWaves = enemyWaves;
    }

    public double getDefenseBoostPercent() {
        return defenseBoostPercent;
    }

    public void setDefenseBoostPercent(double defenseBoostPercent) {
        this.defenseBoostPercent = defenseBoostPercent;
    }

    public double getEnemyBoostPercent() {
        return enemyBoostPercent;
    }

    public void setEnemyBoostPercent(double enemyBoostPercent) {
        this.enemyBoostPercent = enemyBoostPercent;
    }
}