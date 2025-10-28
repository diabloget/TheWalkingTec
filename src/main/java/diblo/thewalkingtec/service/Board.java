package diblo.thewalkingtec.service;

import diblo.thewalkingtec.model.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Representa el tablero de juego (cuadrícula 25x25)
 */
public class Board implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int SIZE = 25;

    private final Cell[][] grid;
    private final List<Defense> activeDefenses;
    private final List<Zombie> activeZombies;

    public Board() {
        grid = new Cell[SIZE][SIZE];
        activeDefenses = new ArrayList<>();
        activeZombies = new ArrayList<>();

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                grid[i][j] = new Cell(new Position(i, j));
            }
        }
    }

    public boolean placeComponent(Component component, Position pos) {
        if (!isValidPosition(pos)) {
            return false;
        }

        Cell cell = getCell(pos.getX(), pos.getY());

        // Las unidades terrestres no pueden apilarse.
        if (!component.getType().isAerial() && cell.hasGroundOccupant()) {
            return false;
        }

        cell.addOccupant(component);
        component.setPosition(pos);

        if (component instanceof Defense) {
            activeDefenses.add((Defense) component);
        } else if (component instanceof Zombie) {
            activeZombies.add((Zombie) component);
        }

        return true;
    }

    public void removeComponent(Component component) {
        Position pos = component.getPosition();
        if (pos != null && isValidPosition(pos)) {
            getCell(pos.getX(), pos.getY()).removeOccupant(component);
        }

        if (component instanceof Defense) {
            activeDefenses.remove(component);
        } else if (component instanceof Zombie) {
            activeZombies.remove(component);
        }
    }

    public boolean moveComponent(Component component, Position newPos) {
        if (!isValidPosition(newPos)) {
            return false;
        }

        Cell oldCell = getCell(component.getPosition().getX(), component.getPosition().getY());
        Cell newCell = getCell(newPos.getX(), newPos.getY());

        // --- LÓGICA DE COLISIÓN MEJORADA ---
        if (component.getType().isAerial()) {
            // Aéreo: No puede moverse si la nueva celda YA tiene un aéreo.
            if (newCell.hasAerialOccupant()) {
                return false;
            }
        } else {
            // Terrestre: No puede moverse si la nueva celda YA tiene un terrestre.
            if (newCell.hasGroundOccupant()) {
                return false;
            }
        }
        // --- FIN DE LÓGICA ---

        oldCell.removeOccupant(component);
        newCell.addOccupant(component);
        component.setPosition(newPos);
        return true;
    }

    public void cleanupDestroyedComponents() {
        List<Component> toRemove = new ArrayList<>();
        for (Cell[] row : grid) {
            for (Cell cell : row) {
                toRemove.addAll(cell.getOccupants().stream()
                        .filter(Component::isDestroyed)
                        .collect(Collectors.toList()));
            }
        }
        toRemove.forEach(this::removeComponent);
    }

    public boolean isValidPosition(Position pos) {
        return pos.getX() >= 0 && pos.getX() < SIZE && pos.getY() >= 0 && pos.getY() < SIZE;
    }

    public boolean isOccupied(Position pos) {
        return isValidPosition(pos) && !getCell(pos.getX(), pos.getY()).isEmpty();
    }

    public boolean isOccupiedByDefense(Position pos) {
        return isValidPosition(pos) && getDefenseAt(pos) != null;
    }

    public Defense getDefenseAt(Position pos) {
        if (!isValidPosition(pos)) return null;
        return (Defense) getCell(pos.getX(), pos.getY()).getOccupants().stream()
                .filter(c -> c instanceof Defense && !c.isDestroyed())
                .findFirst().orElse(null);
    }

    public Zombie getZombieAt(Position pos) {
        if (!isValidPosition(pos)) return null;
        return (Zombie) getCell(pos.getX(), pos.getY()).getOccupants().stream()
                .filter(c -> c instanceof Zombie && !c.isDestroyed())
                .findFirst().orElse(null);
    }

    public Component getComponentAt(Position pos) {
        if (!isValidPosition(pos)) return null;
        return getCell(pos.getX(), pos.getY()).getOccupants().stream()
                .findFirst().orElse(null);
    }

    public List<Position> getNeighbors(Position pos) {
        List<Position> neighbors = new ArrayList<>();
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int[] dir : directions) {
            Position next = new Position(pos.getX() + dir[0], pos.getY() + dir[1]);
            if (isValidPosition(next)) {
                neighbors.add(next);
            }
        }
        return neighbors;
    }

    public Position getRandomEdgePosition() {
        java.util.Random rand = new java.util.Random();
        int side = rand.nextInt(4);
        return switch (side) {
            case 0 -> new Position(0, rand.nextInt(SIZE));
            case 1 -> new Position(SIZE - 1, rand.nextInt(SIZE));
            case 2 -> new Position(rand.nextInt(SIZE), 0);
            default -> new Position(rand.nextInt(SIZE), SIZE - 1);
        };
    }

    public void clear() {
        activeDefenses.clear();
        activeZombies.clear();
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                grid[i][j].clear();
            }
        }
    }

    public List<Defense> getActiveDefenses() {
        return new ArrayList<>(activeDefenses);
    }

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