package diblo.thewalkingtec.model;

import diblo.thewalkingtec.model.config.DefenseConfig;
import diblo.thewalkingtec.model.enums.ComponentType;
import diblo.thewalkingtec.service.Cell;
import diblo.thewalkingtec.service.GameContext;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Representa una defensa colocada por el jugador.
 * Hereda de Componente e implementa la lógica de ataque y (si es Dron) movimiento.
 */
public class Defense extends Component {
    private static final long serialVersionUID = 1L;

    // --- Atributos de Configuración ---
    private final int range;
    private final int maxTargetsSimultaneous;
    private final int cost; // Costo en monedas para comprar

    // --- Atributos de Estado ---
    private long lastAttackTime;
    private long lastMoveTime; // Específico para Drones

    public Defense(DefenseConfig config, double boost) {
        super(config.getId(),
                config.getName(),
                ComponentType.valueOf(config.getType().toUpperCase()),
                (int) (config.getBaseHealth() * (1 + boost)), // Aplica boost de nivel
                (int) (config.getBaseDamage() * (1 + boost)),
                1, // Hits per second (Hardcodeado a 1)
                config.getFields(), // 'fields' son los espacios que ocupa
                config.getUnlockLevel(),
                config.getImagePath());

        this.range = config.getRange();
        this.maxTargetsSimultaneous = 1; // Hardcodeado a 1
        this.cost = config.getCost(); // El costo en monedas
        this.lastAttackTime = 0;
        this.lastMoveTime = 0;
    }

    /**
     * Lógica de la Defensa en cada tick del juego.
     * Si es Aérea (Dron), intenta moverse.
     * Si tiene daño, intenta buscar objetivos y atacar.
     */
    @Override
    public void onTick(GameContext ctx) {

        // Lógica de movimiento exclusiva para Drones (Aéreos)
        if (type.isAerial()) {
            long currentTime = System.currentTimeMillis();
            double moveInterval = 1000.0; // Moverse 1 vez por segundo
            if (currentTime - lastMoveTime >= moveInterval) {
                move(ctx); // Llama al método move()
                lastMoveTime = currentTime;
            }
        }

        // Si la defensa no tiene daño (ej. Muro, Alambre) no hace nada más
        if (getDamagePerHit() == 0) {
            return;
        }

        // Lógica de Ataque (para todas las defensas con daño)
        long currentTime = System.currentTimeMillis();
        double attackInterval = 1000.0 / getHitsPerSecond();

        // Controla la cadencia de disparo
        if (currentTime - lastAttackTime >= attackInterval) {
            List<Zombie> targets = findTargets(ctx);
            if (!targets.isEmpty()) {
                for (Zombie target : targets) {
                    attack(target); // Ataca a cada objetivo encontrado
                }
                lastAttackTime = currentTime;
            }
        }
    }


    /**
     * Lógica de movimiento para Drones (Componentes Aéreos).
     * Busca el zombie más cercano y se mueve hacia él.
     */
    private void move(GameContext ctx) {
        // 1. Buscar el zombie más cercano
        Position target = ctx.getBoard().getActiveZombies().stream()
                .filter(z -> !z.isDestroyed())
                .min(Comparator.comparingDouble(z -> z.getPosition().distanceTo(position)))
                .map(Component::getPosition)
                .orElse(null);

        if (target == null) return; // No hay zombies, no se mueve

        // 2. Encontrar la ruta (usando pathfinding aéreo)
        List<Position> path = ctx.getPathfindingService().findPath(ctx.getBoard(), position, target, this.type);

        if (path != null && path.size() > 1) {
            Position nextPos = path.get(1); // El siguiente paso en la ruta

            // 3. Solicitar el movimiento al tablero.
            // El tablero (Board.moveComponent) se encarga de la colisión.
            ctx.getBoard().moveComponent(this, nextPos);
        }
    }


    /**
     * Encuentra objetivos (Zombies) según el tipo de defensa.
     * @param ctx El contexto del juego.
     * @return Una lista de zombies a los que se puede atacar.
     */
    private List<Zombie> findTargets(GameContext ctx) {
        if (type.isAerial()) {
            // Drones (Aéreos): Atacan CUALQUIER zombie (terrestre o aéreo)
            // que esté en su MISMA celda.
            return ctx.getBoard().getCell(position.getX(), position.getY()).getOccupants().stream()
                    .filter(c -> c instanceof Zombie && !c.isDestroyed())
                    .map(c -> (Zombie) c)
                    .limit(maxTargetsSimultaneous)
                    .collect(Collectors.toList());
        } else {
            // Defensas Terrestres: Atacan zombies en rango,
            // pero NO pueden atacar a zombies aéreos.
            return ctx.getBoard().getActiveZombies().stream()
                    .filter(z -> !z.isDestroyed())
                    .filter(z -> !z.getType().isAerial()) // NO atacan aéreos
                    .filter(z -> position.distanceTo(z.getPosition()) <= range) // Dentro del rango
                    .limit(maxTargetsSimultaneous)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Aplica daño a un zombie objetivo y lo registra en el log.
     * @param target El Zombie a atacar.
     */
    private void attack(Zombie target) {
        int lifeBefore = target.getCurrentLife();
        target.receiveDamage(damagePerHit, this.id, this.name);
        // Registra el ataque HECHO
        logAttack(target.getId(), target.getName(), damagePerHit, lifeBefore, target.getCurrentLife());
    }

    // --- Getters ---
    public int getRange() { return range; }
    public int getCost() { return cost; }
    public int getMaxTargetsSimultaneous() { return maxTargetsSimultaneous; }
}