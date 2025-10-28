package diblo.thewalkingtec.model;

import diblo.thewalkingtec.model.config.DefenseConfig;
import diblo.thewalkingtec.model.enums.ComponentType;
import diblo.thewalkingtec.service.Cell;
import diblo.thewalkingtec.service.GameContext;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Representa una defensa colocada por el jugador
 */
public class Defense extends Component {
    private static final long serialVersionUID = 1L;

    private final int range;
    private final int maxTargetsSimultaneous;
    private long lastAttackTime;
    private final int cost;
    private long lastMoveTime;

    public Defense(DefenseConfig config, double boost) {
        super(config.getId(),
                config.getName(),
                ComponentType.valueOf(config.getType().toUpperCase()),
                (int) (config.getBaseHealth() * (1 + boost)),
                (int) (config.getBaseDamage() * (1 + boost)),
                1, // Hits per second
                config.getFields(),
                config.getUnlockLevel(),
                config.getImagePath());
        this.range = config.getRange();
        this.maxTargetsSimultaneous = 1;
        this.cost = config.getCost(); // El costo se guarda aquí correctamente
        this.lastAttackTime = 0;
        this.lastMoveTime = 0;
    }

    @Override
    public void onTick(GameContext ctx) {

        // --- LÓGICA DE MOVIMIENTO PARA DRONES ---
        if (type.isAerial()) {
            long currentTime = System.currentTimeMillis();
            // Moverse 1 vez por segundo
            double moveInterval = 1000.0;
            if (currentTime - lastMoveTime >= moveInterval) {
                move(ctx); // Llama al método move() de abajo
                lastMoveTime = currentTime;
            }
        }
        // --- FIN ---

        if (getDamagePerHit() == 0) { // Lógica de 'barbed_wire'
            return;
        }

        long currentTime = System.currentTimeMillis();
        double attackInterval = 1000.0 / getHitsPerSecond();

        if (currentTime - lastAttackTime >= attackInterval) {
            List<Zombie> targets = findTargets(ctx);
            if (!targets.isEmpty()) {
                for (Zombie target : targets) {
                    attack(target);
                }
                lastAttackTime = currentTime;
            }
        }
    }


    // --- AÑADIR ESTE MÉTODO NUEVO ---
    private void move(GameContext ctx) {
        // Lógica de Dron: buscar zombie más cercano para sobrevolar
        Position target = ctx.getBoard().getActiveZombies().stream()
                .filter(z -> !z.isDestroyed())
                .min(Comparator.comparingDouble(z -> z.getPosition().distanceTo(position)))
                .map(Component::getPosition)
                .orElse(null);

        if (target == null) return; // No hay zombies, no se mueve

        List<Position> path = ctx.getPathfindingService().findPath(ctx.getBoard(), position, target, this.type);

        if (path != null && path.size() > 1) {
            Position nextPos = path.get(1);

            // Simplemente intenta moverte. El Board (moveComponent)
            // se encargará de la lógica de colisión aérea.
            ctx.getBoard().moveComponent(this, nextPos);
        }
    }


    private List<Zombie> findTargets(GameContext ctx) {
        if (type.isAerial()) {
            // CORREGIDO: Drones (Aéreos) atacan CUALQUIER zombie
            // (terrestre o aéreo) que esté en su MISMA celda.
            return ctx.getBoard().getCell(position.getX(), position.getY()).getOccupants().stream()
                    .filter(c -> c instanceof Zombie && !c.isDestroyed())
                    .map(c -> (Zombie) c)
                    .limit(maxTargetsSimultaneous)
                    .collect(Collectors.toList());
        } else {
            // Terrestres NO atacan aéreos (tu lógica original era correcta aquí)
            return ctx.getBoard().getActiveZombies().stream()
                    .filter(z -> !z.isDestroyed())
                    .filter(z -> !z.getType().isAerial()) // NO atacan aéreos
                    .filter(z -> position.distanceTo(z.getPosition()) <= range)
                    .limit(maxTargetsSimultaneous)
                    .collect(Collectors.toList());
        }
    }

    private void attack(Zombie target) {
        int lifeBefore = target.getCurrentLife();
        target.receiveDamage(damagePerHit, this.id, this.name);
        logAttack(target.getId(), target.getName(), damagePerHit, lifeBefore, target.getCurrentLife());
    }

    // Getters
    public int getRange() { return range; }
    public int getCost() { return cost; }
    public int getMaxTargetsSimultaneous() { return maxTargetsSimultaneous; }
}