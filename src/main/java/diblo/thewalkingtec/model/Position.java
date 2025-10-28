package diblo.thewalkingtec.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Representa una coordenada (x, y) en la cuadrícula del juego.
 * Esta clase es fundamental para el pathfinding y el posicionamiento.
 * Es Serializable para poder guardar el estado de los componentes.
 */
public class Position implements Serializable {
    private static final long serialVersionUID = 1L;

    private int x;
    private int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // Getters
    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }

    // Setters
    public void setX(int x) {
        this.x = x;
    }
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Calcula la distancia euclidiana (diagonal) a otra posición.
     * Útil para defensas de rango circular.
     *
     * @param other La otra posición.
     * @return La distancia como un double.
     */
    public double distanceTo(Position other) {
        int dx = x - other.x;
        int dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Calcula la distancia Manhattan (en cuadrícula, sin diagonales) a otra posición.
     * Usado como heurística para el algoritmo de pathfinding A*.
     *
     * @param other La otra posición.
     * @return La distancia como un entero.
     */
    public int manhattanDistanceTo(Position other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }

    /**
     * Crea una nueva instancia de Position con las mismas coordenadas.
     * @return Una copia de esta posición.
     */
    public Position copy() {
        return new Position(x, y);
    }

    /**
     * Compara si dos objetos Position representan la misma coordenada (x, y).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false; // Asegura que 'o' sea un objeto Position
        Position position = (Position) o;
        return x == position.x && y == position.y;
    }

    /**
     * Genera un código hash basado en las coordenadas x e y.
     * Esencial para que las Position funcionen correctamente en HashMaps y HashSets
     * (usados en el PathfindingService).
     */
    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}