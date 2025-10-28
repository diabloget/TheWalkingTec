package diblo.thewalkingtec.model.config;

public class WaveConfig {
    private String zombieId;
    private int quantity;
    private int delaySeconds;

    // Getters and Setters

    public String getZombieId() {
        return zombieId;
    }

    public void setZombieId(String zombieId) {
        this.zombieId = zombieId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(int delaySeconds) {
        this.delaySeconds = delaySeconds;
    }
}