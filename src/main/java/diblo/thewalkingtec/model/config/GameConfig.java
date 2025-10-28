package diblo.thewalkingtec.model.config;

import java.util.List;

public class GameConfig {
    private List<DefenseConfig> defenses;
    private List<EnemyConfig> enemies;
    private List<LevelConfig> levels;

    public List<DefenseConfig> getDefenses() {
        return defenses;
    }

    public void setDefenses(List<DefenseConfig> defenses) {
        this.defenses = defenses;
    }

    public List<EnemyConfig> getEnemies() {
        return enemies;
    }

    public void setEnemies(List<EnemyConfig> enemies) {
        this.enemies = enemies;
    }

    public List<LevelConfig> getLevels() {
        return levels;
    }

    public void setLevels(List<LevelConfig> levels) {
        this.levels = levels;
    }
}