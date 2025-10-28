package diblo.thewalkingtec.service;

import diblo.thewalkingtec.model.Player;
import diblo.thewalkingtec.model.Position;

/**
 * Contexto del juego compartido entre todos los componentes
 * No es serializable porque se reconstruye en cada sesión
 */
public class GameContext {
    private Board board;
    private Player player;
    private Position relicPosition;
    private PathfindingService pathfindingService;
    private Game game;

    public GameContext(Board board, Player player, Position relicPosition,
                       PathfindingService pathfindingService, Game game) {
        this.board = board;
        this.player = player;
        this.relicPosition = relicPosition;
        this.pathfindingService = pathfindingService;
        this.game = game;
    }

    public synchronized void damageRelic(int damage) {
        game.damageRelic(damage);
    }

    public void healRelic() {
        game.healRelic();
    }

    public boolean isRelicDestroyed() {
        return game.isRelicDestroyed();
    }

    public double getRelicLifePercentage() {
        return game.getMaxRelicLife() > 0 ? (double) game.getRelicLife() / game.getMaxRelicLife() : 0;
    }

    // Getters
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