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

/**
 * Clase principal de renderizado del juego.
 * Se encarga de dibujar el tablero (Canvas), la UI (paneles)
 * y manejar la interacción del usuario (clics, botones).
 */
public class GameRenderer {
    // --- Constantes de Renderizado ---
    private static final int CELL_SIZE = 25; // Tamaño de cada celda en píxeles
    private static final int BOARD_SIZE = Board.SIZE; // 25
    private static final int CANVAS_SIZE = CELL_SIZE * BOARD_SIZE; // 25 * 25 = 625px

    // --- Referencias ---
    private Game game; // El motor del juego
    private GameUI gameUI; // El controlador principal de la UI (para volver al menú)
    private Stage stage;
    private Canvas canvas; // El tablero donde se dibuja
    private GraphicsContext gc; // Pincel para el canvas
    private AnimationTimer gameLoop; // Bucle de renderizado (aprox. 60 FPS)

    // --- Componentes de la UI ---
    private Label levelLabel, coinsLabel, capacityLabel, relicLabel, scoreLabel;
    private Button pauseResumeBtn;
    private VBox defenseShop;
    private ComponentInfoPanel infoPanel; // Panel inferior

    // --- Estado de la UI ---
    private Defense selectedDefense; // Defensa seleccionada en la tienda (instancia)
    private Position hoveredCell; // Celda sobre la que está el mouse
    private Map<String, Image> imageCache = new HashMap<>(); // Caché de imágenes

    public GameRenderer(Game game, GameUI gameUI) {
        this.game = game;
        this.gameUI = gameUI;
    }

    /**
     * Inicia el renderizador, construye la escena y la muestra.
     * @param stage El Stage principal.
     */
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("The Walking TEC - " + game.getPlayer().getName());

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a1a;");

        // Centro: El canvas del juego
        canvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
        gc = canvas.getGraphicsContext2D();
        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setStyle("-fx-background-color: #2b2b2b;");
        root.setCenter(canvasPane);

        // Paneles de UI
        root.setTop(createTopBar()); // Barra de stats (monedas, nivel)
        root.setRight(createRightPanel()); // Tienda

        infoPanel = new ComponentInfoPanel(game); // Panel de información (abajo)
        root.setBottom(infoPanel.getPanel());

        // Eventos
        setupCanvasEvents(); // Clics y movimiento del mouse
        setupGameListeners(); // Eventos del motor (ej. "levelCompleted")
        startRenderLoop(); // Inicia el AnimationTimer

        Scene scene = new Scene(root, 1400, 800); // Tamaño total de la ventana
        stage.setScene(scene);
        stage.show();

