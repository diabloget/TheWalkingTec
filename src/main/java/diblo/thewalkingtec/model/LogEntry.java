package diblo.thewalkingtec.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Registra una interacción de combate entre componentes
 */
public class LogEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    private final String attackerId;
    private final String defenderId;
    private final String attackerName;
    private final String defenderName;
    private final int damage;
    private final int lifeBefore;
    private final int lifeAfter;
    private final long timestamp;

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

    public String getAttackerId() {
        return attackerId;
    }

    public String getDefenderId() {
        return defenderId;
    }

    public boolean wasKilled() {
        return lifeAfter <= 0;
    }

    @Override
    public String toString() {
        String time = DATE_FORMAT.format(new Date(timestamp));
        String killed = wasKilled() ? " [ELIMINADO]" : "";
        return String.format("[%s] %s atacó a %s | Daño: %d | Vida: %d → %d%s",
                time, attackerName, defenderName, damage, lifeBefore, lifeAfter, killed);
    }
}