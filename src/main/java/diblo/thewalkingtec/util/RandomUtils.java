package diblo.thewalkingtec.util;

import java.util.List;
import java.util.Random;

/**
 * Utilidades para generación de números aleatorios
 */
public class RandomUtils {
    private static final Random RANDOM = new Random();

    /**
     * Genera un double aleatorio entre min (inclusivo) y max (exclusivo)
     */
    public static double randomDouble(double min, double max) {
        return min + (max - min) * RANDOM.nextDouble();
    }

    /**
     * Genera un int aleatorio entre min (inclusivo) y max (exclusivo)
     */
    public static int randomInt(int min, int max) {
        return RANDOM.nextInt(max - min) + min;
    }

    /**
     * Retorna true con una probabilidad dada (0.0 a 1.0)
     */
    public static boolean randomChance(double probability) {
        return RANDOM.nextDouble() < probability;
    }

    /**
     * Selecciona un elemento aleatorio de una lista
     */
    public static <T> T randomElement(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(RANDOM.nextInt(list.size()));
    }

    /**
     * Genera un boolean aleatorio
     */
    public static boolean randomBoolean() {
        return RANDOM.nextBoolean();
    }
}