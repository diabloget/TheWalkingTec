package diblo.thewalkingtec.service;

import diblo.thewalkingtec.model.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Representa el tablero de juego (cuadrícula 25x25).
 * Gestiona la posición de todos los componentes (Zombies y Defensas)
 * y contiene la lógica de movimiento y colisión.
 */
public class Board implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int SIZE = 25; // Tamaño del tablero (25x25)

    private final Cell[][] grid; // La cuadrícula de celdas
    // Listas para acceso rápido a componentes, evitando iterar la cuadrícula
    private final List<Defense> activeDefenses;
    private final List<Zombie> activeZombies;

    public Board() {
        grid = new Cell[SIZE][SIZE];
        activeDefenses = new ArrayList<>();
        activeZombies = new ArrayList<>();

        // Inicializa cada celda en la cuadrícula
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                grid[i][j] = new Cell(new Position(i, j));
            }
        }
    }

    /**
     * Coloca un componente nuevo en el tablero por primera vez (ej. spawn, compra).
     *
     * @param component El componente a colocar (Zombie o Defense).
     * @param pos La posición donde se colocará.
     * @return true si se colocó con éxito, false si la posición es inválida o está bloqueada.
     */
    public boolean placeComponent(Component component, Position pos) {
        if (!isValidPosition(pos)) {
            return false;
        }

        Cell cell = getCell(pos.getX(), pos.getY());

        // Regla de colocación: Las unidades terrestres no pueden apilarse.
        if (!component.getType().isAerial() && cell.hasGroundOccupant()) {
            return false;
        }
        // (Las unidades aéreas pueden apilarse con terrestres, pero no entre sí,
        // lo cual se maneja en moveComponent)

        cell.addOccupant(component);
        component.setPosition(pos);

        // Añade a las listas de acceso rápido
        if (component instanceof Defense) {
            activeDefenses.add((Defense) component);
        } else if (component instanceof Zombie) {
            activeZombies.add((Zombie) component);
        }

        return true;
    }

    /**
     * Remueve un componente del tablero y de las listas activas.
     * (Ej. cuando es destruido o vendido).
     *
     * @param component El componente a remover.
     */
    public void removeComponent(Component component) {
        Position pos = component.getPosition();
        // Quita de la celda
        if (pos != null && isValidPosition(pos)) {
            getCell(pos.getX(), pos.getY()).removeOccupant(component);
        }

        // Quita de las listas activas
        if (component instanceof Defense) {
            activeDefenses.remove(component);
        } else if (component instanceof Zombie) {
            activeZombies.remove(component);
        }
    }

    /**
     * Mueve un componente de una celda a otra, aplicando lógica de colisión.
     * Este es el único método que debe usarse para cambiar la posición de un componente.
     *
     * @param component El componente a mover.
     * @param newPos La nueva posición deseada.
     * @return true si el movimiento fue exitoso, false si fue bloqueado.
     */
    public boolean moveComponent(Component component, Position newPos) {
        if (!isValidPosition(newPos)) {
            return false;
        }

        Cell oldCell = getCell(component.getPosition().getX(), component.getPosition().getY());
        Cell newCell = getCell(newPos.getX(), newPos.getY());

        // Lógica de colisión centralizada
        if (component.getType().isAerial()) {
            // Aéreo: No puede moverse si la nueva celda YA tiene otro aéreo.
            if (newCell.hasAerialOccupant()) {
                return false;
            }
        } else {
            // Terrestre: No puede moverse si la nueva celda YA tiene un terrestre.
            if (newCell.hasGroundOccupant()) {
                return false;
            }
        }

        // Realiza el movimiento
        oldCell.removeOccupant(component);
        newCell.addOccupant(component);
        component.setPosition(newPos);
        return true;
    }

    /**
     * Itera por todo el tablero y remueve los componentes marcados como 'isDestroyed'.
     * Se llama al final de cada gameTick.
     */
    public void cleanupDestroyedComponents() {
        List<Component> toRemove = new ArrayList<>();
        // Recorre la cuadrícula buscando componentes destruidos
        for (Cell[] row : grid) {
            for (Cell cell : row) {
                toRemove.addAll(cell.getOccupants().stream()
                        .filter(Component::isDestroyed)
                        .collect(Collectors.toList()));
            }
        }
        // Llama a removeComponent para cada uno (esto los quita de las listas activas también)
        toRemove.forEach(this::removeComponent);
    }

    /**
     * Verifica si una posición está dentro de los límites del tablero (0 a 24).
     */
    public boolean isValidPosition(Position pos) {
        return pos.getX() >= 0 && pos.getX() < SIZE && pos.getY() >= 0 && pos.getY() < SIZE;
    }

    /**
     * Verifica si una celda tiene algún ocupante.
     */
    public boolean isOccupied(Position pos) {
        return isValidPosition(pos) && !getCell(pos.getX(), pos.getY()).isEmpty();
    }

    /**
     * Verifica si una celda tiene una defensa activa.
     */
    public boolean isOccupiedByDefense(Position pos) {
        return isValidPosition(pos) && getDefenseAt(pos) != null;
    }

    /**
     * Obtiene la defensa activa en una posición.
     * @return La Defensa, o null si no hay ninguna.
     */
    public Defense getDefenseAt(Position pos) {
        if (!isValidPosition(pos)) return null;
        return (Defense) getCell(pos.getX(), pos.getY()).getOccupants().stream()
                .filter(c -> c instanceof Defense && !c.isDestroyed())
                .findFirst().orElse(null);
    }

    /**
     * Obtiene el primer zombie activo en una posición.
     * @return El Zombie, o null si no hay ninguno.
     */
    public Zombie getZombieAt(Position pos) {
        if (!isValidPosition(pos)) return null;
        return (Zombie) getCell(pos.getX(), pos.getY()).getOccupants().stream()
                .filter(c -> c instanceof Zombie && !c.isDestroyed())
                .findFirst().orElse(null);
    }

    /**
     * Obtiene el primer componente (de cualquier tipo) en una posición.
     * @return El Componente, o null si está vacía.
     */
    public Component getComponentAt(Position pos) {
        if (!isValidPosition(pos)) return null;
        return getCell(pos.getX(), pos.getY()).getOccupants().stream()
                .findFirst().orElse(null);
    }

    /**
     * Devuelve las posiciones vecinas válidas (Arriba, Abajo, Izquierda, Derecha).
     * Usado por el PathfindingService y para ataques adyacentes.
     *
     * @param pos La posición central.
     * @return Una lista de 2 a 4 posiciones vecinas válidas.
     */
    public List<Position> getNeighbors(Position pos) {
        List<Position> neighbors = new ArrayList<>();
        // Define los 4 movimientos direccionales
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // Arriba, Abajo, Izq, Der

        for (int[] dir : directions) {
            Position next = new Position(pos.getX() + dir[0], pos.getY() + dir[1]);
            if (isValidPosition(next)) {
                neighbors.add(next);
            }
        }
        return neighbors;
    }

    /**
     * Genera una posición aleatoria en uno de los 4 bordes del tablero.
     * Usado para el spawn de zombies.
     *
     * @return Una Posición en el borde (x=0, x=24, y=0, o y=24).
     */
    public Position getRandomEdgePosition() {
        java.util.Random rand = new java.util.Random();
        int side = rand.nextInt(4); // Elige un lado (0=arriba, 1=abajo, 2=izq, 3=der)
        return switch (side) {
            case 0 -> new Position(0, rand.nextInt(SIZE)); // Borde superior
            case 1 -> new Position(SIZE - 1, rand.nextInt(SIZE)); // Borde inferior
            case 2 -> new Position(rand.nextInt(SIZE), 0); // Borde izquierdo
            default -> new Position(rand.nextInt(SIZE), SIZE - 1); // Borde derecho
        };
    }

    /**
     * Limpia completamente el tablero, borrando todas las celdas y listas activas.
     * Se usa al iniciar un nuevo nivel.
     */
    public void clear() {
        activeDefenses.clear();
        activeZombies.clear();
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                grid[i][j].clear();
            }
        }
    }

    // --- Getters ---

    /** Devuelve una COPIA de la lista de defensas activas. */
    public List<Defense> getActiveDefenses() {
        return new ArrayList<>(activeDefenses);
    }

    /** Devuelve una COPIA de la lista de zombies activos. */
    public List<Zombie> getActiveZombies() {
        return new ArrayList<>(activeZombies);
    }

    public int getSize() {
        return SIZE;
    }

    public Cell[][] getGrid() {
        return grid;
    }

    public Cell getCell(int x, int y) {
        return grid[x][y];
    }

    @Override
    public String toString() {
        return String.format("Board [%dx%d] - Defensas: %d, Zombies: %d",
                SIZE, SIZE, activeDefenses.size(), activeZombies.size());
    }
}