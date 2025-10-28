package diblo.thewalkingtec.service;

import diblo.thewalkingtec.model.Position;
import diblo.thewalkingtec.model.enums.ComponentType;

import java.util.*;

/**
 * Servicio de pathfinding usando A* (arreglo y mejoras).
 * - Corrige la creación de nodos (g = +inf para nodos nuevos)
 * - Soporta cutoff (maxExpandedNodes) y devuelve la mejor aproximación si se corta
 * - Soporta heuristicWeight (>1 para búsqueda más rápida no óptima)
 */
public class PathfindingService {

    public List<Position> findPath(Board board, Position from, Position to) {
        return findPath(board, from, to, ComponentType.CONTACT, Integer.MAX_VALUE, 1.0);
    }

    public List<Position> findPath(Board board, Position from, Position to, ComponentType moverType) {
        return findPath(board, from, to, moverType, Integer.MAX_VALUE, 1.0);
    }

    public List<Position> findPath(Board board, Position from, Position to, ComponentType moverType,
                                   int maxExpandedNodes, double heuristicWeight) {
        if (from == null || to == null) return null;
        if (from.equals(to)) {
            return Collections.singletonList(from);
        }

        Set<Position> closedSet = new HashSet<>();
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<Position, Node> allNodes = new HashMap<>();

        Node startNode = new Node(from, 0.0, from.manhattanDistanceTo(to));
        startNode.fScore = startNode.gScore + heuristicWeight * startNode.hScore;

        openSet.add(startNode);
        allNodes.put(from, startNode);

        int expanded = 0;
        Node bestSeen = startNode; // mejor aproximación por hScore

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            // actualizar bestSeen por cercanía heurística
            if (current.hScore < bestSeen.hScore) {
                bestSeen = current;
            }

            // si alcanzamos el destino, reconstruir camino
            if (current.position.equals(to)) {
                return reconstructPath(current);
            }

            closedSet.add(current.position);

            // control de expansión para evitar stall en mapas grandes
            expanded++;
            if (expanded > maxExpandedNodes) {
                // devolver mejor aproximación (camino hasta bestSeen)
                return reconstructPath(bestSeen);
            }

            // explorar vecinos
            for (Position neighborPos : board.getNeighbors(current.position)) {
                if (closedSet.contains(neighborPos)) continue;

                // si no es aéreo, bloquear por ocupantes terrestres (excepto la celda destino)
                if (!moverType.isAerial()) {
                    if (!neighborPos.equals(to) && board.getCell(neighborPos.getX(), neighborPos.getY()).hasGroundOccupant()) {
                        continue;
                    }
                }

                double tentativeGScore = current.gScore + 1.0; // coste uniforme entre celdas

                Node neighborNode = allNodes.get(neighborPos);
                if (neighborNode == null) {
                    // CORRECCIÓN: crear con gScore = +inf para que la primera vez sea considerado mejora
                    neighborNode = new Node(neighborPos, Double.POSITIVE_INFINITY, neighborPos.manhattanDistanceTo(to));
                    allNodes.put(neighborPos, neighborNode);
                }

                // si no es mejor camino, ignorar
                if (tentativeGScore >= neighborNode.gScore) {
                    continue;
                }

                // mejor camino -> actualizar
                neighborNode.parent = current;
                neighborNode.gScore = tentativeGScore;
                neighborNode.hScore = neighborPos.manhattanDistanceTo(to);
                neighborNode.fScore = neighborNode.gScore + heuristicWeight * neighborNode.hScore;

                // insertar/actualizar en openSet
                if (!openSet.contains(neighborNode)) {
                    openSet.add(neighborNode);
                } else {
                    // PriorityQueue no actualiza posición, así que remove+add para reordenar
                    openSet.remove(neighborNode);
                    openSet.add(neighborNode);
                }
            }
        }

        // no se encontró camino
        return null;
    }

    private List<Position> reconstructPath(Node endNode) {
        List<Position> path = new ArrayList<>();
        Node current = endNode;
        while (current != null) {
            path.add(current.position);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private static class Node {
        Position position;
        Node parent;
        double gScore;
        double hScore;
        double fScore;

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
            return position.equals(node.position);
        }

        @Override
        public int hashCode() {
            return position.hashCode();
        }
    }
}
