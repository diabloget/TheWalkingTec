package diblo.thewalkingtec.service;

import diblo.thewalkingtec.model.Position;
import diblo.thewalkingtec.model.enums.ComponentType;

import java.util.*;

/**
 * Servicio de búsqueda de rutas (Pathfinding) usando el algoritmo A* (A-Star).
 * Este servicio encuentra el camino más corto (o una aproximación)
 * desde una posición 'from' a una 'to', respetando los obstáculos.
 */
public class PathfindingService {

    /**
     * Sobrecarga simple de findPath (para componentes terrestres, sin límite).
     */
    public List<Position> findPath(Board board, Position from, Position to) {
        // Por defecto, asume un componente terrestre y sin límite de búsqueda
        return findPath(board, from, to, ComponentType.CONTACT, Integer.MAX_VALUE, 1.0);
    }

    /**
     * Sobrecarga de findPath que considera el tipo de componente.
     */
    public List<Position> findPath(Board board, Position from, Position to, ComponentType moverType) {
        return findPath(board, from, to, moverType, Integer.MAX_VALUE, 1.0);
    }

    /**
     * Implementación principal del algoritmo A*.
     *
     * @param board El tablero actual.
     * @param from Posición inicial.
     * @param to Posición final (objetivo).
     * @param moverType El tipo de componente que se mueve (AERIAL o terrestre).
     * @param maxExpandedNodes Límite de nodos a expandir (para evitar lag).
     * @param heuristicWeight Peso de la heurística (1.0 = A* estándar, >1.0 = Búsqueda "Greedy").
     * @return Una lista de Posiciones (la ruta), o null si no se encontró.
     */
    public List<Position> findPath(Board board, Position from, Position to, ComponentType moverType,
                                   int maxExpandedNodes, double heuristicWeight) {
        if (from == null || to == null) return null;
        if (from.equals(to)) {
            return Collections.singletonList(from); // Ya está en el destino
        }

        Set<Position> closedSet = new HashSet<>(); // Nodos ya evaluados
        // Cola de prioridad que ordena por fScore (el costo estimado total)
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<Position, Node> allNodes = new HashMap<>(); // Acceso rápido a todos los nodos creados

        // 1. Inicializar con el nodo de inicio
        Node startNode = new Node(from, 0.0, from.manhattanDistanceTo(to));
        startNode.fScore = startNode.gScore + heuristicWeight * startNode.hScore; // f = g + (w * h)

        openSet.add(startNode);
        allNodes.put(from, startNode);

        int expanded = 0; // Contador de nodos expandidos
        Node bestSeen = startNode; // La mejor aproximación encontrada (el más cercano al 'to')

        // 2. Bucle principal de A*
        while (!openSet.isEmpty()) {
            Node current = openSet.poll(); // Obtiene el nodo con el menor fScore

            // Actualiza la mejor aproximación (el que tenga menor hScore)
            if (current.hScore < bestSeen.hScore) {
                bestSeen = current;
            }

            // Si es el destino, hemos terminado
            if (current.position.equals(to)) {
                return reconstructPath(current);
            }

            closedSet.add(current.position); // Marca como visitado

            // Control de límite de expansión (para rendimiento)
            expanded++;
            if (expanded > maxExpandedNodes) {
                return reconstructPath(bestSeen); // Devuelve la mejor ruta parcial
            }

            // 3. Explorar vecinos
            for (Position neighborPos : board.getNeighbors(current.position)) {
                if (closedSet.contains(neighborPos)) continue; // Ignora si ya se evaluó

                // Regla de colisión:
                // Si NO soy Aéreo, compruebo si el vecino tiene un ocupante terrestre.
                if (!moverType.isAerial()) {
                    // (Ignoramos la colisión si el vecino es el destino final)
                    if (!neighborPos.equals(to) && board.getCell(neighborPos.getX(), neighborPos.getY()).hasGroundOccupant()) {
                        continue; // Es un obstáculo terrestre, ignorar vecino
                    }
                }
                // (Si soy Aéreo, no hay obstáculos terrestres, sigo)

                double tentativeGScore = current.gScore + 1.0; // Costo de moverse al vecino (1)

                Node neighborNode = allNodes.get(neighborPos);
                if (neighborNode == null) {
                    // Si es un nodo nuevo, se crea con G infinito
                    neighborNode = new Node(neighborPos, Double.POSITIVE_INFINITY, neighborPos.manhattanDistanceTo(to));
                    allNodes.put(neighborPos, neighborNode);
                }

                // Si la ruta actual NO es mejor que la que ya tenía, ignorar
                if (tentativeGScore >= neighborNode.gScore) {
                    continue;
                }

                // Es un mejor camino, actualizar el nodo vecino
                neighborNode.parent = current;
                neighborNode.gScore = tentativeGScore;
                neighborNode.hScore = neighborPos.manhattanDistanceTo(to); // Heurística Manhattan
                neighborNode.fScore = neighborNode.gScore + heuristicWeight * neighborNode.hScore;

                // Re-insertar en la cola de prioridad para reordenar
                if (openSet.contains(neighborNode)) {
                    openSet.remove(neighborNode);
                }
                openSet.add(neighborNode);
            }
        }

        // No se encontró camino (destino inalcanzable)
        return null;
    }

    /**
     * Reconstruye la lista de posiciones (ruta) yendo hacia atrás
     * desde el nodo final hasta el nodo inicial (parent == null).
     *
     * @param endNode El nodo final (destino).
     * @return La lista de posiciones en orden (inicio -> fin).
     */
    private List<Position> reconstructPath(Node endNode) {
        List<Position> path = new ArrayList<>();
        Node current = endNode;
        while (current != null) {
            path.add(current.position);
            current = current.parent;
        }
        Collections.reverse(path); // Invierte la lista (quedó fin -> inicio)
        return path;
    }

    /**
     * Clase interna que representa un nodo en el grafo de búsqueda A*.
     */
    private static class Node {
        Position position;
        Node parent; // Nodo anterior en la ruta
        double gScore; // Costo real desde el inicio (G)
        double hScore; // Costo heurístico estimado al final (H)
        double fScore; // Costo total (F = G + H)

        Node(Position position, double gScore, double hScore) {
            this.position = position;
            this.gScore = gScore;
            this.hScore = hScore;
            this.fScore = gScore + hScore;
            this.parent = null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Node)) return false;
            Node node = (Node) o;
            return position.equals(node.position); // Compara nodos por su posición
        }

        @Override
        public int hashCode() {
            return position.hashCode(); // Usa el hash de la posición
        }
    }
}