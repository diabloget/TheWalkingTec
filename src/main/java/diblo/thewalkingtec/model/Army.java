package diblo.thewalkingtec.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Representa el ejército de defensas del jugador.
 * Esta clase gestiona la capacidad máxima (espacios) y la lista
 * de defensas activas que posee el jugador.
 */
public class Army implements Serializable {
    private static final long serialVersionUID = 1L;

    private int maxCapacity; // Capacidad máxima de espacios
    private List<Defense> defenses;

    public Army(int initialCapacity) {
        this.maxCapacity = initialCapacity;
        this.defenses = new ArrayList<>();
    }

    /**
     * Intenta añadir una defensa al ejército.
     * Comprueba si hay suficiente capacidad (espacios) disponible.
     *
     * @param defense La defensa a añadir.
     * @return true si se añadió exitosamente, false si no hay capacidad.
     */
    public boolean addDefense(Defense defense) {
        int usedSpaces = getUsedSpaces();
        int requiredSpaces = defense.getFields(); // 'fields' son los espacios que ocupa

        if (usedSpaces + requiredSpaces > maxCapacity) {
            return false; // No hay suficiente capacidad
        }

        defenses.add(defense);
        return true;
    }

    /**
     * Remueve una defensa del ejército (por objeto).
     * @param defense El objeto Defense a remover.
     * @return true si se removió.
     */
    public boolean removeDefense(Defense defense) {
        return defenses.remove(defense);
    }

    /**
     * Remueve una defensa del ejército usando su UUID.
     * @param id El UUID de la defensa a remover.
     * @return true si se encontró y removió.
     */
    public boolean removeDefenseById(UUID id) {
        // Usa removeIf para encontrar la defensa por su ID y borrarla
        return defenses.removeIf(d -> d.getId().equals(id));
    }

    /**
     * Calcula los espacios actualmente usados por todas las defensas.
     * @return La suma de los 'fields' (espacios) de todas las defensas.
     */
    public int getUsedSpaces() {
        // Suma los 'fields' (espacios) de cada defensa en la lista
        return defenses.stream()
                .mapToInt(Defense::getFields)
                .sum();
    }

    /**
     * Calcula los espacios disponibles en el ejército.
     * @return Capacidad máxima - espacios usados.
     */
    public int getAvailableSpaces() {
        return maxCapacity - getUsedSpaces();
    }

    /**
     * Verifica si hay capacidad para una defensa específica.
     * @param defense La defensa que se desea comprobar.
     * @return true si hay espacio suficiente.
     */
    public boolean hasCapacityFor(Defense defense) {
        return getAvailableSpaces() >= defense.getFields();
    }

    /**
     * Aumenta la capacidad máxima del ejército (ej. al subir de nivel).
     * @param amount La cantidad de espacios a añadir.
     */
    public void increaseCapacity(int amount) {
        maxCapacity += amount;
    }

    /**
     * Limpia la lista de defensas, removiendo todas las que estén destruidas.
     */
    public void cleanupDestroyed() {
        defenses.removeIf(Defense::isDestroyed);
    }

    /**
     * Vacía el ejército por completo.
     * Se usa al pasar de nivel para resetear las defensas del jugador.
     */
    public void clear() {
        this.defenses.clear();
    }

    /**
     * Obtiene el número total de defensas que no están destruidas.
     * @return Conteo de defensas activas.
     */
    public int getActiveDefenseCount() {
        return (int) defenses.stream()
                .filter(d -> !d.isDestroyed())
                .count();
    }

    /**
     * Obtiene una defensa específica por su ID.
     * @param id El UUID a buscar.
     * @return La Defensa encontrada, o null si no existe.
     */
    public Defense getDefenseById(UUID id) {
        return defenses.stream()
                .filter(d -> d.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // --- Getters y Setters ---

    public int getMaxCapacity() { return maxCapacity; }

    /**
     * Devuelve una COPIA de la lista de defensas para evitar modificaciones externas.
     */
    public List<Defense> getDefenses() { return new ArrayList<>(defenses); }

    public int getDefenseCount() { return defenses.size(); }

    public void setMaxCapacity(int capacity) { this.maxCapacity = capacity; }

    @Override
    public String toString() {
        return String.format("Army [%d/%d espacios usados, %d defensas activas]",
                getUsedSpaces(), maxCapacity, getActiveDefenseCount());
    }
}