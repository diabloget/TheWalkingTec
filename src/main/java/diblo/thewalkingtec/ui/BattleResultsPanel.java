package diblo.thewalkingtec.ui;

import diblo.thewalkingtec.model.Component;
import diblo.thewalkingtec.model.Defense;
import diblo.thewalkingtec.model.LogEntry;
import diblo.thewalkingtec.model.Zombie;
import diblo.thewalkingtec.service.Game;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel (Ventana Modal) que muestra los resultados detallados de la batalla.
 * Cumple con el requisito del PDF: "mostrar información de todos los componentes
 * después de la batalla".
 */
public class BattleResultsPanel {

    /**
     * Muestra la ventana modal de resultados.
     * @param game La instancia del juego (ya pausado o detenido).
     * @param won true si el jugador ganó el nivel, false si perdió.
     * @param owner La ventana principal (Stage) para bloquearla.
     */
    public static void showResults(Game game, boolean won, Stage owner) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL); // Bloquea la ventana de atrás
        dialog.initOwner(owner);
        dialog.setTitle("Resultados de la Batalla - Nivel " + game.getCurrentLevel().getLevelNumber());

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2b2b2b;"); // Fondo oscuro
        root.setPadding(new Insets(15));

        // --- 1. Título (Victoria/Derrota) ---
        Label titleLabel = new Label(won ? "¡VICTORIA!" : "DERROTA");
        titleLabel.setStyle(
                "-fx-font-size: 24px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: " + (won ? "#00ff00" : "#ff0000") + ";" // Verde o Rojo
        );

        Label subtitleLabel = new Label(
                String.format("Nivel %d | Jugador: %s | Puntos: %d",
                        game.getCurrentLevel().getLevelNumber(),
                        game.getPlayer().getName(),
                        game.getPlayer().getScore())
        );
        subtitleLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 14px;");

        VBox header = new VBox(10, titleLabel, subtitleLabel);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(10));

        // --- 2. Panel de Pestañas ---
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: #2b2b2b;");

        // Tab: Resumen (Vida Reliquia, Boosts, Log de Reliquia)
        Tab summaryTab = new Tab("Resumen");
        summaryTab.setClosable(false);
        summaryTab.setContent(createSummaryPanel(game, won));

        // Tab: Defensas (Lista de todas las defensas y sus logs)
        Tab defensesTab = new Tab("Defensas");
        defensesTab.setClosable(false);
        // Nota: game.getBoard().getActiveDefenses() al final de la batalla
        // incluye las defensas VIVAS y las DESTRUIDAS (aún no limpiadas).
        defensesTab.setContent(createComponentsPanel(game.getBoard().getActiveDefenses(), "Defensas"));

        // Tab: Zombies (Lista de todos los zombies y sus logs)
        Tab zombiesTab = new Tab("Zombies");
        zombiesTab.setClosable(false);
        zombiesTab.setContent(createComponentsPanel(
                new ArrayList<>(game.getBoard().getActiveZombies()), // Incluye vivos y muertos
                "Zombies"
        ));

        tabPane.getTabs().addAll(summaryTab, defensesTab, zombiesTab);

        // --- 3. Botón de Cierre ---
        Button closeBtn = new Button("Cerrar");
        closeBtn.setOnAction(e -> dialog.close());
        closeBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10px 30px;");

        HBox buttonBox = new HBox(closeBtn);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));

        // --- Ensamblaje ---
        root.setTop(header);
        root.setCenter(tabPane);
        root.setBottom(buttonBox);

        Scene scene = new Scene(root, 900, 600); // Ventana grande
        dialog.setScene(scene);
        dialog.showAndWait(); // Muestra y espera a que se cierre
    }

    /**
     * Crea el contenido de la pestaña "Resumen".
     */
    private static VBox createSummaryPanel(Game game, boolean won) {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: #333333;");

        // Estado general
        Label statusLabel = new Label("Estado: " + (won ? "VICTORIA" : "DERROTA"));
        statusLabel.setStyle(
                "-fx-font-size: 18px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: " + (won ? "#00ff00" : "#ff0000") + ";"
        );

        // Vida de la Reliquia
        Label relicLabel = new Label(
                String.format("Vida de Reliquia: %d/%d (%.1f%%)",
                        game.getRelicLife(),
                        game.getMaxRelicLife(),
                        (double) game.getRelicLife() / game.getMaxRelicLife() * 100)
        );
        relicLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        // Conteo de Defensas
        int totalDefenses = game.getBoard().getActiveDefenses().size();
        int survivingDefenses = (int) game.getBoard().getActiveDefenses().stream()
                .filter(d -> !d.isDestroyed())
                .count();
        Label defenseLabel = new Label(
                String.format("Defensas: %d/%d sobrevivieron", survivingDefenses, totalDefenses)
        );
        defenseLabel.setStyle("-fx-text-fill: cyan; -fx-font-size: 14px;");

        // Conteo de Zombies
        int totalZombies = game.getBoard().getActiveZombies().size();
        int destroyedZombies = (int) game.getBoard().getActiveZombies().stream()
                .filter(Component::isDestroyed)
                .count();
        Label zombieLabel = new Label(
                String.format("Zombies: %d/%d eliminados", destroyedZombies, totalZombies)
        );
        zombieLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");

        // Boosts del Nivel
        Label boostLabel = new Label(
                String.format("Boosts de Nivel: Defensas +%.1f%% | Enemigos +%.1f%%",
                        game.getCurrentDefenseBoost() * 100,
                        game.getCurrentEnemyBoost() * 100)
        );
        boostLabel.setStyle("-fx-text-fill: #ffaa00; -fx-font-size: 12px;");

        // Log de Daño a la Reliquia
        Label logTitle = new Label("Registro de Daño a la Reliquia:");
        logTitle.setStyle("-fx-text-fill: #ffaa00; -fx-font-size: 12px; -fx-font-weight: bold;");

        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(8);
        logArea.setStyle(
                "-fx-control-inner-background: #1a1a1a; " +
                        "-fx-text-fill: #aaaaaa; " +
                        "-fx-font-family: 'Courier New'; " + // Fuente monoespaciada
                        "-fx-font-size: 9px;"
        );

        // Recolecta logs de todos los zombies que atacaron la reliquia
        List<LogEntry> relicLogs = new ArrayList<>();
        for (Zombie zombie : game.getBoard().getActiveZombies()) {
            for (LogEntry log : zombie.getInteractionsLog()) {
                if (log.getDefenderId() == null) { // 'null' ID significa que el defensor es la Reliquia
                    relicLogs.add(log);
                }
            }
        }

        StringBuilder logText = new StringBuilder();
        if (relicLogs.isEmpty()) {
            logText.append("La reliquia no recibió daño.");
        } else {
            for (LogEntry log : relicLogs) {
                logText.append(log.toString()).append("\n");
            }
        }
        logArea.setText(logText.toString());
        logArea.setScrollTop(Double.MAX_VALUE); // Scroll al final

        panel.getChildren().addAll(
                statusLabel, new Separator(),
                relicLabel, defenseLabel, zombieLabel, new Separator(),
                boostLabel, new Separator(),
                logTitle, logArea
        );

        return panel;
    }

    /**
     * Crea el contenido de las pestañas "Defensas" y "Zombies".
     * @param components La lista de componentes (Defensas o Zombies) a mostrar.
     * @param type El nombre (ej. "Defensas").
     * @return Un ScrollPane que contiene las tarjetas de componentes.
     */
    private static ScrollPane createComponentsPanel(List<? extends Component> components, String type) {
        VBox container = new VBox(10); // Contenedor vertical para las tarjetas
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: #333333;");

        if (components.isEmpty()) {
            Label emptyLabel = new Label("No hay " + type.toLowerCase() + " en esta batalla");
            emptyLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px;");
            container.getChildren().add(emptyLabel);
        } else {
            // Crea una "tarjeta" (VBox) para cada componente en la lista
            for (Component component : components) {
                VBox componentCard = createComponentCard(component);
                container.getChildren().add(componentCard);
            }
        }

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #333333; -fx-background-color: #333333;");

        return scrollPane;
    }

    /**
     * Crea una "tarjeta" individual (VBox) que muestra toda la info de un componente,
     * incluyendo su log de combate (Requisito PDF).
     *
     * @param component El componente a mostrar.
     * @return Un VBox formateado como una tarjeta.
     */
    private static VBox createComponentCard(Component component) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));

        // Borde verde si sobrevivió, rojo si fue destruido
        String borderColor = component.isDestroyed() ? "#ff0000" : "#00ff00";
        card.setStyle(
                "-fx-background-color: #2a2a2a; " +
                        "-fx-border-color: " + borderColor + "; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 5px; " +
                        "-fx-background-radius: 5px;"
        );

        // Título (Nombre y Estado)
        String typeIcon = component instanceof Defense ? "🛡️" : "🧟";
        Label nameLabel = new Label(typeIcon + " " + component.getName() +
                (component.isDestroyed() ? " [DESTRUIDO]" : " [SOBREVIVIÓ]"));
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        // Stats (Tipo, Posición)
        Label statsLabel = new Label(String.format(
                "Tipo: %s | Posición: %s | Nivel: %d",
                component.getType().getDisplayName(),
                component.getPosition(),
                component.getLevel()
        ));
        statsLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 11px;");

        // Stats (Vida, Daño)
        Label lifeLabel = new Label(String.format(
                "Vida: %d/%d (%.1f%%) | Daño: %d | Golpes/seg: %.1f",
                component.getCurrentLife(),
                component.getMaxLife(),
                component.getLifePercentage() * 100,
                component.getDamagePerHit(),
                component.getHitsPerSecond()
        ));
        lifeLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 11px;");

        // Registro de combate (Logs)
        List<LogEntry> logs = component.getInteractionsLog();
        if (!logs.isEmpty()) {
            Label logTitle = new Label("Registro de Combate (" + logs.size() + " interacciones):");
            logTitle.setStyle("-fx-text-fill: #ffaa00; -fx-font-size: 11px; -fx-font-weight: bold;");

            TextArea logArea = new TextArea();
            logArea.setEditable(false);
            logArea.setPrefRowCount(5);
            logArea.setStyle(
                    "-fx-control-inner-background: #1a1a1a; " +
                            "-fx-text-fill: #aaaaaa; " +
                            "-fx-font-family: 'Courier New'; " +
                            "-fx-font-size: 9px;"
            );

            StringBuilder logText = new StringBuilder();
            // Muestra solo las últimas 20 entradas para no saturar
            int start = Math.max(0, logs.size() - 20);
            for (int i = start; i < logs.size(); i++) {
                logText.append(logs.get(i).toString()).append("\n");
            }
            logArea.setText(logText.toString());

            card.getChildren().addAll(nameLabel, statsLabel, lifeLabel, new Separator(), logTitle, logArea);
        } else {
            // Si no tiene logs
            Label noLogLabel = new Label("Sin interacciones de combate");
            noLogLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 10px; -fx-font-style: italic;");
            card.getChildren().addAll(nameLabel, statsLabel, lifeLabel, noLogLabel);
        }

        return card;
    }
}