package diblo.thewalkingtec.service;

import diblo.thewalkingtec.model.Player;
import diblo.thewalkingtec.model.Position;

/**
 * Contexto del juego compartido entre todos los componentes.
 * Esta clase actúa como un "contenedor" de acceso rápido a los servicios
 * y al estado principal del juego (Tablero, Jugador, Reliquia).
 *
 * No es serializable porque se reconstruye en cada sesión o al cargar
 * una partida guardada.
 */
public class GameContext {
    private Board board;
    private Player player;
    private Position relicPosition;
    private PathfindingService pathfindingService;
    private Game game; // Referencia al motor principal del juego

    public GameContext(Board board, Player player, Position relicPosition,
                       PathfindingService pathfindingService, Game game) {
        this.board = board;
        this.player = player;
        this.relicPosition = relicPosition;
        this.pathfindingService = pathfindingService;
        this.game = game;
    }

    /**
     * Delega el daño a la reliquia al objeto Game (para sincronización).
     * @param damage El daño a infligir.
     */
    public synchronized void damageRelic(int damage) {
        game.damageRelic(damage);
    }

    /**
     * Delega la curación de la reliquia al objeto Game.
     */
    public void healRelic() {
        game.healRelic();
    }

    /**
     * Comprueba si la reliquia está destruida.
     */
    public boolean isRelicDestroyed() {
        return game.isRelicDestroyed();
    }

    /**
     * Obtiene el porcentaje de vida de la reliquia (0.0 a 1.0).
     */
    public double getRelicLifePercentage() {
        return game.getMaxRelicLife() > 0 ? (double) game.getRelicLife() / game.getMaxRelicLife() : 0;
    }

    // --- Getters de Acceso Rápido ---

    public Board getBoard() { return board; }
    public Player getPlayer() { return player; }
    public Position getRelicPosition() { return relicPosition; }
    public PathfindingService getPathfindingService() { return pathfindingService; }
    public int getRelicLife() { return game.getRelicLife(); }
    public int getMaxRelicLife() { return game.getMaxRelicLife(); }

    @Override
    public String toString() {
        return String.format("GameContext [Relic: %d/%d HP at %s]",
                getRelicLife(), getMaxRelicLife(), relicPosition);
    }
}