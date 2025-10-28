package diblo.thewalkingtec.model;

import diblo.thewalkingtec.model.enums.ComponentType;
import diblo.thewalkingtec.service.GameContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase base abstracta para todos los componentes del juego (defensas y zombies)
 */
public abstract class Component implements Runnable, Serializable {
    private static final long serialVersionUID = 1L;

    protected String id;
    protected String name;
    protected String imagePath;
    protected int maxLife;
    protected int currentLife;
    protected int damagePerHit;
    protected double hitsPerSecond;
    protected int fields;
    protected int level;
    protected int appearanceLevel;
    protected ComponentType type;
    protected Position position;
    protected boolean isDestroyed;
    protected List<LogEntry> interactionsLog;
    protected transient GameContext context;

    public Component(String id, String name, ComponentType type, int maxLife, int damage,
                     double hitsPerSecond, int fields, int appearanceLevel, String imagePath) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.maxLife = maxLife;
        this.currentLife = maxLife;
        this.damagePerHit = damage;
        this.hitsPerSecond = hitsPerSecond;
        this.fields = fields;
        this.appearanceLevel = appearanceLevel;
        this.level = 1;
        this.isDestroyed = false;
        this.interactionsLog = new ArrayList<>();
        this.imagePath = imagePath;
    }

    @Override
    public void run() {
        if (!isDestroyed && context != null) {
            try {
                onTick(context);
            } catch (Exception e) {
                System.err.println("Error en tick de " + name + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public abstract void onTick(GameContext ctx);

    public void receiveDamage(int damage, String attackerId, String attackerName) {
        int lifeBefore = currentLife;
        currentLife = Math.max(0, currentLife - damage);

        if (attackerId != null) {
            LogEntry entry = new LogEntry(attackerId, this.id, attackerName, this.name, damage, lifeBefore, currentLife);
            interactionsLog.add(entry);
        }

        if (currentLife <= 0) {
            isDestroyed = true;
        }
    }

    protected void logAttack(String defenderId, String defenderName, int damage,
                             int defenderLifeBefore, int defenderLifeAfter) {
        LogEntry entry = new LogEntry(this.id, defenderId, this.name, defenderName, damage, defenderLifeBefore, defenderLifeAfter);
        interactionsLog.add(entry);
    }

    public double getLifePercentage() {
        return maxLife > 0 ? (double) currentLife / maxLife : 0;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getImagePath() { return imagePath; }
    public int getMaxLife() { return maxLife; }
    public int getCurrentLife() { return currentLife; }
    public int getDamagePerHit() { return damagePerHit; }
    public double getHitsPerSecond() { return hitsPerSecond; }
    public int getFields() { return fields; }
    public int getLevel() { return level; }
    public int getAppearanceLevel() { return appearanceLevel; }
    public ComponentType getType() { return type; }
    public Position getPosition() { return position; }
    public boolean isDestroyed() { return isDestroyed; }
    public List<LogEntry> getInteractionsLog() { return new ArrayList<>(interactionsLog); }

    public void setMaxLife(int maxLife) {
        this.maxLife = maxLife;
    }

    public void setCurrentLife(int currentLife) {
        this.currentLife = currentLife;
    }

    // Setters
    public void setPosition(Position position) { this.position = position; }
    public void setContext(GameContext context) { this.context = context; }
    public void setLevel(int level) { this.level = level; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}
