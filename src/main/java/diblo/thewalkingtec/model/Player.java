package diblo.thewalkingtec.model;

import java.io.Serializable;

/**
 * Representa al jugador del juego
 */
public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private int level;
    private int coins; // monedas para comprar defensas
    private int capacityBase; // capacidad base del ejército
    private Army army;
    private int score;

    public Player(String name, int initialCapacity, int initialCoins) {
        this.name = name;
        this.level = 1;
        this.capacityBase = initialCapacity;
        this.coins = initialCoins;
        this.army = new Army(initialCapacity);
        this.score = 0;
    }

    /**
     * Intenta colocar una defensa en el tablero
     */
    public boolean placeDefense(Defense defense) {
        if (coins < defense.getCost()) {
            return false; // No hay suficientes monedas
        }

        if (!army.hasCapacityFor(defense)) {
            return false; // No hay capacidad en el ejército
        }

        if (army.addDefense(defense)) {
            coins -= defense.getCost();
            return true;
        }

        return false;
    }

    /**
     * Remueve una defensa del ejército (recupera parte del costo)
     */
    public void removeDefense(Defense defense) {
        if (army.removeDefense(defense)) {
            coins += (int) (defense.getCost() * 0.5); // recupera 50% del costo
        }
    }

    /**
     * Sube de nivel y aumenta la capacidad del ejército
     */
    public void levelUp() {
        level++;
        int capacityIncrease = 10 + (level * 2); // más capacidad cada nivel
        army.increaseCapacity(capacityIncrease);
        capacityBase += capacityIncrease;
    }

    /**
     * Añade monedas al jugador
     */
    public void addCoins(int amount) {
        coins += amount;
    }

    /**
     * Resta monedas al jugador
     */
    public boolean spendCoins(int amount) {
        if (coins >= amount) {
            coins -= amount;
            return true;
        }
        return false;
    }

    /**
     * Añade puntos al score
     */
    public void addScore(int points) {
        score += points;
    }

    /**
     * Calcula la capacidad total del ejército
     */
    public int getTotalCapacity() {
        return army.getMaxCapacity();
    }

    /**
     * Calcula la capacidad disponible
     */
    public int getAvailableCapacity() {
        return army.getAvailableSpaces();
    }

    /**
     * Verifica si puede comprar una defensa
     */
    public boolean canBuy(Defense defense) {
        return coins >= defense.getCost() && army.hasCapacityFor(defense);
    }

    // Getters
    public String getName() { return name; }
    public int getLevel() { return level; }
    public int getCoins() { return coins; }
    public int getCapacityBase() { return capacityBase; }
    public Army getArmy() { return army; }
    public int getScore() { return score; }

    // Setters
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