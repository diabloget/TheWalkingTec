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
 * Representa un zombie enemigo.
 * Hereda de Componente e implementa la lógica de IA, movimiento y ataque.
 */
public class Zombie extends Component {
    private static final long serialVersionUID = 1L;
    private static final Random RANDOM = new Random(); // Para la IA de tipo RANDOM

    // --- Atributos de Configuración ---
    private final double movementSpeed; // Movimientos por segundo
    private final AIType aiType; // Tipo de IA (SEEK_NEAREST, CRASH, etc.)

    // --- Atributos de Estado ---
    private long lastMoveTime;
    private long lastAttackTime;

    public Zombie(EnemyConfig config, double boost) {
        super(config.getId(),
                config.getName(),
                ComponentType.valueOf(config.getType().toUpperCase()),
                (int) (config.getBaseHealth() * (1 + boost)), // Aplica boost de nivel
                (int) (config.getBaseDamage() * (1 + boost)),
                1, // Hits per second (Hardcodeado a 1)
                config.getFields(), // 'fields' (espacios)
                config.getUnlockLevel(),
                config.getImagePath());

        this.movementSpeed = config.getSpeed();
        this.lastMoveTime = 0;
        this.lastAttackTime = 0;

        // Asignación de IA robusta para prevenir crashes si el config.json es inválido
        AIType parsedAiType;
        try {
            if (config.getAiType() == null || config.getAiType().isEmpty()) {
                throw new IllegalArgumentException("aiType es null o vacío para " + config.getName());
            }
            parsedAiType = AIType.valueOf(config.getAiType().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Si la IA no es válida, usa una por defecto y avisa en consola
            System.err.println("ADVERTENCIA: aiType inválido ('" + config.getAiType() + "') para " + config.getName() + ". Usando SEEK_NEAREST por defecto.");
            parsedAiType = AIType.SEEK_NEAREST;
        }
        this.aiType = parsedAiType;
    }

    /**
     * Lógica del Zombie en cada tick del juego.
     * Controla la cadencia de movimiento y ataque.
     */
    @Override
    public void onTick(GameContext ctx) {
        long currentTime = System.currentTimeMillis();

        // Control de velocidad de movimiento
        double moveInterval = 1000.0 / movementSpeed;
        if (currentTime - lastMoveTime >= moveInterval) {
            move(ctx);
            lastMoveTime = currentTime;
        }

        // Control de velocidad de ataque
        double attackInterval = 1000.0 / getHitsPerSecond();
        if (currentTime - lastAttackTime >= attackInterval) {
            attack(ctx);
            lastAttackTime = currentTime;
        }
    }

    /**
     * Lógica de movimiento del Zombie.
     * 1. Determina un objetivo (basado en la IA).
     * 2. Pide una ruta al PathfindingService.
     * 3. Solicita moverse al siguiente paso de la ruta.
     */
    private void move(GameContext ctx) {
        // 1. Decide a dónde ir
        Position target = determineTarget(ctx);
        if (target == null) return; // No hay objetivo

        // 2. Pide la ruta (el pathfinding sabe si somos aéreos o terrestres)
        List<Position> path = ctx.getPathfindingService().findPath(ctx.getBoard(), position, target, this.type);

        if (path != null && path.size() > 1) {
            Position nextPos = path.get(1); // El siguiente paso en la ruta

            // Si el siguiente paso es la Reliquia y no somos 'CRASH', nos detenemos
            // en la casilla anterior para atacar desde adyacente.
            if (nextPos.equals(ctx.getRelicPosition())) {
                if (aiType != AIType.CRASH) {
                    return; // Detenerse y atacar desde adyacente
                }
            }

            // 3. Solicita el movimiento al tablero.
            // El tablero (Board.moveComponent) se encarga de la colisión.
            ctx.getBoard().moveComponent(this, nextPos);
        }
    }

    /**
     * Lógica de ataque del Zombie.
     * Ataca defensas adyacentes o la reliquia.
     */
    private void attack(GameContext ctx) {
        // 1. Atacar si está ENCIMA de la reliquia (solo para tipo CRASH)
        if (position.equals(ctx.getRelicPosition())) {
            ctx.damageRelic(damagePerHit);
            logAttack(null, "Reliquia", damagePerHit, ctx.getRelicLife() + damagePerHit, ctx.getRelicLife());
            if (aiType == AIType.CRASH) isDestroyed = true; // Se autodestruye
            return;
        }

        // 2. Atacar si está ADYACENTE a una defensa o a la reliquia
        for (Position neighbor : ctx.getBoard().getNeighbors(position)) {

            // Buscar defensas adyacentes
            Defense defense = ctx.getBoard().getDefenseAt(neighbor);
            if (defense != null && !defense.isDestroyed()) {

                // Regla: Zombies terrestres NO pueden atacar defensas aéreas (Drones)
                if (!this.getType().isAerial() && defense.getType().isAerial()) {
                    continue; // Ignora esta defensa y sigue buscando
                }

                // Si soy aéreo, o la defensa es terrestre, ataco.
                int lifeBefore = defense.getCurrentLife();
                defense.receiveDamage(damagePerHit, this.id, this.name);
                logAttack(defense.getId(), defense.getName(), damagePerHit, lifeBefore, defense.getCurrentLife());
                if (aiType == AIType.CRASH) isDestroyed = true; // Se autodestruye al atacar
                return; // Solo ataca un objetivo por tick
            }

            // Buscar reliquia adyacente
            if (neighbor.equals(ctx.getRelicPosition())) {
                ctx.damageRelic(damagePerHit);
                logAttack(null, "Reliquia", damagePerHit, ctx.getRelicLife() + damagePerHit, ctx.getRelicLife());
                if (aiType == AIType.CRASH) isDestroyed = true; // Se autodestruye
                return; // Atacó la reliquia, termina el turno
            }
        }
    }

    /**
     * Determina la posición objetivo basado en el tipo de IA.
     * @param ctx El contexto del juego.
     * @return La Posición del objetivo.
     */
    private Position determineTarget(GameContext ctx) {
        return switch (aiType) {
            case SEEK_NEAREST -> findNearestTarget(ctx); // Busca defensa más cercana
            case RANDOM -> generateRandomTarget(ctx);     // Busca objetivo aleatorio
            case CRASH -> ctx.getRelicPosition();      // Va directo a la reliquia
            default -> ctx.getRelicPosition();           // Por defecto, va a la reliquia
        };
    }

    /**
     * Lógica para IA SEEK_NEAREST.
     * @return Posición de la defensa más cercana, o la reliquia si no hay defensas.
     */
    private Position findNearestTarget(GameContext ctx) {
        Position relic = ctx.getRelicPosition();
        // Busca en todas las defensas activas y encuentra la más cercana
        return ctx.getBoard().getActiveDefenses().stream()
                .filter(d -> !d.isDestroyed())
                .map(Component::getPosition)
                .min(Comparator.comparingDouble(p -> p.distanceTo(position)))
                .orElse(relic); // Si no hay defensas, va a la reliquia
    }

    /**
     * Lógica para IA RANDOM.
     * 30% de probabilidad de ir a una defensa aleatoria, 70% de ir a la reliquia.
     */
    private Position generateRandomTarget(GameContext ctx) {
        if (RANDOM.nextDouble() < 0.3) { // 30% de probabilidad
            List<Defense> defenses = ctx.getBoard().getActiveDefenses();
            if (!defenses.isEmpty()) {
                // Elige una defensa al azar
                return defenses.get(RANDOM.nextInt(defenses.size())).getPosition();
            }
        }
        // 70% de las veces, o si no hay defensas, va a la reliquia
        return ctx.getRelicPosition();
    }

    // --- Getters ---
    public double getMovementSpeed() { return movementSpeed; }
    public AIType getAiType() { return aiType; }
}