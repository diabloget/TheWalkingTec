package diblo.thewalkingtec.model;

import diblo.thewalkingtec.model.config.EnemyConfig;
import diblo.thewalkingtec.model.enums.AIType;
import diblo.thewalkingtec.model.enums.ComponentType;
import diblo.thewalkingtec.service.Cell;
import diblo.thewalkingtec.service.GameContext;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Representa un zombie enemigo
 */
public class Zombie extends Component {
    private static final long serialVersionUID = 1L;
    private static final Random RANDOM = new Random();

    private final double movementSpeed;
    private final AIType aiType;
    private long lastMoveTime;
    private long lastAttackTime;

    public Zombie(EnemyConfig config, double boost) {
        super(config.getId(),
              config.getName(),
              ComponentType.valueOf(config.getType().toUpperCase()),
              (int) (config.getBaseHealth() * (1 + boost)),
              (int) (config.getBaseDamage() * (1 + boost)),
              1, // Hits per second, puede ser parte de la config
                config.getFields(),
              config.getUnlockLevel(),
              config.getImagePath());
        this.movementSpeed = config.getSpeed();
        this.lastMoveTime = 0;
        this.lastAttackTime = 0;

        // Asignación de IA robusta para prevenir crashes
        AIType parsedAiType;
        try {
            if (config.getAiType() == null || config.getAiType().isEmpty()) {
                throw new IllegalArgumentException("aiType is null or empty in config for " + config.getName());
            }
            parsedAiType = AIType.valueOf(config.getAiType().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("ADVERTENCIA: aiType inválido ('" + config.getAiType() + "') para " + config.getName() + ". Usando SEEK_NEAREST por defecto.");
            parsedAiType = AIType.SEEK_NEAREST;
        }
        this.aiType = parsedAiType;
    }

    @Override
    public void onTick(GameContext ctx) {
        long currentTime = System.currentTimeMillis();

        double moveInterval = 1000.0 / movementSpeed;
        if (currentTime - lastMoveTime >= moveInterval) {
            move(ctx);
            lastMoveTime = currentTime;
        }

        double attackInterval = 1000.0 / getHitsPerSecond();
        if (currentTime - lastAttackTime >= attackInterval) {
            attack(ctx);
            lastAttackTime = currentTime;
        }
    }

    private void move(GameContext ctx) {
        Position target = determineTarget(ctx);
        if (target == null) return;

        // Usar el pathfinding que considera el tipo de unidad
        List<Position> path = ctx.getPathfindingService().findPath(ctx.getBoard(), position, target, this.type);

        if (path != null && path.size() > 1) {
            Position nextPos = path.get(1);

            // Detenerse ante la reliquia (lógica correcta que tenías)
            if (nextPos.equals(ctx.getRelicPosition())) {
                if (aiType != AIType.CRASH) {
                    return; // Detenerse y atacar desde adyacente
                }
            }

            // --- INICIO CORRECCIÓN MOVIMIENTO ---
            // Simplemente intenta moverte. El Board (moveComponent)
            // se encargará de la lógica de colisión.
            ctx.getBoard().moveComponent(this, nextPos);
            // --- FIN CORRECCIÓN MOVIMIENTO ---
        }
    }

    private void attack(GameContext ctx) {
        // 1. Atacar si está ENCIMA de la reliquia (para tipo CRASH)
        if (position.equals(ctx.getRelicPosition())) {
            ctx.damageRelic(damagePerHit);
            logAttack(null, "Reliquia", damagePerHit, ctx.getRelicLife() + damagePerHit, ctx.getRelicLife());
            if (aiType == AIType.CRASH) isDestroyed = true;
            return;
        }

        // 2. Atacar si está ADYACENTE a una defensa o a la reliquia
        for (Position neighbor : ctx.getBoard().getNeighbors(position)) {

            // Buscar defensas adyacentes
            Defense defense = ctx.getBoard().getDefenseAt(neighbor); // Asumo que Board.getDefenseAt() existe
            if (defense != null && !defense.isDestroyed()) {

                // --- INICIO CORRECCIÓN ATAQUE ---
                // Si yo NO soy aéreo Y la defensa SÍ es aérea, NO puedo atacar.
                if (!this.getType().isAerial() && defense.getType().isAerial()) {
                    continue; // Saltar este vecino, no puedo atacar al Dron
                }
                // --- FIN CORRECCIÓN ATAQUE ---

                // Si soy aéreo, o la defensa es terrestre, ataco.
                int lifeBefore = defense.getCurrentLife();
                defense.receiveDamage(damagePerHit, this.id, this.name);
                logAttack(defense.getId(), defense.getName(), damagePerHit, lifeBefore, defense.getCurrentLife());
                if (aiType == AIType.CRASH) isDestroyed = true;
                return; // Atacó una defensa, termina
            }

            // --- LÓGICA FALTANTE ---
            // Buscar reliquia adyacente
            if (neighbor.equals(ctx.getRelicPosition())) {
                ctx.damageRelic(damagePerHit);
                logAttack(null, "Reliquia", damagePerHit, ctx.getRelicLife() + damagePerHit, ctx.getRelicLife());
                if (aiType == AIType.CRASH) isDestroyed = true;
                return; // Atacó la reliquia, termina el turno de ataque
            }
            // --- FIN DE LÓGICA FALTANTE ---
        }
    }

    private Position determineTarget(GameContext ctx) {
        return switch (aiType) {
            case SEEK_NEAREST -> findNearestTarget(ctx);
            case RANDOM -> generateRandomTarget(ctx);
            case CRASH -> ctx.getRelicPosition();
            default -> ctx.getRelicPosition();
        };
    }

    private Position findNearestTarget(GameContext ctx) {
        Position relic = ctx.getRelicPosition();
        return ctx.getBoard().getActiveDefenses().stream()
                .filter(d -> !d.isDestroyed())
                .map(Component::getPosition)
                .min(Comparator.comparingDouble(p -> p.distanceTo(position)))
                .orElse(relic);
    }

    private Position generateRandomTarget(GameContext ctx) {
        if (RANDOM.nextDouble() < 0.3) {
            List<Defense> defenses = ctx.getBoard().getActiveDefenses();
            if (!defenses.isEmpty()) {
                return defenses.get(RANDOM.nextInt(defenses.size())).getPosition();
            }
        }
        return ctx.getRelicPosition();
    }

    // Getters
    public double getMovementSpeed() { return movementSpeed; }
    public AIType getAiType() { return aiType; }
}