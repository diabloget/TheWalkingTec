package diblo.thewalkingtec.model.enums;

/**
 * Tipos de componentes en el juego
 */
public enum ComponentType {
    // Tipos de Defensas
    CONTACT("Contacto", "Ataca enemigos adyacentes"),
    RANGED("Rango", "Ataca a distancia"),
    AERIAL("Aéreo", "Ataca desde el aire"),
    IMPACT("Impacto", "Causa daño de área"),
    MULTI("Multi", "Ataca múltiples objetivos"),
    BLOCK("Bloque", "Bloquea el paso sin atacar"),

    // Tipos de Zombies
    ZOMBIE_BASIC("Zombie Básico", "Zombie estándar"),
    ZOMBIE_RUNNER("Zombie Corredor", "Zombie rápido"),
    ZOMBIE_TANK("Zombie Tanque", "Zombie resistente"),
    ZOMBIE_EXPLODER("Zombie Explosivo", "Explota al contacto");

    private final String displayName;
    private final String description;

    ComponentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDefense() {
        return this == CONTACT || this == RANGED || this == AERIAL ||
                this == IMPACT || this == MULTI || this == BLOCK;
    }

    public boolean isZombie() {
        return !isDefense();
    }

    public boolean isAerial() {
        return this == AERIAL;
    }
}