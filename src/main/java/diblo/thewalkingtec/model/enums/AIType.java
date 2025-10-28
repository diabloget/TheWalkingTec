package diblo.thewalkingtec.model.enums;

/**
 * Tipos de comportamiento de IA para zombies
 */
public enum AIType {
    SEEK_NEAREST("Buscar Más Cercano", "Ataca la defensa o reliquia más cercana"),
    RANDOM("Aleatorio", "Se mueve de forma impredecible"),
    CRASH("Kamikaze", "Va directo a la reliquia y explota al contacto");

    private final String displayName;
    private final String description;

    AIType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
