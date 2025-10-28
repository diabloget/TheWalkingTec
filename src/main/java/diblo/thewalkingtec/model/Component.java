package diblo.thewalkingtec.model;

import diblo.thewalkingtec.model.enums.ComponentType;
import diblo.thewalkingtec.service.GameContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase base abstracta para todos los componentes del juego (Defensas y Zombies).
 * Implementa Runnable para que cada componente pueda actuar en su propio hilo
 * (manejado por un ExecutorService en la clase Game).
 * Implementa Serializable para poder guardar el estado del componente.
 */
public abstract class Component implements Runnable, Serializable {
    private static final long serialVersionUID = 1L;

    // --- Atributos de Configuración (leídos de config.json) ---
    protected String id;
    protected String name;
    protected String imagePath;
    protected int maxLife;
    protected int damagePerHit;
    protected double hitsPerSecond;
    protected int fields;
    protected int appearanceLevel;
    protected ComponentType type;

    // --- Atributos de Estado (guardados en partida.json) ---
    protected int currentLife;
    protected Position position;
    protected boolean isDestroyed;
    protected List<LogEntry> interactionsLog;
    protected int level;

    /**
     * El contexto del juego (tablero, jugador, etc.).
     * Es 'transient' para que no se serialize al guardar la partida.
     * Debe ser re-inyectado usando setContext() al cargar.
     */
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

    /**
     * Método principal del hilo (Runnable).
     * Se ejecuta en cada "tick" del juego (controlado por el ExecutorService).
     */
    @Override
    public void run() {
        // Solo actúa si no está destruido y si tiene un contexto válido
        if (!isDestroyed && context != null) {
            try {
                onTick(context); // Llama a la lógica específica (Zombie o Defense)
            } catch (Exception e) {
                System.err.println("Error en tick de " + name + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Método abstracto que define la lógica de este componente en cada tick.
     * (Ej. moverse, buscar objetivo, atacar).
     *
     * @param ctx El contexto actual del juego.
     */
    public abstract void onTick(GameContext ctx);

    /**
     * Aplica daño a este componente y registra la interacción.
     * Marca el componente como destruido si la vida llega a 0.
     *
     * @param damage El daño a recibir.
     * @param attackerId ID del atacante (para el log).
     * @param attackerName Nombre del atacante (para el log).
     */
    public void receiveDamage(int damage, String attackerId, String attackerName) {
        int lifeBefore = currentLife;
        currentLife = Math.max(0, currentLife - damage); // Evita vida negativa

        if (attackerId != null) {
            // Registra el ataque recibido
            LogEntry entry = new LogEntry(attackerId, this.id, attackerName, this.name, damage, lifeBefore, currentLife);
            interactionsLog.add(entry);
        }

        if (currentLife <= 0) {
            isDestroyed = true; // Marca para ser eliminado del tablero
        }
    }

    /**
     * Método helper para que la subclase (Zombie, Defense) registre un ataque HECHO.
     */
    protected void logAttack(String defenderId, String defenderName, int damage,
                             int defenderLifeBefore, int defenderLifeAfter) {
        LogEntry entry = new LogEntry(this.id, defenderId, this.name, defenderName, damage, defenderLifeBefore, defenderLifeAfter);
        interactionsLog.add(entry);
    }

    /**
     * Calcula el porcentaje de vida restante (para la barra de vida).
     * @return Un valor entre 0.0 y 1.0.
     */
    public double getLifePercentage() {
        return maxLife > 0 ? (double) currentLife / maxLife : 0;
    }

    // --- Getters ---
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
    /**
     * Devuelve una COPIA de la lista de logs para evitar modificaciones externas.
     */
    public List<LogEntry> getInteractionsLog() { return new ArrayList<>(interactionsLog); }

    // --- Setters ---
    public void setPosition(Position position) { this.position = position; }
    public void setContext(GameContext context) { this.context = context; } // Usado al cargar partida
    public void setLevel(int level) { this.level = level; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public void setMaxLife(int maxLife) {
        this.maxLife = maxLife;
    }
    public void setCurrentLife(int currentLife) {
        this.currentLife = currentLife;
    }
}