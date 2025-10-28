package diblo.thewalkingtec.ui;

import diblo.thewalkingtec.model.*;
import diblo.thewalkingtec.model.config.DefenseConfig;
import diblo.thewalkingtec.model.config.LevelConfig;
import diblo.thewalkingtec.service.Board;
import diblo.thewalkingtec.service.Game;
import diblo.thewalkingtec.service.SaveManager;
import diblo.thewalkingtec.util.Logger;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameRenderer {
    private static final int CELL_SIZE = 25;
    private static final int BOARD_SIZE = Board.SIZE;
    private static final int CANVAS_SIZE = CELL_SIZE * BOARD_SIZE;

    private Game game;
    private GameUI gameUI;
    private Stage stage;
    private Canvas canvas;
    private GraphicsContext gc;
    private AnimationTimer gameLoop;

    // UI Components
    private Label levelLabel;
    private Label coinsLabel;
    private Label capacityLabel;
    private Label relicLabel;
    private Label scoreLabel;
    private Button pauseResumeBtn;
    private VBox defenseShop;
    private ComponentInfoPanel infoPanel;

    // Game state
    private Defense selectedDefense;
    private Position hoveredCell;
    private Map<String, Image> imageCache = new HashMap<>();

    public GameRenderer(Game game, GameUI gameUI) {
        this.game = game;
        this.gameUI = gameUI;
    }

    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("The Walking TEC - " + game.getPlayer().getName());

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a1a;");

        canvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
        gc = canvas.getGraphicsContext2D();

        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setStyle("-fx-background-color: #2b2b2b;");
        root.setCenter(canvasPane);

        root.setTop(createTopBar());
        root.setRight(createRightPanel());

        infoPanel = new ComponentInfoPanel(game);
        root.setBottom(infoPanel.getPanel());

        setupCanvasEvents();
        setupGameListeners();
        startRenderLoop();

        //game.start(true); // Iniciar el juego

        Scene scene = new Scene(root, 1400, 800);
        stage.setScene(scene);
        stage.show();

        Logger.info("GameRenderer iniciado");
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(10));
        topBar.setStyle("-fx-background-color: #333333;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Player player = game.getPlayer();

        levelLabel = new Label("Nivel: " + game.getCurrentLevel().getLevelNumber());
        levelLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        coinsLabel = new Label("Monedas: " + player.getCoins());
        coinsLabel.setStyle("-fx-text-fill: gold; -fx-font-size: 14px; -fx-font-weight: bold;");

        capacityLabel = new Label(String.format("Capacidad: %d/%d",
                player.getArmy().getUsedSpaces(), player.getTotalCapacity()));
        capacityLabel.setStyle("-fx-text-fill: cyan; -fx-font-size: 14px; -fx-font-weight: bold;");

        relicLabel = new Label(String.format("Reliquia: %d/%d",
                game.getRelicLife(), game.getMaxRelicLife()));
        relicLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px; -fx-font-weight: bold;");

        scoreLabel = new Label("Puntos: " + player.getScore());
        scoreLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        pauseResumeBtn = new Button(game.isPaused() ? "Reanudar" : "Pausar");
        pauseResumeBtn.setOnAction(e -> togglePause());

        Button saveBtn = new Button("Guardar");
        saveBtn.setOnAction(e -> saveGame());

        Button menuBtn = new Button("Menú");
        menuBtn.setOnAction(e -> returnToMenu());

        topBar.getChildren().addAll(levelLabel, coinsLabel, capacityLabel, relicLabel, scoreLabel,
                spacer, pauseResumeBtn, saveBtn, menuBtn);

        return topBar;
    }

    private void togglePause() {
        if (game.isPaused()) {
            game.resume();
            pauseResumeBtn.setText("Pausar");
        } else {
            game.pause();
            pauseResumeBtn.setText("Reanudar");
        }
    }

    private VBox createRightPanel() {
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(10));
        rightPanel.setPrefWidth(300);
        rightPanel.setStyle("-fx-background-color: #2b2b2b;");

        Label shopTitle = new Label("TIENDA DE DEFENSAS");
        shopTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        defenseShop = new VBox(10);
        ScrollPane shopScroll = new ScrollPane(defenseShop);
        shopScroll.setFitToWidth(true);
        shopScroll.setStyle("-fx-background: #2b2b2b; -fx-background-color: #2b2b2b;");

        populateDefenseShop();

        rightPanel.getChildren().addAll(shopTitle, shopScroll);
        return rightPanel;
    }

    private void populateDefenseShop() {
        defenseShop.getChildren().clear();
        List<DefenseConfig> availableDefenses = game.getGameConfig().getDefenses();

        for (DefenseConfig defenseConfig : availableDefenses) {
            if (game.getPlayer().getLevel() >= defenseConfig.getUnlockLevel()) {
                VBox defenseCard = createDefenseCard(defenseConfig);
                defenseShop.getChildren().add(defenseCard);
            }
        }
    }

    private VBox createDefenseCard(DefenseConfig defenseConfig) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));

        // Comprueba si esta tarjeta es la que está seleccionada actualmente
        // Nota: Comparamos el ID del config con el ID de la instancia seleccionada
        boolean isSelected = selectedDefense != null && selectedDefense.getId().equals(defenseConfig.getId());
        String borderColor = isSelected ? "#00ff00" : "#555555"; // Verde si está seleccionada

        card.setStyle(
                "-fx-background-color: #3a3a3a; " +
                        "-fx-border-color: " + borderColor + "; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 5px; " +
                        "-fx-background-radius: 5px;"
        );

        Label nameLabel = new Label(defenseConfig.getName());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Label typeLabel = new Label("Tipo: " + defenseConfig.getType());
        typeLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 11px;");

        Label statsLabel = new Label(String.format("Vida: %d | Daño: %d | Rango: %d",
                defenseConfig.getBaseHealth(), defenseConfig.getBaseDamage(), defenseConfig.getRange()));
        statsLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 10px;");

        // --- CORRECCIÓN 1: USAR getFields() ---
        // Tu código anterior usaba getCost() para los espacios
        Label costLabel = new Label(String.format("Costo: %d | Espacios: %d",
                defenseConfig.getCost(),
                defenseConfig.getFields() // <-- ARREGLADO
        ));
        costLabel.setStyle("-fx-text-fill: gold; -fx-font-size: 11px;");

        Button buyBtn = new Button(isSelected ? "Seleccionada" : "Seleccionar");
        buyBtn.setMaxWidth(Double.MAX_VALUE);
        buyBtn.setDisable(isSelected);

        // --- CORRECCIÓN 2: LÓGICA DE VALIDACIÓN Y ASIGNACIÓN (FIX DEL BUG DE LA IMAGEN) ---
        buyBtn.setOnAction(e -> {
            // 1. Validar Dinero
            if (game.getPlayer().getCoins() < defenseConfig.getCost()) {
                showAlert(Alert.AlertType.WARNING, "Fondos Insuficientes",
                        "No tienes suficientes monedas para comprar: " + defenseConfig.getName());
                this.selectedDefense = null; // Limpia la selección
                populateDefenseShop(); // Redibuja la tienda
                return;
            }

            // 2. Validar Capacidad del Ejército
            if (game.getPlayer().getArmy().getAvailableSpaces() < defenseConfig.getFields()) {
                showAlert(Alert.AlertType.WARNING, "Capacidad Llena",
                        "No tienes suficientes espacios en el ejército (" + defenseConfig.getFields() + " requeridos).");
                this.selectedDefense = null; // Limpia la selección
                populateDefenseShop(); // Redibuja la tienda
                return;
            }

            // 3. ÉXITO: Crear la instancia y seleccionarla
            // Aquí estaba el error de tipos. Creamos una instancia Defense
            Defense newDefenseInstance = game.createDefense(defenseConfig.getId());

            if (newDefenseInstance == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "No se pudo crear la instancia de la defensa.");
                this.selectedDefense = null;
            } else {
                // Asigna la INSTANCIA (Defense) a la variable (Defense)
                this.selectedDefense = newDefenseInstance;
            }

            // Redibuja la tienda para que este botón cambie a "Seleccionada"
            populateDefenseShop();
        });
        // --- FIN DE LA CORRECCIÓN ---

        card.getChildren().addAll(nameLabel, typeLabel, statsLabel, costLabel, buyBtn);
        return card;
    }

    private void setupCanvasEvents() {
        canvas.setOnMouseClicked(event -> {
            int cellX = (int) (event.getX() / CELL_SIZE);
            int cellY = (int) (event.getY() / CELL_SIZE);
            Position clickedPos = new Position(cellX, cellY);

            if (event.getButton() == MouseButton.PRIMARY) {
                handleLeftClick(clickedPos);
            } else if (event.getButton() == MouseButton.SECONDARY) {
                handleRightClick(clickedPos);
            }
        });

        canvas.setOnMouseMoved(event -> {
            int cellX = (int) (event.getX() / CELL_SIZE);
            int cellY = (int) (event.getY() / CELL_SIZE);
            hoveredCell = new Position(cellX, cellY);
        });
    }

    private void handleLeftClick(Position pos) {
        if (selectedDefense != null) {
            if (game.placeDefense(selectedDefense, pos)) {
                selectedDefense = null;
                populateDefenseShop();
                updateInfoBar();
            }
        } else {
            // Primero, verificar si se hizo click en un componente (zombie/defensa)
            Component component = game.getBoard().getComponentAt(pos);
            if (component != null) {
                infoPanel.showComponentInfo(component);
            }
            // Si no hay componente, verificar si se hizo click en la Reliquia
            else if (pos.equals(game.getRelicPosition())) {
                infoPanel.showRelicInfo(); // Usar un nuevo método para la reliquia
            } else {
                // Si se hace clic en una celda vacía, limpia el panel
                infoPanel.clear();
            }
        }
    }

    private void handleRightClick(Position pos) {
        Defense defense = game.getBoard().getDefenseAt(pos);
        if (defense != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmar");
            confirm.setHeaderText("¿Remover defensa?");
            confirm.setContentText("Recuperarás " + (defense.getCost() / 2) + " monedas.");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    game.removeDefense(defense);
                    updateInfoBar();
                    infoPanel.clear();
                }
            });
        }
    }

    private void setupGameListeners() {
        game.addGameEventListener(new Game.GameEventListener() {
            @Override
            public void onLevelCompleted(LevelConfig level) {
                Platform.runLater(() -> showLevelCompletedDialog(level));
            }

            @Override
            public void onGameWon() {
                Platform.runLater(() -> showGameWonDialog());
            }

            @Override
            public void onGameLost() {
                Platform.runLater(() -> showGameLostDialog());
            }

            @Override
            public void onMaxLevelReached() {
                Platform.runLater(() -> showMaxLevelDialog());
            }
        });
    }

    private void showLevelCompletedDialog(LevelConfig level) {
        // Mostrar panel de resultados primero
        BattleResultsPanel.showResults(game, true, stage);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("¡Nivel Completado!");
        alert.setHeaderText("Has completado el nivel " + level.getLevelNumber());
        alert.setContentText("¿Continuar al siguiente nivel?");

        ButtonType continueBtn = new ButtonType("Continuar", ButtonBar.ButtonData.OK_DONE);
        ButtonType menuBtn = new ButtonType("Menú", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(continueBtn, menuBtn);

        alert.showAndWait().ifPresent(response -> {
            if (response == continueBtn) {
                game.nextLevel();
            } else {
                returnToMenu();
            }
        });
    }

    private void showGameWonDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("¡Victoria!");
        alert.setHeaderText("¡Has completado todos los niveles!");
        alert.setContentText("Puntuación final: " + game.getPlayer().getScore());
        alert.showAndWait();
        returnToMenu();
    }

    private void showMaxLevelDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("¡Niveles Completados!");
        alert.setHeaderText("Has completado todos los niveles configurados");
        alert.setContentText(
                "Puntuación final: " + game.getPlayer().getScore() + "\n\n" +
                        "Para agregar más niveles, edita el archivo config.json"
        );
        alert.showAndWait();
        returnToMenu();
    }

    private void showGameLostDialog() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Derrota");
        alert.setHeaderText("La reliquia ha sido destruida");
        alert.setContentText("¿Qué deseas hacer?");

        ButtonType retryBtn = new ButtonType("Reintentar Nivel", ButtonBar.ButtonData.OK_DONE);
        ButtonType skipBtn = new ButtonType("Avanzar al Siguiente", ButtonBar.ButtonData.OTHER);
        ButtonType menuBtn = new ButtonType("Menú", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(retryBtn, skipBtn, menuBtn);

        alert.showAndWait().ifPresent(response -> {
            if (response == retryBtn) {
                game.startLevel(game.getCurrentLevelIndex());
            } else if (response == skipBtn) {
                // --- INICIO DE LA CORRECCIÓN ---
                // Ya no se permite avanzar al perder.
                showAlert(Alert.AlertType.ERROR, "Acción no permitida", "Solo puedes avanzar al siguiente nivel si ganas la batalla.");
                // Se queda en el diálogo (o podrías llamar a returnToMenu() si prefieres)
                // --- FIN DE LA CORRECCIÓN ---
            } else {
                returnToMenu();
            }
        });
    }

    private void startRenderLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                render();
                updateInfoBar();
            }
        };
        gameLoop.start();
    }

    private void render() {
        gc.clearRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        gc.setFill(Color.rgb(30, 30, 30));
        gc.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);

        drawGrid();
        drawRelic();

        game.getBoard().getActiveDefenses().forEach(this::drawComponent);
        game.getBoard().getActiveZombies().forEach(this::drawComponent);

        if (selectedDefense != null && hoveredCell != null) {
            drawPlacementPreview(hoveredCell);
        }
    }

    private void drawGrid() {
        gc.setStroke(Color.rgb(50, 50, 50));
        gc.setLineWidth(1);

        for (int i = 0; i <= BOARD_SIZE; i++) {
            gc.strokeLine(i * CELL_SIZE, 0, i * CELL_SIZE, CANVAS_SIZE);
            gc.strokeLine(0, i * CELL_SIZE, CANVAS_SIZE, i * CELL_SIZE);
        }
    }

    private void drawRelic() {
        Position relicPos = game.getRelicPosition();
        int x = relicPos.getX() * CELL_SIZE;
        int y = relicPos.getY() * CELL_SIZE;

        // Ruta de la imagen de la reliquia (hardcoded como solicitaste)
        String relicImagePath = "diblo/thewalkingtec/images/reliq.png";

        // Usamos el método loadImage() que ya tienes, el cual usa el caché
        Image image = loadImage(relicImagePath);

        if (image != null) {
            // Si la imagen carga, la dibuja
            gc.drawImage(image, x, y, CELL_SIZE, CELL_SIZE);
        } else {
            // Si la imagen falla (ruta incorrecta, etc.), dibuja el círculo dorado como antes
            gc.setFill(Color.GOLD);
            gc.fillOval(x + 5, y + 5, CELL_SIZE - 10, CELL_SIZE - 10);
            gc.setStroke(Color.ORANGE);
            gc.setLineWidth(2);
            gc.strokeOval(x + 5, y + 5, CELL_SIZE - 10, CELL_SIZE - 10);
        }
    }

    private void drawComponent(Component component) {
        Position pos = component.getPosition();
        if (pos == null) return;

        int x = pos.getX() * CELL_SIZE;
        int y = pos.getY() * CELL_SIZE;

        // Dibujar imagen si existe, si no, usar color por defecto
        String imagePath = component.getImagePath();
        Image image = loadImage(imagePath);

        if (image != null) {
            gc.drawImage(image, x, y, CELL_SIZE, CELL_SIZE);
        } else {
            // Color por defecto según tipo
            if (component instanceof Defense) {
                gc.setFill(Color.BLUE);
            } else {
                gc.setFill(Color.RED);
            }
            gc.fillRect(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4);
        }

        drawHealthBar(x, y, component.getLifePercentage());
    }

    private Image loadImage(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        // Revisar si ya fue cargada o falló previamente
        if (imageCache.containsKey(path)) {
            return imageCache.get(path);
        }

        InputStream is = null;
        try {
            Image image = null;

            // 1️⃣ Intentar cargar desde el classpath (por ejemplo: "diblo/thewalkingtec/images/drone.png")
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
            if (is != null) {
                image = new Image(is);
                imageCache.put(path, image);
                return image;
            }

            // 2️⃣ Si no está en el classpath, intentar desde el sistema de archivos (path relativo o absoluto)
            File file = new File(path);
            if (!file.isAbsolute()) {
                // Si el path es relativo, tomar como base el directorio de ejecución
                file = new File(System.getProperty("user.dir"), path);
            }

            if (file.exists()) {
                image = new Image(new FileInputStream(file));
                imageCache.put(path, image);
                return image;
            }

            // 3️⃣ Si tampoco existe, registrar advertencia
            Logger.warning("No se encontró imagen: " + path);
            imageCache.put(path, null);
            return null;

        } catch (Exception e) {
            Logger.warning("Error al cargar imagen '" + path + "': " + e.getMessage());
            imageCache.put(path, null);
            return null;

        } finally {
            try {
                if (is != null) is.close();
            } catch (Exception ignored) {}
        }
    }

    private void drawHealthBar(int x, int y, double lifePercentage) {
        int barWidth = CELL_SIZE - 4;
        int barHeight = 3;
        int barX = x + 2;
        int barY = y + 1;

        gc.setFill(Color.RED);
        gc.fillRect(barX, barY, barWidth, barHeight);

        gc.setFill(Color.LIME);
        gc.fillRect(barX, barY, barWidth * lifePercentage, barHeight);
    }

    private void drawPlacementPreview(Position pos) {
        if (!game.getBoard().isValidPosition(pos)) return;

        int x = pos.getX() * CELL_SIZE;
        int y = pos.getY() * CELL_SIZE;

        boolean canPlace = !game.getBoard().isOccupiedByDefense(pos) &&
                game.getPlayer().getCoins() >= selectedDefense.getCost();

        Color previewColor = canPlace ? Color.rgb(0, 255, 0, 0.3) : Color.rgb(255, 0, 0, 0.3);
        gc.setFill(previewColor);
        gc.fillRect(x, y, CELL_SIZE, CELL_SIZE);

        if (canPlace && selectedDefense.getRange() > 0) {
            drawRange(pos, selectedDefense.getRange(), Color.rgb(0, 255, 255, 0.2));
        }
    }

    private void drawRange(Position center, int range, Color color) {
        gc.setStroke(color);
        gc.setLineWidth(2);
        int centerX = center.getX() * CELL_SIZE + CELL_SIZE / 2;
        int centerY = center.getY() * CELL_SIZE + CELL_SIZE / 2;
        int radius = range * CELL_SIZE;

        gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
    }

    private void updateInfoBar() {
        Platform.runLater(() -> {
            Player player = game.getPlayer();
            levelLabel.setText("Nivel: " + game.getCurrentLevel().getLevelNumber());
            coinsLabel.setText("Monedas: " + player.getCoins());
            capacityLabel.setText(String.format("Capacidad: %d/%d",
                    player.getArmy().getUsedSpaces(), player.getTotalCapacity()));
            relicLabel.setText(String.format("Reliquia: %d/%d",
                    game.getRelicLife(), game.getMaxRelicLife()));
            scoreLabel.setText("Puntos: " + player.getScore());
        });
    }

    private void updateSelectedCardVisuals(VBox selectedCard) {
        // defenseShop es el VBox que contiene todas las tarjetas
        defenseShop.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                if (node == selectedCard) {
                    // Estilo para la tarjeta seleccionada
                    node.setStyle(
                            "-fx-background-color: #555555; " +
                                    "-fx-border-color: #00ffff; " + // Borde cian
                                    "-fx-border-width: 2px; " +
                                    "-fx-cursor: hand;");
                } else {
                    // Estilo normal para las otras
                    node.setStyle(
                            "-fx-background-color: #444444; " +
                                    "-fx-border-color: #666666; " +
                                    "-fx-border-width: 1px; " +
                                    "-fx-cursor: hand;");
                }
            }
        });
    }

    private void saveGame() {
        game.pause();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Partida");
        fileChooser.setInitialFileName("partida.json"); // Corrected from config.json if it was wrong
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            if (SaveManager.saveGame(game, file.getAbsolutePath())) {
                showAlert(Alert.AlertType.INFORMATION, "Éxito", "Partida guardada correctamente");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "No se pudo guardar la partida");
            }
        }
        game.resume();
    }

    public Button getPauseResumeBtn() {
        return pauseResumeBtn;
    }

    private void returnToMenu() {
        cleanup();
        gameUI.returnToMainMenu();
    }

    public void cleanup() {
        if (gameLoop != null) {
            gameLoop.stop();
            gameLoop = null;
        }
        if (game != null && game.isRunning()) {
            game.stop();
        }
        imageCache.clear();
        Logger.info("GameRenderer limpiado");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}