        Logger.info("GameRenderer iniciado");
    }

    /**
     * Crea la barra superior con estadísticas del jugador y botones de control.
     */
    private HBox createTopBar() {
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(10));
        topBar.setStyle("-fx-background-color: #333333;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Player player = game.getPlayer();

        // Labels de Stats
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
        HBox.setHgrow(spacer, Priority.ALWAYS); // Empuja los botones a la derecha

        // Botones de Control
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

    /**
     * Cambia el estado de pausa del motor del juego.
     */
    private void togglePause() {
        if (game.isPaused()) {
            game.resume();
            pauseResumeBtn.setText("Pausar");
        } else {
            game.pause();
            pauseResumeBtn.setText("Reanudar");
        }
    }

    /**
     * Crea el panel derecho (Tienda de Defensas).
     */
    private VBox createRightPanel() {
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(10));
        rightPanel.setPrefWidth(300);
        rightPanel.setStyle("-fx-background-color: #2b2b2b;");

        Label shopTitle = new Label("TIENDA DE DEFENSAS");
        shopTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        defenseShop = new VBox(10); // Contenedor para las tarjetas de defensas
        ScrollPane shopScroll = new ScrollPane(defenseShop);
        shopScroll.setFitToWidth(true);
        shopScroll.setStyle("-fx-background: #2b2b2b; -fx-background-color: #2b2b2b;");

        populateDefenseShop(); // Llena la tienda

        rightPanel.getChildren().addAll(shopTitle, shopScroll);
        return rightPanel;
    }

    /**
     * (Re)llena la tienda con las defensas disponibles para el nivel actual.
     */
    private void populateDefenseShop() {
        defenseShop.getChildren().clear();
        List<DefenseConfig> availableDefenses = game.getGameConfig().getDefenses();

        for (DefenseConfig defenseConfig : availableDefenses) {
            // Solo muestra si el jugador tiene el nivel para desbloquearla
            if (game.getPlayer().getLevel() >= defenseConfig.getUnlockLevel()) {
                VBox defenseCard = createDefenseCard(defenseConfig);
                defenseShop.getChildren().add(defenseCard);
            }
        }
    }

    /**
     * Crea una "tarjeta" de UI para una defensa en la tienda.
     * @param defenseConfig La configuración de la defensa a mostrar.
     */
    private VBox createDefenseCard(DefenseConfig defenseConfig) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));

        // Comprueba si esta es la defensa seleccionada actualmente
        boolean isSelected = selectedDefense != null && selectedDefense.getId().equals(defenseConfig.getId());
        String borderColor = isSelected ? "#00ff00" : "#555555"; // Borde verde si está seleccionada

        card.setStyle(
                "-fx-background-color: #3a3a3a; " +
                        "-fx-border-color: " + borderColor + "; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 5px; " +
                        "-fx-background-radius: 5px;"
        );

        // Labels de información
        Label nameLabel = new Label(defenseConfig.getName());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Label typeLabel = new Label("Tipo: " + defenseConfig.getType());
        typeLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 11px;");

        Label statsLabel = new Label(String.format("Vida: %d | Daño: %d | Rango: %d",
                defenseConfig.getBaseHealth(), defenseConfig.getBaseDamage(), defenseConfig.getRange()));
        statsLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 10px;");

        Label costLabel = new Label(String.format("Costo: %d | Espacios: %d",
                defenseConfig.getCost(),
                defenseConfig.getFields() // Muestra los espacios
        ));
        costLabel.setStyle("-fx-text-fill: gold; -fx-font-size: 11px;");

        // Botón de Seleccionar/Comprar
        Button buyBtn = new Button(isSelected ? "Seleccionada" : "Seleccionar");
        buyBtn.setMaxWidth(Double.MAX_VALUE);
        buyBtn.setDisable(isSelected); // Deshabilitado si ya está seleccionada

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

            // 3. ÉXITO: Crea la INSTANCIA de la defensa y la selecciona
            Defense newDefenseInstance = game.createDefense(defenseConfig.getId());

            if (newDefenseInstance == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "No se pudo crear la instancia de la defensa.");
                this.selectedDefense = null;
            } else {
                this.selectedDefense = newDefenseInstance; // Guarda la instancia
            }

            populateDefenseShop(); // Redibuja la tienda para actualizar el botón
        });

        card.getChildren().addAll(nameLabel, typeLabel, statsLabel, costLabel, buyBtn);
        return card;
    }

    /**
     * Configura los listeners de clic y movimiento del mouse en el Canvas.
     */
    private void setupCanvasEvents() {
        canvas.setOnMouseClicked(event -> {
            int cellX = (int) (event.getX() / CELL_SIZE);
            int cellY = (int) (event.getY() / CELL_SIZE);
            Position clickedPos = new Position(cellX, cellY);

            if (event.getButton() == MouseButton.PRIMARY) {
                handleLeftClick(clickedPos); // Clic izquierdo
            } else if (event.getButton() == MouseButton.SECONDARY) {
                handleRightClick(clickedPos); // Clic derecho
            }
        });

        canvas.setOnMouseMoved(event -> {
            // Actualiza la celda sobre la que está el mouse (para la vista previa)
            int cellX = (int) (event.getX() / CELL_SIZE);
            int cellY = (int) (event.getY() / CELL_SIZE);
            hoveredCell = new Position(cellX, cellY);
        });
    }

    /**
     * Lógica para el Clic Izquierdo en el canvas.
     * Si hay una defensa seleccionada, intenta colocarla.
     * Si no, intenta mostrar información del componente en esa celda.
     */
    private void handleLeftClick(Position pos) {
        // 1. Intentar colocar defensa
        if (selectedDefense != null) {
            if (game.placeDefense(selectedDefense, pos)) {
                selectedDefense = null; // Limpia selección
                populateDefenseShop(); // Actualiza la tienda (para el botón)
                updateInfoBar(); // Actualiza monedas y capacidad
            }
        } else {
            // 2. Intentar mostrar información
            Component component = game.getBoard().getComponentAt(pos); // Busca CUALQUIER componente
            if (component != null) {
                infoPanel.showComponentInfo(component); // Muestra info del componente
            }
            // Si no hay componente, verificar si se hizo clic en la Reliquia
            else if (pos.equals(game.getRelicPosition())) {
                infoPanel.showRelicInfo(); // Muestra info de la Reliquia
            } else {
                // Si se hace clic en una celda vacía, limpia el panel
                infoPanel.clear();
            }
        }
    }

    /**
     * Lógica para el Clic Derecho en el canvas (Vender defensa).
     */
    private void handleRightClick(Position pos) {
        Defense defense = game.getBoard().getDefenseAt(pos); // Busca solo defensas
        if (defense != null) {
            // Pide confirmación para vender
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmar");
            confirm.setHeaderText("¿Remover defensa?");
            confirm.setContentText("Recuperarás " + (defense.getCost() / 2) + " monedas.");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    game.removeDefense(defense);
                    updateInfoBar(); // Actualiza monedas y capacidad
                    infoPanel.clear(); // Limpia el panel de info
                }
            });
        }
    }

    /**
     * Se suscribe a los eventos del motor del juego (Game.GameEventListener).
     * Esto permite que la UI reaccione a eventos como "Nivel Completado".
     */
    private void setupGameListeners() {
        game.addGameEventListener(new Game.GameEventListener() {
            @Override
            public void onLevelCompleted(LevelConfig level) {
                // Asegura que se ejecute en el hilo de la UI de JavaFX
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

    /**
     * Muestra el diálogo de Nivel Completado.
     */
    private void showLevelCompletedDialog(LevelConfig level) {
        // 1. Muestra primero el panel de resultados detallados
        BattleResultsPanel.showResults(game, true, stage);

        // 2. Muestra el diálogo simple de "Continuar"
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("¡Nivel Completado!");
        alert.setHeaderText("Has completado el nivel " + level.getLevelNumber());
        alert.setContentText("¿Continuar al siguiente nivel?");

        ButtonType continueBtn = new ButtonType("Continuar", ButtonBar.ButtonData.OK_DONE);
        ButtonType menuBtn = new ButtonType("Menú", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(continueBtn, menuBtn);

        alert.showAndWait().ifPresent(response -> {
            if (response == continueBtn) {
                game.nextLevel(); // Carga el siguiente nivel
            } else {
                returnToMenu(); // Vuelve al menú principal
            }
        });
    }

    /**
     * Muestra el diálogo de Juego Ganado (completó todos los niveles).
     */
    private void showGameWonDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("¡Victoria!");
        alert.setHeaderText("¡Has completado todos los niveles!");
        alert.setContentText("Puntuación final: " + game.getPlayer().getScore());
        alert.showAndWait();
        returnToMenu();
    }

    /**
     * Muestra el diálogo de que se acabaron los niveles configurados.
     */
    private void showMaxLevelDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("¡Niveles Completados!");
        alert.setHeaderText("Has completado todos los niveles configurados");
        alert.setContentText(
                "Puntuación final: " + game.getPlayer().getScore() + "\n\n" +
                        "Para agregar más niveles, usa el Editor de Configuración."
        );
        alert.showAndWait();
        returnToMenu();
    }

    /**
     * Muestra el diálogo de Juego Perdido (Reliquia destruida).
     */
    private void showGameLostDialog() {
        // 1. Muestra el panel de resultados detallados
        BattleResultsPanel.showResults(game, false, stage);

        // 2. Muestra el diálogo de "Reintentar"
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Derrota");
        alert.setHeaderText("La reliquia ha sido destruida");
        alert.setContentText("¿Qué deseas hacer?");

        ButtonType retryBtn = new ButtonType("Reintentar Nivel", ButtonBar.ButtonData.OK_DONE);
        ButtonType menuBtn = new ButtonType("Menú", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(retryBtn, menuBtn); // Botón de "Avanzar" eliminado

        alert.showAndWait().ifPresent(response -> {
            if (response == retryBtn) {
                game.startLevel(game.getCurrentLevelIndex()); // Reinicia el nivel actual
            } else {
                returnToMenu();
            }
        });
    }

    /**
     * Inicia el AnimationTimer (bucle de renderizado).
     */
    private void startRenderLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                render(); // Dibuja el estado actual del juego
                updateInfoBar(); // Actualiza los labels de la UI
            }
        };
        gameLoop.start();
    }

    /**
     * Método principal de dibujado (se llama ~60 veces por segundo).
     */
    private void render() {
        // 1. Limpia el canvas
        gc.clearRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        gc.setFill(Color.rgb(30, 30, 30)); // Fondo del tablero
        gc.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);

        // 2. Dibuja la cuadrícula y la reliquia
        drawGrid();
        drawRelic();

        // 3. Dibuja todos los componentes (Defensas y Zombies)
        game.getBoard().getActiveDefenses().forEach(this::drawComponent);
        game.getBoard().getActiveZombies().forEach(this::drawComponent);

        // 4. Dibuja la vista previa de colocación (si hay una defensa seleccionada)
        if (selectedDefense != null && hoveredCell != null) {
            drawPlacementPreview(hoveredCell);
        }
    }

    private void drawGrid() {
        gc.setStroke(Color.rgb(50, 50, 50)); // Color de la cuadrícula
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

        String relicImagePath = "diblo/thewalkingtec/images/reliq.png";
        Image image = loadImage(relicImagePath);

        if (image != null) {
            gc.drawImage(image, x, y, CELL_SIZE, CELL_SIZE);
        } else {
            // Fallback por si la imagen no carga
            gc.setFill(Color.GOLD);
            gc.fillOval(x + 5, y + 5, CELL_SIZE - 10, CELL_SIZE - 10);
        }
    }

    private void drawComponent(Component component) {
        Position pos = component.getPosition();
        if (pos == null) return;

        int x = pos.getX() * CELL_SIZE;
        int y = pos.getY() * CELL_SIZE;

        Image image = loadImage(component.getImagePath());

        if (image != null) {
            gc.drawImage(image, x, y, CELL_SIZE, CELL_SIZE);
        } else {
            // Fallback si la imagen no carga
            gc.setFill(component instanceof Defense ? Color.BLUE : Color.RED);
            gc.fillRect(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4);
        }

        // Dibuja la barra de vida encima del componente
        drawHealthBar(x, y, component.getLifePercentage());
    }

    /**
     * Carga una imagen desde el caché, o la carga desde el
     * ClassLoader (recursos internos) o el sistema de archivos (editor).
     */
    private Image loadImage(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        if (imageCache.containsKey(path)) {
            return imageCache.get(path); // Devuelve desde caché
        }

        InputStream is = null;
        try {
            Image image = null;
            // 1. Intenta cargar desde recursos internos (Classpath)
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
            if (is != null) {
                image = new Image(is);
                imageCache.put(path, image); // Guarda en caché
                return image;
            }

            // 2. Intenta cargar desde sistema de archivos (para el editor)
            File file = new File(path);
            if (file.exists()) {
                image = new Image(new FileInputStream(file));
                imageCache.put(path, image); // Guarda en caché
                return image;
            }

            // 3. No se encontró
            Logger.warning("No se encontró imagen: " + path);
            imageCache.put(path, null); // Guarda null para no intentarlo de nuevo
            return null;

        } catch (Exception e) {
            Logger.warning("Error al cargar imagen '" + path + "': " + e.getMessage());
            imageCache.put(path, null);
            return null;
        } finally {
            try { if (is != null) is.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Dibuja la barra de vida de un componente.
     */
    private void drawHealthBar(int x, int y, double lifePercentage) {
        int barWidth = CELL_SIZE - 4;
        int barHeight = 3;
        int barX = x + 2;
        int barY = y + 1; // Encima del componente

        gc.setFill(Color.RED); // Fondo de la barra
        gc.fillRect(barX, barY, barWidth, barHeight);

        gc.setFill(Color.LIME); // Vida restante
        gc.fillRect(barX, barY, barWidth * lifePercentage, barHeight);
    }

    /**
     * Dibuja la vista previa de colocación (cuadrado verde/rojo) y el rango.
     */
    private void drawPlacementPreview(Position pos) {
        if (!game.getBoard().isValidPosition(pos)) return;

        int x = pos.getX() * CELL_SIZE;
        int y = pos.getY() * CELL_SIZE;

        // Comprueba si se puede colocar (si no hay defensa terrestre)
        boolean canPlace = !game.getBoard().getCell(pos.getX(), pos.getY()).hasGroundOccupant();

        // Verde si se puede, Rojo si no
        Color previewColor = canPlace ? Color.rgb(0, 255, 0, 0.3) : Color.rgb(255, 0, 0, 0.3);
        gc.setFill(previewColor);
        gc.fillRect(x, y, CELL_SIZE, CELL_SIZE);

        // Dibuja el círculo de rango
        if (canPlace && selectedDefense.getRange() > 0) {
            drawRange(pos, selectedDefense.getRange(), Color.rgb(0, 255, 255, 0.2));
        }
    }

    /**
     * Dibuja un círculo de rango.
     */
    private void drawRange(Position center, int range, Color color) {
        gc.setStroke(color);
        gc.setLineWidth(2);
        int centerX = center.getX() * CELL_SIZE + CELL_SIZE / 2;
        int centerY = center.getY() * CELL_SIZE + CELL_SIZE / 2;
        int radius = range * CELL_SIZE; // Rango en píxeles

        gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
    }

    /**
     * Actualiza los labels de la barra superior (se llama en el AnimationTimer).
     */
    private void updateInfoBar() {
        // Usa Platform.runLater para asegurar que se actualice en el hilo de UI
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

    /**
     * Muestra el diálogo para guardar la partida.
     */
    private void saveGame() {
        game.pause(); // Pausa el juego antes de abrir el diálogo
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Partida");
        fileChooser.setInitialFileName("partida.json");
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
        game.resume(); // Reanuda el juego después de cerrar el diálogo
    }

    public Button getPauseResumeBtn() {
        return pauseResumeBtn;
    }

    /**
     * Regresa al menú principal, limpiando la partida actual.
     */
    private void returnToMenu() {
        cleanup();
        gameUI.returnToMainMenu();
    }

    /**
     * Detiene el bucle de renderizado, el motor del juego y limpia el caché.
     */
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