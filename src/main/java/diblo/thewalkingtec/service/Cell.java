package diblo.thewalkingtec.service;

import diblo.thewalkingtec.model.Component;
import diblo.thewalkingtec.model.Position;
import diblo.thewalkingtec.model.enums.ComponentType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Representa una celda individual en el tablero, capaz de contener múltiples componentes.
 */
public class Cell implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Position position;
    private final List<Component> occupants;

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

    public List<Component> getOccupants() {
        return new ArrayList<>(occupants);
    }

    public Position getPosition() {
        return position;
    }

    /**
     * Verifica si la celda contiene un componente terrestre (no aéreo).
     */
    public boolean hasGroundOccupant() {
        return occupants.stream().anyMatch(c -> !c.getType().isAerial());
    }

    /**
     * Verifica si la celda contiene un componente aéreo.
     */
    public boolean hasAerialOccupant() {
        return occupants.stream().anyMatch(c -> c.getType().isAerial());
    }

    /**
     * Obtiene todos los componentes terrestres en la celda.
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
        String occupantNames = occupants.stream()
                .map(Component::getName)
                .collect(Collectors.joining(", "));
        return "Cell at " + position + " occupied by " + occupantNames;
    }
}