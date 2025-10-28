package diblo.thewalkingtec.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Registra una interacción de combate única entre componentes.
 * Esta clase es inmutable y se usa para cumplir el requisito del PDF
 * de mostrar un log de combate.
 */
public class LogEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    // Formato para mostrar la hora del evento en el log
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    private final String attackerId;
    private final String defenderId;
    private final String attackerName;
    private final String defenderName;
    private final int damage;
    private final int lifeBefore; // Vida del defensor ANTES del golpe
    private final int lifeAfter;  // Vida del defensor DESPUÉS del golpe
    private final long timestamp;  // Hora en que ocurrió el evento

    /**
     * Crea una nueva entrada de log de combate.
     *
     * @param attackerId ID del atacante.
     * @param defenderId ID del defensor (puede ser null si es la Reliquia).
     * @param attackerName Nombre del atacante.
     * @param defenderName Nombre del defensor.
     * @param damage Daño infligido.
     * @param lifeBefore Vida del defensor antes del golpe.
     * @param lifeAfter Vida del defensor después del golpe.
     */
    public LogEntry(String attackerId, String defenderId, String attackerName,
                    String defenderName, int damage, int lifeBefore, int lifeAfter) {
        this.attackerId = attackerId;
        this.defenderId = defenderId;
        this.attackerName = attackerName;
        this.defenderName = defenderName;
        this.damage = damage;
        this.lifeBefore = lifeBefore;
        this.lifeAfter = lifeAfter;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters

    public String getAttackerId() {
        return attackerId;
    }

    public String getDefenderId() {
        return defenderId;
    }

    /**
     * Comprueba si este golpe resultó en la eliminación del defensor.
     * @return true si la vida después del golpe es 0 o menos.
     */
    public boolean wasKilled() {
        return lifeAfter <= 0;
    }

    /**
     * Genera una representación en String formateada para mostrar en la UI.
     * @return String formateado del log.
     */
    @Override
    public String toString() {
        String time = DATE_FORMAT.format(new Date(timestamp));
        String killed = wasKilled() ? " [ELIMINADO]" : ""; // Añade si el golpe fue fatal
        return String.format("[%s] %s atacó a %s | Daño: %d | Vida: %d → %d%s",
                time, attackerName, defenderName, damage, lifeBefore, lifeAfter, killed);
    }
}