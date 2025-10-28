package diblo.thewalkingtec.ui;

import diblo.thewalkingtec.service.Game;
import diblo.thewalkingtec.service.SaveManager;
import diblo.thewalkingtec.util.Logger;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Interfaz de usuario consolidada.
 * Actúa como el controlador principal de la UI, gestionando el Menú Principal
 * y lanzando el GameRenderer cuando se inicia o carga una partida.
 */
public class GameUI {
    private Stage primaryStage;
    private Game currentGame; // Instancia del juego actual
    private GameRenderer currentRenderer; // Instancia del renderizador actual

    /**
     * Inicia la aplicación de UI, mostrando el menú principal.
     * @param primaryStage El Stage principal de JavaFX.
     */
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("The Walking TEC");
        primaryStage.setOnCloseRequest(e -> cleanup()); // Asegura que se limpie al cerrar
        showMainMenu();
    }

    /**
     * Muestra la escena del Menú Principal.
     */
    private void showMainMenu() {
        VBox root = createMainMenu();
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Construye el VBox del Menú Principal con sus botones.
     */
    private VBox createMainMenu() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #2b2b2b;");

        Label titleLabel = new Label("The Walking Tec");
        titleLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #ff4444;");

        Label subtitleLabel = new Label("Protege la reliquia");
        subtitleLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #cccccc;");

        // Botones del menú
        Button newGameBtn = createMenuButton("Nueva Partida", this::showNewGameDialog);
        Button loadGameBtn = createMenuButton("Cargar Partida", this::loadGame);
        Button instructionsBtn = createMenuButton("Instrucciones", this::showInstructions);
        Button exitBtn = createMenuButton("Cerrar", () -> primaryStage.close());

        VBox buttonBox = new VBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(newGameBtn, loadGameBtn, instructionsBtn, exitBtn);

        root.getChildren().addAll(titleLabel, subtitleLabel, buttonBox);
        return root;
    }

    /**
     * Helper para crear un botón de menú estilizado.
     */
    private Button createMenuButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMinWidth(250);
        btn.setStyle(
                "-fx-font-size: 16px; " +
                        "-fx-background-color: #444444; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 15px; " +
                        "-fx-background-radius: 5px; " +
                        "-fx-cursor: hand;"
        );

        // Efecto Hover
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle().replace("#444444", "#555555")));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace("#555555", "#444444")));
        btn.setOnAction(e -> action.run());

        return btn;
    }

    /**
     * Muestra un diálogo para pedir el nombre del jugador.
     */
    private void showNewGameDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nueva Partida");
        dialog.setHeaderText("Ingrese el nombre del jugador:");
        dialog.setContentText("Nombre:");

        dialog.showAndWait().ifPresent(playerName -> {
            playerName = playerName.trim();
            if (!playerName.isEmpty()) {
                startNewGame(playerName); // Inicia el juego si el nombre es válido
            } else {
                showAlert(Alert.AlertType.WARNING, "Error", "Por favor ingrese un nombre válido");
            }
        });
    }

    /**
     * Inicia una nueva partida.
     * @param playerName El nombre del jugador.
     */
    private void startNewGame(String playerName) {
        Logger.info("Iniciando nueva partida para: " + playerName);
        cleanup(); // Limpia cualquier juego anterior

        currentGame = new Game(playerName); // Crea la instancia del motor
        currentRenderer = new GameRenderer(currentGame, this); // Crea el renderizador
        currentRenderer.start(primaryStage); // Muestra la escena del juego
        currentGame.start(true); // Inicia el bucle de juego
        currentRenderer.getPauseResumeBtn().setText("Pausar"); // Pone el botón en "Pausar"
    }

    /**
     * Muestra un FileChooser para cargar una partida guardada.
     */
    private void loadGame() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Cargar Partida");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos de guardado (*.json)", "*.json")
        );

        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            Logger.info("Cargando partida desde: " + file.getAbsolutePath());
            Game game = SaveManager.loadGame(file.getAbsolutePath()); // Intenta cargar

            if (game != null) {
                cleanup(); // Limpia juego anterior
                currentGame = game; // Asigna el juego cargado
                currentRenderer = new GameRenderer(currentGame, this);
                currentRenderer.start(primaryStage);
                currentGame.start(false); // Inicia bucle (false = no es juego nuevo)
                currentGame.resume(); // Asegura que inicie corriendo
                currentRenderer.getPauseResumeBtn().setText("Pausar");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "No se pudo cargar la partida. El archivo puede estar corrupto.");
            }
        }
    }

    /**
     * Muestra una alerta con las instrucciones del juego.
     */
    private void showInstructions() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Instrucciones");
        alert.setHeaderText("Cómo jugar The Walking TEC");
        alert.setContentText(
                "OBJETIVO:\n" +
                        "Defiende la reliquia en el centro del mapa de las oleadas de zombies.\n\n" +
                        "CONTROLES:\n" +
                        "• Click izquierdo: Colocar defensa o ver información\n" +
                        "• Click derecho: Remover defensa (recupera 50% del costo)\n\n" +
                        "MECÁNICAS:\n" +
                        "• Compra defensas con monedas\n" +
                        "• Cada defensa ocupa espacios en tu ejército\n" +
                        "• Los zombies atacan defensas y la reliquia\n" +
                        "• Completa todas las oleadas para ganar el nivel\n\n" +
                        "¡Buena suerte!"
        );
        alert.showAndWait();
    }

    /**
     * Método llamado por GameRenderer para volver al menú principal.
     */
    public void returnToMainMenu() {
        cleanup(); // Detiene el juego y el renderizador
        showMainMenu(); // Muestra el menú
    }

    /**
     * Limpia las instancias de Game y GameRenderer para liberar recursos.
     */
    private void cleanup() {
        if (currentRenderer != null) {
            currentRenderer.cleanup();
            currentRenderer = null;
        }
        if (currentGame != null && currentGame.isRunning()) {
            currentGame.stop();
            currentGame = null;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}