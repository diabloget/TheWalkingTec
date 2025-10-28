package diblo.thewalkingtec.model;

import java.io.Serializable;

/**
 * Representa al jugador del juego.
 * Gestiona las monedas, el nivel, el score y el ejército (Army).
 */
public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private int level;
    private int coins; // Monedas para comprar defensas
    private int capacityBase; // Capacidad base del ejército
    private Army army; // El ejército del jugador (gestiona defensas y espacios)
    private int score;

    public Player(String name, int initialCapacity, int initialCoins) {
        this.name = name;
        this.level = 1;
        this.capacityBase = initialCapacity;
        this.coins = initialCoins;
        this.army = new Army(initialCapacity); // Crea el ejército con la capacidad inicial
        this.score = 0;
    }

    /**
     * Intenta comprar y añadir una defensa al ejército del jugador.
     * Valida si el jugador tiene suficientes monedas y capacidad.
     *
     * @param defense La defensa que se intenta comprar.
     * @return true si la compra y colocación fue exitosa.
     */
    public boolean placeDefense(Defense defense) {
        if (coins < defense.getCost()) {
            return false; // No hay suficientes monedas
        }

        if (!army.hasCapacityFor(defense)) {
            return false; // No hay capacidad en el ejército
        }

        // Si se puede añadir al ejército, se cobra
        if (army.addDefense(defense)) {
            coins -= defense.getCost();
            return true;
        }

        return false;
    }

    /**
     * Remueve una defensa del ejército (ej. "vender").
     * Devuelve al jugador una parte del costo original.
     *
     * @param defense La defensa a remover.
     */
    public void removeDefense(Defense defense) {
        if (army.removeDefense(defense)) {
            coins += (int) (defense.getCost() * 0.5); // Recupera 50% del costo
        }
    }

    /**
     * Sube de nivel al jugador y aumenta la capacidad del ejército.
     */
    public void levelUp() {
        level++;
        // Fórmula simple para aumentar la capacidad con cada nivel
        int capacityIncrease = 10 + (level * 2);
        army.increaseCapacity(capacityIncrease);
        capacityBase += capacityIncrease;
    }

    /**
     * Añade monedas al jugador (ej. al matar un zombie).
     * @param amount La cantidad de monedas a añadir.
     */
    public void addCoins(int amount) {
        coins += amount;
    }

    /**
     * Intenta gastar monedas.
     * @param amount La cantidad a gastar.
     * @return true si tenía suficientes monedas y se gastaron, false en caso contrario.
     */
    public boolean spendCoins(int amount) {
        if (coins >= amount) {
            coins -= amount;
            return true;
        }
        return false;
    }

    /**
     * Añade puntos al score del jugador.
     * @param points Puntos a añadir.
     */
    public void addScore(int points) {
        score += points;
    }

    /**
     * Calcula la capacidad total del ejército.
     * @return El máximo de espacios permitidos.
     */
    public int getTotalCapacity() {
        return army.getMaxCapacity();
    }

    /**
     * Calcula la capacidad disponible (espacios libres).
     * @return Espacios libres en el ejército.
     */
    public int getAvailableCapacity() {
        return army.getAvailableSpaces();
    }

    /**
     * Verifica si el jugador cumple los requisitos para comprar una defensa.
     * (Usado por la UI para habilitar/deshabilitar botones de compra).
     *
     * @param defense La defensa a comprobar.
     * @return true si tiene monedas y espacio.
     */
    public boolean canBuy(Defense defense) {
        return coins >= defense.getCost() && army.hasCapacityFor(defense);
    }

    // --- Getters y Setters ---
    public String getName() { return name; }
    public int getLevel() { return level; }
    public int getCoins() { return coins; }
    public int getCapacityBase() { return capacityBase; }
    public Army getArmy() { return army; }
    public int getScore() { return score; }

    public void setName(String name) { this.name = name; }
    public void setLevel(int level) { this.level = level; }
    public void setCoins(int coins) { this.coins = coins; }
    public void setScore(int score) { this.score = score; }
    public void setCapacityBase(int capacityBase) {
        this.capacityBase = capacityBase;
    }

    @Override
    public String toString() {
        return String.format("Player: %s | Level: %d | Coins: %d | Score: %d | Capacity: %d/%d",
                name, level, coins, score, army.getUsedSpaces(), army.getMaxCapacity());
    }
}