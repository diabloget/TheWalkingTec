package diblo.thewalkingtec.service;

import diblo.thewalkingtec.model.Component;
import diblo.thewalkingtec.model.Position;
import diblo.thewalkingtec.model.enums.ComponentType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Representa una celda individual en el tablero (Board).
 * Es capaz de contener múltiples componentes, permitiendo que
 * un componente aéreo (Dron) y uno terrestre (Torreta) coexistan.
 */
public class Cell implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Position position;
    private final List<Component> occupants; // Lista de componentes en esta celda

    public Cell(Position position) {
        this.position = position;
        this.occupants = new ArrayList<>();
    }

    public boolean isEmpty() {
        return occupants.isEmpty();
    }

    public void addOccupant(Component component) {
        if (!occupants.contains(component)) {
            occupants.add(component);
        }
    }

    public void removeOccupant(Component component) {
        occupants.remove(component);
    }

    public void clear() {
        occupants.clear();
    }

    /**
     * Devuelve una COPIA de la lista de ocupantes.
     */
    public List<Component> getOccupants() {
        return new ArrayList<>(occupants);
    }

    public Position getPosition() {
        return position;
    }

    /**
     * Verifica si la celda contiene al menos un componente terrestre (no aéreo).
     * @return true si hay un componente terrestre.
     */
    public boolean hasGroundOccupant() {
        // Devuelve true si CUALQUIER ocupante NO es aéreo
        return occupants.stream().anyMatch(c -> !c.getType().isAerial());
    }

    /**
     * Verifica si la celda contiene al menos un componente aéreo.
     * @return true si hay un componente aéreo.
     */
    public boolean hasAerialOccupant() {
        // Devuelve true si CUALQUIER ocupante ES aéreo
        return occupants.stream().anyMatch(c -> c.getType().isAerial());
    }

    /**
     * Obtiene todos los componentes terrestres en la celda.
     * @return Una lista de componentes no aéreos.
     */
    public List<Component> getGroundOccupants() {
        return occupants.stream()
                .filter(c -> !c.getType().isAerial())
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "Empty Cell at " + position;
        }
        // Crea un string con los nombres, ej. "Torreta, Dron"
        String occupantNames = occupants.stream()
                .map(Component::getName)
                .collect(Collectors.joining(", "));
        return "Cell at " + position + " occupied by " + occupantNames;
    }
}