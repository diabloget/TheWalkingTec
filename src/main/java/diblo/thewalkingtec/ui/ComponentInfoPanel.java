package diblo.thewalkingtec.ui;

import diblo.thewalkingtec.model.Component;
import diblo.thewalkingtec.model.Defense;
import diblo.thewalkingtec.model.LogEntry;
import diblo.thewalkingtec.model.Zombie;
import diblo.thewalkingtec.service.Game;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel para mostrar información detallada de un componente
 */
public class ComponentInfoPanel {
    private VBox panel;
    private Label titleLabel;
    private Label statsLabel;
    private TextArea logArea;
    private Component currentComponent;
    private Game game;

    public ComponentInfoPanel(Game game) { // <--- MODIFICAR CONSTRUCTOR
        this.game = game; // <--- AÑADIR
        createPanel();
    }

    private void createPanel() {
        panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setPrefHeight(200);
        panel.setStyle("-fx-background-color: #333333;");

        titleLabel = new Label("Selecciona un componente para ver su información");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        statsLabel = new Label("");
        statsLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 12px;");
        statsLabel.setWrapText(true);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(120);
        logArea.setStyle(
                "-fx-control-inner-background: #2b2b2b; " +
                        "-fx-text-fill: #aaaaaa; " +
                        "-fx-font-family: 'Courier New'; " +
                        "-fx-font-size: 10px;"
        );

        ScrollPane logScroll = new ScrollPane(logArea);
        logScroll.setFitToWidth(true);

        panel.getChildren().addAll(titleLabel, statsLabel, logScroll);
    }

    public void showComponentInfo(Component component) {
        this.currentComponent = component;

        String componentType = component instanceof Defense ? "DEFENSA" : "ZOMBIE";
        titleLabel.setText(componentType + ": " + component.getName());

        StringBuilder stats = new StringBuilder();
        stats.append(String.format("Vida: %d/%d (%.0f%%)\n",
                component.getCurrentLife(), component.getMaxLife(),
                component.getLifePercentage() * 100));
        stats.append(String.format("Daño: %d | Golpes/seg: %.1f\n",
                component.getDamagePerHit(), component.getHitsPerSecond()));
        stats.append(String.format("Nivel: %d | Espacios: %d\n",
                component.getLevel(), component.getFields()));
        stats.append(String.format("Posición: %s\n", component.getPosition()));

        if (component instanceof Defense) {
            Defense defense = (Defense) component;
            stats.append(String.format("Rango: %d | Objetivos: %d | Costo: %d",
                    defense.getRange(), defense.getMaxTargetsSimultaneous(), defense.getCost()));
        } else if (component instanceof Zombie) {
            Zombie zombie = (Zombie) component;
            stats.append(String.format("Velocidad: %.1f | IA: %s",
                    zombie.getMovementSpeed(), zombie.getAiType().name()));
        }

        statsLabel.setText(stats.toString());

        List<LogEntry> logs = component.getInteractionsLog();
        StringBuilder logText = new StringBuilder();
        logText.append("=== REGISTRO DE COMBATE ===\n");

        if (logs.isEmpty()) {
            logText.append("Sin interacciones registradas\n");
        } else {
            int start = Math.max(0, logs.size() - 20);
            for (int i = start; i < logs.size(); i++) {
                logText.append(logs.get(i).toString()).append("\n");
            }
        }

        logArea.setText(logText.toString());
        logArea.setScrollTop(Double.MAX_VALUE);
    }

    public void showRelicInfo() {
        this.currentComponent = null; // No es un 'component' estándar

        titleLabel.setText("OBJETIVO: Reliquia");

        // Mostrar stats de la reliquia
        StringBuilder stats = new StringBuilder();
        stats.append(String.format("Vida: %d/%d (%.0f%%)\n",
                game.getRelicLife(), game.getMaxRelicLife(),
                (double) game.getRelicLife() / game.getMaxRelicLife() * 100));
        stats.append("Posición: " + game.getRelicPosition().toString() + "\n");
        stats.append("Si la vida llega a 0, pierdes la batalla.");
        statsLabel.setText(stats.toString());

        // Recolectar logs de todos los zombies que atacaron la reliquia
        List<LogEntry> relicLogs = new ArrayList<>();
        for (Zombie zombie : game.getBoard().getActiveZombies()) {
            for (LogEntry log : zombie.getInteractionsLog()) {
                // El 'defenderId' es null para la reliquia (según Zombie.java)
                if (log.getDefenderId() == null) {
                    relicLogs.add(log);
                }
            }
        }

        // Mostrar logs
        StringBuilder logText = new StringBuilder();
        logText.append("=== REGISTRO DE DAÑO RECIBIDO ===\n");

        if (relicLogs.isEmpty()) {
            logText.append("Sin daño registrado\n");
        } else {
            int start = Math.max(0, relicLogs.size() - 20);
            for (int i = start; i < relicLogs.size(); i++) {
                logText.append(relicLogs.get(i).toString()).append("\n");
            }
        }
        logArea.setText(logText.toString());
        logArea.setScrollTop(Double.MAX_VALUE);
    }

    public void clear() {
        currentComponent = null;
        titleLabel.setText("Selecciona un componente para ver su información");
        statsLabel.setText("");
        logArea.setText("");
    }

    public boolean isShowingComponent(Component component) {
        return currentComponent != null && currentComponent.getId().equals(component.getId());
    }

    public VBox getPanel() {
        return panel;
    }
}