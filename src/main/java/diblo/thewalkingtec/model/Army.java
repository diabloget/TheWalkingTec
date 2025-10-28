package diblo.thewalkingtec.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Representa el ejército de defensas del jugador
 */
public class Army implements Serializable {
    private static final long serialVersionUID = 1L;

    private int maxCapacity; // capacidad máxima de espacios
    private List<Defense> defenses;

    public Army(int initialCapacity) {
        this.maxCapacity = initialCapacity;
        this.defenses = new ArrayList<>();
    }

    /**
     * Intenta añadir una defensa al ejército
     * @return true si se añadió exitosamente, false si no hay capacidad
     */
    public boolean addDefense(Defense defense) {
        int usedSpaces = getUsedSpaces();
        int requiredSpaces = defense.getFields();

        if (usedSpaces + requiredSpaces > maxCapacity) {
            return false; // No hay suficiente capacidad
        }

        defenses.add(defense);
        return true;
    }

    /**
     * Remueve una defensa del ejército
     */
    public boolean removeDefense(Defense defense) {
        return defenses.remove(defense);
    }

    /**
     * Remueve una defensa por su ID
     */
    public boolean removeDefenseById(UUID id) {
        return defenses.removeIf(d -> d.getId().equals(id));
    }

    /**
     * Calcula los espacios actualmente usados
     */
    public int getUsedSpaces() {
        return defenses.stream()
                .mapToInt(Defense::getFields)
                .sum();
    }

    /**
     * Calcula los espacios disponibles
     */
    public int getAvailableSpaces() {
        return maxCapacity - getUsedSpaces();
    }

    /**
     * Verifica si hay capacidad para una defensa
     */
    public boolean hasCapacityFor(Defense defense) {
        return getAvailableSpaces() >= defense.getFields();
    }

    /**
     * Aumenta la capacidad máxima del ejército
     */
    public void increaseCapacity(int amount) {
        maxCapacity += amount;
    }

    /**
     * Limpia todas las defensas destruidas
     */
    public void cleanupDestroyed() {
        defenses.removeIf(Defense::isDestroyed);
    }

    /**
     * Obtiene el número total de defensas activas
     */
    public int getActiveDefenseCount() {
        return (int) defenses.stream()
                .filter(d -> !d.isDestroyed())
                .count();
    }

    /**
     * Obtiene una defensa por su ID
     */
    public Defense getDefenseById(UUID id) {
        return defenses.stream()
                .filter(d -> d.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // Getters
    public int getMaxCapacity() { return maxCapacity; }
    public List<Defense> getDefenses() { return new ArrayList<>(defenses); }
    public int getDefenseCount() { return defenses.size(); }

    // Setters
    public void setMaxCapacity(int capacity) { this.maxCapacity = capacity; }

    @Override
    public String toString() {
        return String.format("Army [%d/%d espacios usados, %d defensas activas]",
                getUsedSpaces(), maxCapacity, getActiveDefenseCount());
    }
}