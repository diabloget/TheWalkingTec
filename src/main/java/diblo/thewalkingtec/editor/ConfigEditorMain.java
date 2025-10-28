package diblo.thewalkingtec.editor;

// --- Imports de tu proyecto ---
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import diblo.thewalkingtec.model.config.*;
import diblo.thewalkingtec.model.enums.AIType;
import diblo.thewalkingtec.model.enums.ComponentType;

// --- Imports de JavaFX (Completos) ---
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

// --- Imports de Java IO/NIO y Util ---
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aplicación de editor de configuración para The Walking TEC.
 * Esta aplicación permite a un administrador (con login) cargar, modificar
 * y guardar el archivo 'config.json' que usa el juego principal.
 *
 * Cumple con el requisito de "Programa configuración de personajes".
 */
public class ConfigEditorMain extends Application {

    private Stage primaryStage; // La ventana principal de la aplicación
    private GameConfig gameConfig; // El objeto Java que mapea el config.json
    private File currentConfigFile; // El archivo .json que está abierto actualmente
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create(); // Objeto para (de)serializar JSON

    // Listas observables para vincular con la UI de JavaFX
    private ObservableList<DefenseConfig> defenseList = FXCollections.observableArrayList();
    private ObservableList<EnemyConfig> enemyList = FXCollections.observableArrayList();
    private ObservableList<LevelConfig> levelList = FXCollections.observableArrayList();

    // Referencias a los formularios de edición de cada pestaña
    private DefenseEditorForm defenseForm;
    private EnemyEditorForm enemyForm;
    private LevelEditorForm levelForm;

    // Caché simple para no recargar imágenes del disco repetidamente
    private final Map<String, Image> imageCache = new HashMap<>();

    /**
     * Punto de entrada principal de la aplicación JavaFX.
     * @param stage El escenario principal proporcionado por JavaFX.
     */
    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        primaryStage.setTitle("Editor de Configuración - Login");
        // Inicia mostrando la pantalla de login en lugar del editor
        showLoginScreen();
    }

    /**
     * Muestra una ventana de login modal.
     * El editor principal (showMainEditor) solo se llamará si el login es exitoso.
     */
    private void showLoginScreen() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25));
        grid.setStyle("-fx-background-color: #333;"); // Fondo oscuro

        Label title = new Label("Acceso de Administrador");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        grid.add(title, 0, 0, 2, 1);

        // Campo de Usuario
        Label userName = new Label("Usuario:");
        userName.setStyle("-fx-text-fill: white;");
        grid.add(userName, 0, 1);
        TextField userField = new TextField();
        userField.setPromptText("admin"); // Texto de guía
        grid.add(userField, 1, 1);

        // Campo de Contraseña
        Label pw = new Label("Contraseña:");
        pw.setStyle("-fx-text-fill: white;");
        grid.add(pw, 0, 2);
        PasswordField passField = new PasswordField();
        passField.setPromptText("password"); // Texto de guía
        grid.add(passField, 1, 2);

        Button loginButton = new Button("Ingresar");
        loginButton.setDefaultButton(true);
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(loginButton);
        grid.add(hbBtn, 1, 4);

        // Lógica de autenticación
        loginButton.setOnAction(e -> {
            if (userField.getText().equals("admin") && passField.getText().equals("password")) {
                showMainEditor(); // Éxito, muestra el editor
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Usuario o contraseña incorrectos.");
            }
        });

        Scene scene = new Scene(grid, 350, 250);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Construye y muestra la interfaz principal del editor (pestañas, menús, etc.)
     * Este método solo se llama después de un login exitoso.
     */
    private void showMainEditor() {
        primaryStage.setTitle("Editor de Configuración - The Walking TEC");
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // 1. Crear la barra de menú superior (Archivo -> Guardar, Cargar...)
        MenuBar menuBar = createMenuBar();
        root.setTop(menuBar);

        // 2. Crear el panel de pestañas
        TabPane tabPane = new TabPane();
        defenseForm = new DefenseEditorForm();
        enemyForm = new EnemyEditorForm();
        levelForm = new LevelEditorForm();

        // --- Pestaña de Defensas ---
        ListView<DefenseConfig> defenseListView = new ListView<>(defenseList);
        defenseListView.setCellFactory(lv -> new DefenseListCell()); // Asigna la celda visual personalizada
        defenseListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldV, newV) -> showDefenseDetails(newV)); // Llama a showDefenseDetails al seleccionar
        Node defenseTabContent = createEditorPane(defenseListView, defenseForm.getFormPane(), this::createNewDefense);
        Tab defenseTab = new Tab("Defensas", defenseTabContent);
        defenseTab.setClosable(false);

        // --- Pestaña de Enemigos ---
        ListView<EnemyConfig> enemyListView = new ListView<>(enemyList);
        enemyListView.setCellFactory(lv -> new EnemyListCell());
        enemyListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldV, newV) -> showEnemyDetails(newV));
        Node enemyTabContent = createEditorPane(enemyListView, enemyForm.getFormPane(), this::createNewEnemy);
        Tab enemyTab = new Tab("Enemigos", enemyTabContent);
        enemyTab.setClosable(false);

        // --- Pestaña de Niveles ---
        ListView<LevelConfig> levelListView = new ListView<>(levelList);
        levelListView.setCellFactory(lv -> new LevelListCell());
        levelListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldV, newV) -> showLevelDetails(newV));
        Node levelTabContent = createEditorPane(levelListView, levelForm.getFormPane(), this::createNewLevel);
        Tab levelTab = new Tab("Niveles", levelTabContent);
        levelTab.setClosable(false);

        // Añadir todas las pestañas al panel
        tabPane.getTabs().addAll(defenseTab, enemyTab, levelTab);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1000, 700); // Tamaño más grande para el editor
        primaryStage.setScene(scene);

        // 3. Cargar el config.json por defecto (si existe)
        tryLoadDefaultConfig();
    }

    // --- Lógica de Carga/Guardado de Archivos ---

    /**
     * Crea la barra de menú "Archivo" con opciones para Cargar, Guardar, y Salir.
     * @return Un nodo MenuBar listo para ser añadido a la UI.
     */
    private MenuBar createMenuBar() {
        Menu fileMenu = new Menu("Archivo");
        MenuItem loadItem = new MenuItem("Cargar config.json...");
        loadItem.setOnAction(e -> loadConfig());
        MenuItem saveItem = new MenuItem("Guardar");
        saveItem.setOnAction(e -> saveConfig());
        MenuItem saveAsItem = new MenuItem("Guardar Como...");
        saveAsItem.setOnAction(e -> saveConfigAs());
        MenuItem exitItem = new MenuItem("Salir");
        exitItem.setOnAction(e -> primaryStage.close());

        fileMenu.getItems().addAll(loadItem, saveItem, saveAsItem, new SeparatorMenuItem(), exitItem);
        return new MenuBar(fileMenu);
    }

    /**
     * Intenta cargar 'config.json' desde la raíz del proyecto al iniciar.
     * Si no lo encuentra, inicia con un GameConfig vacío.
     */
    private void tryLoadDefaultConfig() {
        File defaultConfig = new File("config.json");
        if (defaultConfig.exists()) {
            loadConfigFromFile(defaultConfig);
        } else {
            // Si no existe config.json, crea un objeto en memoria para empezar a editar
            gameConfig = new GameConfig();
            gameConfig.setDefenses(new java.util.ArrayList<>());
            gameConfig.setEnemies(new java.util.ArrayList<>());
            gameConfig.setLevels(new java.util.ArrayList<>());
            showAlert(Alert.AlertType.INFORMATION, "Bienvenido", "No se encontró 'config.json'. Se inició un editor vacío.");
        }
    }

    /**
     * Muestra un diálogo FileChooser para que el usuario seleccione un archivo .json para cargar.
     */
    private void loadConfig() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar config.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = chooser.showOpenDialog(primaryStage);
        if (file != null) {
            loadConfigFromFile(file);
        }
    }

    /**
     * Carga el contenido de un archivo .json (dado) y puebla las listas observables (defenseList, etc.).
     * @param file El archivo .json a cargar.
     */
    private void loadConfigFromFile(File file) {
        try (FileReader reader = new FileReader(file)) {
            // Usa Gson para convertir el texto JSON en el objeto GameConfig
            gameConfig = GSON.fromJson(reader, GameConfig.class);
            if (gameConfig == null) gameConfig = new GameConfig(); // Evitar NullPointerException si el archivo está vacío

            // Puebla las listas de la UI con los datos cargados
            defenseList.setAll(gameConfig.getDefenses() != null ? gameConfig.getDefenses() : new java.util.ArrayList<>());
            enemyList.setAll(gameConfig.getEnemies() != null ? gameConfig.getEnemies() : new java.util.ArrayList<>());
            levelList.setAll(gameConfig.getLevels() != null ? gameConfig.getLevels() : new java.util.ArrayList<>());

            currentConfigFile = file; // Guarda la referencia al archivo actual
            primaryStage.setTitle("Editor - " + file.getName());
            showAlert(Alert.AlertType.INFORMATION, "Éxito", "Configuración cargada desde " + file.getName());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "No se pudo leer el archivo de configuración: " + e.getMessage());
        }
    }

    /**
     * Guarda el estado actual del editor en el archivo 'currentConfigFile'.
     * Si el archivo no existe (ej. es un config nuevo), llama a saveConfigAs().
     */
    private void saveConfig() {
        if (currentConfigFile == null) {
            saveConfigAs(); // Forzar "Guardar Como" si no hay archivo abierto
        } else {
            saveConfigToFile(currentConfigFile);
        }
    }

    /**
     * Muestra un diálogo FileChooser para que el usuario elija dónde guardar el archivo .json.
     */
    private void saveConfigAs() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar config.json");
        chooser.setInitialFileName("config.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = chooser.showSaveDialog(primaryStage);
        if (file != null) {
            saveConfigToFile(file);
        }
    }

    /**
     * Escribe el estado actual de las listas (defenseList, etc.) al 'gameConfig'
     * y luego serializa 'gameConfig' a un archivo .json en disco.
     * @param file El archivo destino donde se guardará el JSON.
     */
    private void saveConfigToFile(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            // Sincroniza las listas de la UI con el objeto GameConfig
            gameConfig.setDefenses(new java.util.ArrayList<>(defenseList));
            gameConfig.setEnemies(new java.util.ArrayList<>(enemyList));
            gameConfig.setLevels(new java.util.ArrayList<>(levelList));

            // Usa Gson para convertir el objeto GameConfig a texto JSON
            GSON.toJson(gameConfig, writer);

            currentConfigFile = file; // Actualiza la referencia
            primaryStage.setTitle("Editor - " + file.getName());
            showAlert(Alert.AlertType.INFORMATION, "Éxito", "Configuración guardada en " + file.getName());
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "No se pudo guardar el archivo: " + e.getMessage());
        }
    }

    /**
     * Método genérico para crear el layout de una pestaña (Lista a la izq, Formulario a la der).
     * @param <T> El tipo de dato (DefenseConfig, EnemyConfig, etc.)
     * @param listView La ListView para mostrar a la izquierda.
     * @param formPane El Pane (GridPane) del formulario para mostrar a la derecha.
     * @param newHandler La acción a ejecutar (lambda) cuando se presiona el botón "+".
     * @return Un nodo (SplitPane) que contiene el layout completo de la pestaña.
     */
    private <T> Node createEditorPane(ListView<T> listView, Pane formPane, Runnable newHandler) {
        // Botón para añadir nuevo item
        Button newBtn = new Button("+");
        newBtn.setOnAction(e -> newHandler.run());

        // Botón para borrar item seleccionado
        Button delBtn = new Button("-");
        delBtn.setOnAction(e -> {
            T selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // Pedir confirmación antes de borrar
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Estás seguro de que quieres borrar '" + selected.toString() + "'?", ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        listView.getItems().remove(selected); // Borra de la lista observable
                    }
                });
            }
        });
        HBox listButtons = new HBox(10, newBtn, delBtn);
        listButtons.setPadding(new Insets(5));
        listButtons.setAlignment(Pos.CENTER_RIGHT);

        // Panel izquierdo (lista + botones)
        VBox listPane = new VBox(listView, listButtons);
        VBox.setVgrow(listView, Priority.ALWAYS); // La lista ocupa todo el espacio vertical

        // Panel derecho (formulario dentro de un ScrollPane)
        ScrollPane formScroll = new ScrollPane(formPane);
        formScroll.setFitToWidth(true);
        formScroll.setPadding(new Insets(10));

        // Contenedor principal
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(listPane, formScroll);
        splitPane.setDividerPositions(0.35); // La lista ocupa el 35% del ancho
        return splitPane;
    }

    // --- Lógica de Selección (Handlers para los listeners de las listas) ---

    // Muestra la defensa seleccionada en el formulario de defensas
    private void showDefenseDetails(DefenseConfig config) { if (config != null) defenseForm.setConfig(config); }
    // Crea una nueva defensa con valores por defecto y la añade a la lista
    private void createNewDefense() { DefenseConfig n = new DefenseConfig(); n.setId("nueva_defensa"); n.setName("Nueva Defensa"); n.setType(ComponentType.BLOCK.name()); n.setFields(1); defenseList.add(n); }

    // Muestra el enemigo seleccionado en el formulario de enemigos
    private void showEnemyDetails(EnemyConfig config) { if (config != null) enemyForm.setConfig(config); }
    // Crea un nuevo enemigo con valores por defecto y lo añade a la lista
    private void createNewEnemy() { EnemyConfig n = new EnemyConfig(); n.setId("nuevo_zombie"); n.setName("Nuevo Zombie"); n.setType("CONTACT"); n.setAiType(AIType.SEEK_NEAREST.name()); n.setFields(1); enemyList.add(n); }

    // Muestra el nivel seleccionado en el formulario de niveles
    private void showLevelDetails(LevelConfig config) {
        if (config != null) {
            levelForm.setConfig(config);
        }
    }
    // Crea un nuevo nivel con valores por defecto y lo añade a la lista
    private void createNewLevel() {
        LevelConfig newLevel = new LevelConfig();
        newLevel.setLevelNumber(levelList.size() + 1); // Autoincrementa el número
        newLevel.setPlayerArmySize(20);
        newLevel.setStartingMoney(500);
        newLevel.setEnemyWaves(new java.util.ArrayList<>());
        levelList.add(newLevel);
    }

    // --- Clases Internas: Celdas de Lista Personalizadas ---

    /**
     * Define cómo se ve cada fila en la ListView de Defensas.
     * Muestra una imagen, el nombre y el ID.
     */
    private class DefenseListCell extends ListCell<DefenseConfig> {
        private final HBox root;
        private final ImageView imageView;
        private final Label nameLabel, idLabel;
        public DefenseListCell(){super(); imageView=new ImageView();imageView.setFitWidth(40);imageView.setFitHeight(40);imageView.setPreserveRatio(true); nameLabel=new Label();nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");idLabel=new Label();idLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");VBox infoBox=new VBox(nameLabel,idLabel);infoBox.setSpacing(2);root=new HBox(10,imageView,infoBox);root.setAlignment(Pos.CENTER_LEFT);}
        @Override protected void updateItem(DefenseConfig config,boolean empty){super.updateItem(config,empty);if(empty||config==null){setText(null);setGraphic(null);}else{imageView.setImage(loadImage(config.getImagePath()));nameLabel.setText(config.getName());idLabel.setText(config.getId());setGraphic(root);}}
    }

    /**
     * Define cómo se ve cada fila en la ListView de Enemigos.
     * Muestra una imagen, el nombre y el ID.
     */
    private class EnemyListCell extends ListCell<EnemyConfig> {
        private final HBox root;
        private final ImageView imageView;
        private final Label nameLabel, idLabel;
        public EnemyListCell(){super(); imageView=new ImageView();imageView.setFitWidth(40);imageView.setFitHeight(40);imageView.setPreserveRatio(true); nameLabel=new Label();nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");idLabel=new Label();idLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");VBox infoBox=new VBox(nameLabel,idLabel);infoBox.setSpacing(2);root=new HBox(10,imageView,infoBox);root.setAlignment(Pos.CENTER_LEFT);}
        @Override protected void updateItem(EnemyConfig config,boolean empty){super.updateItem(config,empty);if(empty||config==null){setText(null);setGraphic(null);}else{imageView.setImage(loadImage(config.getImagePath()));nameLabel.setText(config.getName());idLabel.setText(config.getId());setGraphic(root);}}
    }

    /**
     * Define cómo se ve cada fila en la ListView de Niveles.
     * Muestra el número de nivel, dinero y tamaño de ejército.
     */
    private class LevelListCell extends ListCell<LevelConfig> {
        private final Label nameLabel;
        public LevelListCell() {
            super();
            nameLabel = new Label();
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        }
        @Override
        protected void updateItem(LevelConfig config, boolean empty) {
            super.updateItem(config, empty);
            if (empty || config == null) {
                setText(null); setGraphic(null);
            } else {
                nameLabel.setText(String.format("Nivel %d (Dinero: %d, Ejército: %d)",
                        config.getLevelNumber(), config.getStartingMoney(), config.getPlayerArmySize()));
                setGraphic(nameLabel);
            }
        }
    }

    // --- Clases Internas: Formularios de Edición ---

    /**
     * Clase interna que maneja el formulario de edición para Defensas.
     */
    private class DefenseEditorForm {
        private GridPane p; // 'p' por pane
        private DefenseConfig c; // 'c' por currentConfig
        private ImageView pv; // 'pv' por preview
        private TextField id,n,h,d,r,co,f,l,i; // Campos de texto
        private ComboBox<ComponentType> t; // 't' por typeBox

        public DefenseEditorForm(){
            p=cFP(); // cFP = createFormPane
            // Filtra el enum ComponentType para mostrar solo los que son defensas
            List<ComponentType> dt=Arrays.stream(ComponentType.values()).filter(ComponentType::isDefense).collect(Collectors.toList());
            t.setItems(FXCollections.observableArrayList(dt));
        }
        public Pane getFormPane(){return p;}

        // Puebla el formulario con los datos de la defensa seleccionada
        public void setConfig(DefenseConfig config){
            this.c=config;id.setText(c.getId());n.setText(c.getName());
            h.setText(String.valueOf(c.getBaseHealth()));d.setText(String.valueOf(c.getBaseDamage()));
            r.setText(String.valueOf(c.getRange()));co.setText(String.valueOf(c.getCost()));
            f.setText(String.valueOf(c.getFields()));l.setText(String.valueOf(c.getUnlockLevel()));
            i.setText(c.getImagePath());pv.setImage(loadImage(c.getImagePath()));
            try{t.setValue(ComponentType.valueOf(c.getType().toUpperCase()));}catch(Exception e){t.setValue(null);}
        }

        // Construye el GridPane del formulario
        private GridPane cFP(){
            GridPane g=new GridPane();g.setVgap(10);g.setHgap(10);
            pv=new ImageView();pv.setFitHeight(100);pv.setFitWidth(100);pv.setPreserveRatio(true);pv.setStyle("-fx-border-color: gray; -fx-border-width: 1px;");GridPane.setColumnSpan(pv,2);GridPane.setHalignment(pv,HPos.CENTER);g.add(pv,0,0);
            int row=1;
            id=addRow(g,"ID:",row++);n=addRow(g,"Nombre:",row++);
            t=new ComboBox<>();g.add(new Label("Tipo:"),0,row);g.add(t,1,row++);
            h=addRow(g,"Vida Base:",row++);d=addRow(g,"Daño Base:",row++);r=addRow(g,"Rango:",row++);
            co=addRow(g,"Costo (Monedas):",row++);f=addRow(g,"Espacios (Campos):",row++);l=addRow(g,"Nivel de Desbloqueo:",row++);
            g.add(new Label("Ruta de Imagen:"),0,row);i=new TextField();Button bB=new Button("Buscar...");bB.setOnAction(e->browseForImage(i,pv));HBox iB=new HBox(5,i,bB);HBox.setHgrow(i,Priority.ALWAYS);g.add(iB,1,row++);
            Button sB=new Button("Guardar Cambios");sB.setOnAction(e->sCC());g.add(sB,1,row++); // sCC = saveCurrentConfig
            return g;
        }

        // Valida que todos los campos estén llenos
        private boolean vF(){ // vF = validateFields
            StringBuilder s=new StringBuilder();
            if(id.getText().isBlank())s.append("• ID\n");
            if(n.getText().isBlank())s.append("• Nombre\n");
            if(h.getText().isBlank())s.append("• Vida Base\n");
            if(d.getText().isBlank())s.append("• Daño Base\n");
            if(r.getText().isBlank())s.append("• Rango\n");
            if(co.getText().isBlank())s.append("• Costo\n");
            if(f.getText().isBlank())s.append("• Espacios\n");
            if(l.getText().isBlank())s.append("• Nivel\n");
            if(i.getText().isBlank())s.append("• Ruta de Imagen\n");
            if(t.getValue()==null)s.append("• Tipo\n");
            if(s.isEmpty()){return true;}else{showAlert(Alert.AlertType.WARNING,"Campos Incompletos",s.toString());return false;}
        }

        // Guarda los datos del formulario de vuelta al objeto DefenseConfig
        private void sCC(){
            if(c==null)return;if(!vF())return;
            try{
                c.setId(id.getText());c.setName(n.getText());
                c.setBaseHealth(Integer.parseInt(h.getText()));c.setBaseDamage(Integer.parseInt(d.getText()));
                c.setRange(Integer.parseInt(r.getText()));c.setCost(Integer.parseInt(co.getText()));
                c.setFields(Integer.parseInt(f.getText()));c.setUnlockLevel(Integer.parseInt(l.getText()));
                c.setImagePath(i.getText());c.setType(t.getValue().name());
                int ix=defenseList.indexOf(c);if(ix!=-1)defenseList.set(ix,c); // Refresca la lista
                showAlert(Alert.AlertType.INFORMATION,"Guardado","Cambios aplicados a "+c.getName());
            }catch(Exception e){showAlert(Alert.AlertType.ERROR,"Error","Error al guardar: "+e.getMessage());}
        }
    }

    /**
     * Clase interna que maneja el formulario de edición para Enemigos.
     */
    private class EnemyEditorForm {
        private GridPane p; private EnemyConfig c; private ImageView pv; private TextField id,n,h,d,s,co,f,l,i; private ComboBox<String> t; private ComboBox<AIType> aT;

        public EnemyEditorForm(){
            p=cFP();
            t.setItems(FXCollections.observableArrayList("CONTACT","AERIAL","RANGED")); // Tipos de enemigo
            aT.setItems(FXCollections.observableArrayList(AIType.values())); // Tipos de IA
        }
        public Pane getFormPane(){return p;}

        // Puebla el formulario con los datos del enemigo seleccionado
        public void setConfig(EnemyConfig config){
            this.c=config;id.setText(c.getId());n.setText(c.getName());
            h.setText(String.valueOf(c.getBaseHealth()));d.setText(String.valueOf(c.getBaseDamage()));
            s.setText(String.valueOf(c.getSpeed()));co.setText(String.valueOf(c.getCost()));
            f.setText(String.valueOf(c.getFields()));l.setText(String.valueOf(c.getUnlockLevel()));
            i.setText(c.getImagePath());pv.setImage(loadImage(c.getImagePath()));
            t.setValue(c.getType().toUpperCase());
            try{aT.setValue(AIType.valueOf(c.getAiType().toUpperCase()));}catch(Exception e){aT.setValue(null);}
        }

        // Construye el GridPane del formulario
        private GridPane cFP(){
            GridPane g=new GridPane();g.setVgap(10);g.setHgap(10);
            pv=new ImageView();pv.setFitHeight(100);pv.setFitWidth(100);pv.setPreserveRatio(true);pv.setStyle("-fx-border-color: gray; -fx-border-width: 1px;");GridPane.setColumnSpan(pv,2);GridPane.setHalignment(pv,HPos.CENTER);g.add(pv,0,0);
            int row=1;
            id=addRow(g,"ID:",row++);n=addRow(g,"Nombre:",row++);
            g.add(new Label("Tipo Componente:"),0,row);t=new ComboBox<>();g.add(t,1,row++);
            g.add(new Label("Tipo IA:"),0,row);aT=new ComboBox<>();g.add(aT,1,row++);
            h=addRow(g,"Vida Base:",row++);d=addRow(g,"Daño Base:",row++);s=addRow(g,"Velocidad:",row++);
            co=addRow(g,"Costo (Monedas):",row++);f=addRow(g,"Espacios (Campos):",row++);l=addRow(g,"Nivel de Desbloqueo:",row++);
            g.add(new Label("Ruta de Imagen:"),0,row);i=new TextField();Button bB=new Button("Buscar...");bB.setOnAction(e->browseForImage(i,pv));HBox iB=new HBox(5,i,bB);HBox.setHgrow(i,Priority.ALWAYS);g.add(iB,1,row++);
            Button sB=new Button("Guardar Cambios");sB.setOnAction(e->sCC());g.add(sB,1,row++);
            return g;
        }

        // Valida que todos los campos estén llenos
        private boolean vF(){
            StringBuilder b=new StringBuilder();
            if(id.getText().isBlank())b.append("• ID\n");
            if(n.getText().isBlank())b.append("• Nombre\n");
            if(h.getText().isBlank())b.append("• Vida Base\n");
            if(d.getText().isBlank())b.append("• Daño Base\n");
            if(s.getText().isBlank())b.append("• Velocidad\n");
            if(co.getText().isBlank())b.append("• Costo\n");
            if(f.getText().isBlank())b.append("• Espacios\n");
            if(l.getText().isBlank())b.append("• Nivel\n");
            if(i.getText().isBlank())b.append("• Ruta de Imagen\n");
            if(t.getValue()==null)b.append("• Tipo Componente\n");
            if(aT.getValue()==null)b.append("• Tipo IA\n");
            if(b.isEmpty()){return true;}else{showAlert(Alert.AlertType.WARNING,"Campos Incompletos",b.toString());return false;}
        }

        // Guarda los datos del formulario de vuelta al objeto EnemyConfig
        private void sCC(){
            if(c==null)return;if(!vF())return;
            try{
                c.setId(id.getText());c.setName(n.getText());
                c.setBaseHealth(Integer.parseInt(h.getText()));c.setBaseDamage(Integer.parseInt(d.getText()));
                c.setSpeed(Double.parseDouble(s.getText()));c.setCost(Integer.parseInt(co.getText()));
                c.setFields(Integer.parseInt(f.getText()));c.setUnlockLevel(Integer.parseInt(l.getText()));
                c.setImagePath(i.getText());c.setType(t.getValue().toUpperCase());c.setAiType(aT.getValue().name());
                int ix=enemyList.indexOf(c);if(ix!=-1)enemyList.set(ix,c); // Refresca la lista
                showAlert(Alert.AlertType.INFORMATION,"Guardado","Cambios aplicados a "+c.getName());
            }catch(Exception e){showAlert(Alert.AlertType.ERROR,"Error","Error al guardar: "+e.getMessage());}
        }
    }

    /**
     * Clase interna que maneja el formulario de edición para Niveles.
     * Contiene un formulario para las propiedades del nivel y un sub-editor (WaveEditor)
     * para manejar la lista de oleadas.
     */
    private class LevelEditorForm {
        private GridPane formPane; // Panel raíz (contiene el VBox)
        private LevelConfig currentConfig;

        // Campos del Nivel
        private TextField levelNumField, armySizeField, moneyField, defenseBoostField, enemyBoostField;
        // Lista observable para las oleadas del nivel SELECCIONADO
        private ObservableList<WaveConfig> waveList = FXCollections.observableArrayList();
        private WaveEditor waveEditor; // Sub-formulario

        public LevelEditorForm() {
            // Panel para los campos del nivel (N°, Dinero, etc.)
            GridPane levelDetailsPane = new GridPane();
            levelDetailsPane.setVgap(10);
            levelDetailsPane.setHgap(10);

            int row = 0;
            levelNumField = addRow(levelDetailsPane, "Número de Nivel:", row++);
            armySizeField = addRow(levelDetailsPane, "Tamaño Ejército:", row++);
            moneyField = addRow(levelDetailsPane, "Dinero Inicial:", row++);
            defenseBoostField = addRow(levelDetailsPane, "Boost Defensa (%):", row++);
            enemyBoostField = addRow(levelDetailsPane, "Boost Enemigo (%):", row++);

            Button saveButton = new Button("Guardar Cambios de Nivel");
            saveButton.setOnAction(e -> saveCurrentConfig());
            levelDetailsPane.add(saveButton, 1, row);

            // Sub-editor para las oleadas
            // Pasa la lista de enemigos (para el ComboBox) y la lista de oleadas (para la UI)
            waveEditor = new WaveEditor(enemyList, waveList);

            // Panel raíz que combina ambos
            VBox root = new VBox(20, levelDetailsPane, new Separator(), waveEditor.getPane());
            VBox.setVgrow(waveEditor.getPane(), Priority.ALWAYS);

            // Truco para que getFormPane() devuelva el VBox
            formPane = new GridPane();
            formPane.add(root, 0, 0);
        }

        /** @return El panel de formulario completo (GridPane) para esta pestaña. */
        public Pane getFormPane() { return formPane; }

        /**
         * Puebla el formulario de Nivel y el sub-editor de Oleadas con
         * los datos del nivel seleccionado.
         * @param config El LevelConfig seleccionado en la lista.
         */
        public void setConfig(LevelConfig config) {
            this.currentConfig = config;
            levelNumField.setText(String.valueOf(config.getLevelNumber()));
            armySizeField.setText(String.valueOf(config.getPlayerArmySize()));
            moneyField.setText(String.valueOf(config.getStartingMoney()));
            defenseBoostField.setText(String.valueOf(config.getDefenseBoostPercent()));
            enemyBoostField.setText(String.valueOf(config.getEnemyBoostPercent()));

            // Puebla la lista de oleadas del sub-editor
            waveList.setAll(config.getEnemyWaves() != null ? config.getEnemyWaves() : new ArrayList<>());
            waveEditor.clearSelection(); // Limpiar formulario de oleada
        }

        // Valida que los campos principales del nivel estén llenos
        private boolean validateFields() {
            StringBuilder errors = new StringBuilder();
            if (levelNumField.getText().isBlank()) errors.append("• Número de Nivel\n");
            if (armySizeField.getText().isBlank()) errors.append("• Tamaño Ejército\n");
            if (moneyField.getText().isBlank()) errors.append("• Dinero Inicial\n");
            if (defenseBoostField.getText().isBlank()) errors.append("• Boost Defensa\n");
            if (enemyBoostField.getText().isBlank()) errors.append("• Boost Enemigo\n");

            if (errors.isEmpty()) return true;
            else {
                showAlert(Alert.AlertType.WARNING, "Campos Incompletos (Nivel)", errors.toString());
                return false;
            }
        }

        // Guarda los datos del formulario de Nivel de vuelta al objeto LevelConfig
        private void saveCurrentConfig() {
            if (currentConfig == null) return;
            if (!validateFields()) return;

            try {
                // Guarda los campos de texto
                currentConfig.setLevelNumber(Integer.parseInt(levelNumField.getText()));
                currentConfig.setPlayerArmySize(Integer.parseInt(armySizeField.getText()));
                currentConfig.setStartingMoney(Integer.parseInt(moneyField.getText()));
                currentConfig.setDefenseBoostPercent(Double.parseDouble(defenseBoostField.getText()));
                currentConfig.setEnemyBoostPercent(Double.parseDouble(enemyBoostField.getText()));

                // Guarda la lista de oleadas (que fue modificada por el WaveEditor)
                currentConfig.setEnemyWaves(new ArrayList<>(waveList));

                // Refresca la lista principal de niveles
                int index = levelList.indexOf(currentConfig);
                if (index != -1) levelList.set(index, currentConfig);

                showAlert(Alert.AlertType.INFORMATION, "Guardado", "Cambios aplicados a Nivel " + currentConfig.getLevelNumber());
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Valores numéricos inválidos para el Nivel.");
            }
        }

        /**
         * Sub-clase que maneja el editor de Oleadas (Waves) dentro del formulario de Nivel.
         * Muestra una lista de oleadas y un formulario para editar la oleada seleccionada.
         */
        private class WaveEditor {
            private VBox pane; // El panel UI de este sub-editor
            private WaveConfig currentWave; // La oleada seleccionada en la lista
            private ListView<WaveConfig> waveListView;
            private ComboBox<EnemyConfig> zombieIdBox; // ComboBox para elegir enemigo
            private TextField quantityField, delayField;

            /**
             * Construye el editor de oleadas.
             * @param availableEnemies La lista de todos los enemigos (para poblar el ComboBox).
             * @param waveList La lista observable de oleadas del nivel actual (para poblar la ListView).
             */
            public WaveEditor(ObservableList<EnemyConfig> availableEnemies, ObservableList<WaveConfig> waveList) {
                pane = new VBox(10);
                pane.setPadding(new Insets(10));
                pane.setStyle("-fx-border-color: gray; -fx-border-width: 1px; -fx-border-radius: 5;");

                Label title = new Label("Editor de Oleadas");
                title.setStyle("-fx-font-weight: bold;");

                waveListView = new ListView<>(waveList); // Se bindea a la lista del form
                waveListView.setPrefHeight(150);

                // Celda visual para la lista de oleadas
                waveListView.setCellFactory(lv -> new ListCell<>() {
                    @Override
                    protected void updateItem(WaveConfig item, boolean empty) {
                        super.updateItem(item, empty);
                        if(empty || item == null) setText(null);
                        else setText(String.format("ID: %s (Cant: %d, Delay: %ds)",
                                item.getZombieId(), item.getQuantity(), item.getDelaySeconds()));
                    }
                });

                // Botones +/- para oleadas
                Button newWaveBtn = new Button("+");
                newWaveBtn.setOnAction(e -> createNewWave());
                Button delWaveBtn = new Button("-");
                delWaveBtn.setOnAction(e -> {
                    WaveConfig selected = waveListView.getSelectionModel().getSelectedItem();
                    if(selected != null) waveList.remove(selected); // Borra de la lista observable
                });
                HBox waveListButtons = new HBox(5, newWaveBtn, delWaveBtn);
                waveListButtons.setAlignment(Pos.CENTER_RIGHT);

                // Formulario para editar la oleada seleccionada
                GridPane waveForm = new GridPane();
                waveForm.setHgap(10); waveForm.setVgap(5);

                zombieIdBox = new ComboBox<>(availableEnemies);
                // Convertidor para mostrar el ID y Nombre del enemigo en el ComboBox
                zombieIdBox.setConverter(new javafx.util.StringConverter<>() {
                    @Override public String toString(EnemyConfig o) { return o != null ? o.getId() + " (" + o.getName() + ")" : "N/A"; }
                    @Override public EnemyConfig fromString(String s) { return null; } // No se usa
                });

                quantityField = new TextField();
                delayField = new TextField();

                waveForm.add(new Label("Zombie ID:"), 0, 0); waveForm.add(zombieIdBox, 1, 0);
                waveForm.add(new Label("Cantidad:"), 0, 1); waveForm.add(quantityField, 1, 1);
                waveForm.add(new Label("Delay (seg):"), 0, 2); waveForm.add(delayField, 1, 2);

                Button saveWaveBtn = new Button("Guardar Oleada");
                saveWaveBtn.setOnAction(e -> saveCurrentWave());
                waveForm.add(saveWaveBtn, 1, 3);

                // Listener para poblar el formulario de oleada
                waveListView.getSelectionModel().selectedItemProperty().addListener(
                        (obs, o, newWave) -> {
                            this.currentWave = newWave; // Guarda la oleada seleccionada
                            if(newWave != null) {
                                // Encontrar el EnemyConfig (objeto completo) basado en el zombieId (String)
                                EnemyConfig enemy = availableEnemies.stream()
                                        .filter(ec -> ec.getId().equals(newWave.getZombieId()))
                                        .findFirst().orElse(null);

                                zombieIdBox.setValue(enemy);
                                quantityField.setText(String.valueOf(newWave.getQuantity()));
                                delayField.setText(String.valueOf(newWave.getDelaySeconds()));
                            } else {
                                clearSelection(); // Limpia el form si no hay nada seleccionado
                            }
                        });

                pane.getChildren().addAll(title, waveListView, waveListButtons, new Separator(), waveForm);
            }

            /** @return El panel UI de este sub-editor. */
            public Pane getPane() { return pane; }

            /** Limpia los campos del formulario de oleada. */
            public void clearSelection() {
                currentWave = null;
                zombieIdBox.setValue(null);
                quantityField.clear();
                delayField.clear();
            }

            // Crea una nueva oleada con valores por defecto
            private void createNewWave() {
                WaveConfig newWave = new WaveConfig();
                newWave.setZombieId("zombie_basic"); // Default
                newWave.setQuantity(10);
                newWave.setDelaySeconds(0);
                waveList.add(newWave); // Añade a la lista observable
            }

            // Guarda los cambios del formulario de oleada al objeto WaveConfig seleccionado
            private void saveCurrentWave() {
                if(currentWave == null) {
                    showAlert(Alert.AlertType.WARNING, "Error Oleada", "No hay ninguna oleada seleccionada para guardar.");
                    return;
                }
                try {
                    // Validaciones de campos
                    if(zombieIdBox.getValue() == null) {
                        showAlert(Alert.AlertType.ERROR, "Error Oleada", "Debe seleccionar un Zombie ID.");
                        return;
                    }
                    if(quantityField.getText().isBlank() || delayField.getText().isBlank()) {
                        showAlert(Alert.AlertType.ERROR, "Error Oleada", "Cantidad y Delay no pueden estar vacíos.");
                        return;
                    }

                    // Aplicar cambios al objeto
                    currentWave.setZombieId(zombieIdBox.getValue().getId());
                    currentWave.setQuantity(Integer.parseInt(quantityField.getText()));
                    currentWave.setDelaySeconds(Integer.parseInt(delayField.getText()));

                    // Refrescar la lista (importante para que se vea el cambio)
                    int index = waveList.indexOf(currentWave);
                    if (index != -1) waveList.set(index, currentWave);

                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Error Oleada", "Cantidad y Delay deben ser números.");
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error Oleada", "Valores inválidos. Asegúrate de seleccionar un Zombie ID.");
                }
            }
        }
    }


    // --- Métodos Helper (Utilidades) ---

    /**
     * Helper para añadir una fila (Label + TextField) a un GridPane.
     * @return La instancia del TextField creado.
     */
    private TextField addRow(GridPane grid, String label, int row) {
        grid.add(new Label(label), 0, row);
        TextField f = new TextField();
        grid.add(f, 1, row);
        return f;
    }

    /**
     * Muestra un FileChooser para buscar una imagen.
     * Intenta hacer la ruta relativa a 'src/main/resources' si es posible.
     * @param pathField El TextField donde se escribirá la ruta de la imagen.
     * @param preview El ImageView donde se mostrará la vista previa.
     */
    private void browseForImage(TextField pathField, ImageView preview) {
        FileChooser c = new FileChooser();
        c.setTitle("Seleccionar Imagen");
        c.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.gif"));
        File rDir = new File("src/main/resources/diblo/thewalkingtec/images");
        if(rDir.exists()) c.setInitialDirectory(rDir); // Inicia en la carpeta de imágenes del proyecto

        File f = c.showOpenDialog(primaryStage);
        if(f != null){
            String p;
            try {
                Path rP = Paths.get("src/main/resources").toAbsolutePath();
                Path iP = f.toPath().toAbsolutePath();
                // Si la imagen está dentro de resources, guarda la ruta relativa
                if(iP.startsWith(rP)){
                    p = rP.relativize(iP).toString().replace("\\", "/");
                } else {
                    // Si está fuera, guarda la ruta absoluta
                    p = f.getAbsolutePath();
                    showAlert(Alert.AlertType.WARNING, "Ruta no ideal", "Imagen fuera de 'src/main/resources'.");
                }
            } catch (Exception e){
                p = f.getAbsolutePath();
            }
            pathField.setText(p);
            preview.setImage(loadImage(p)); // Actualiza la vista previa
        }
    }

    /**
     * Carga una imagen, primero desde el ClassLoader (para defaults)
     * y luego como archivo local (para imágenes nuevas/buscadas).
     * Usa un caché para evitar recargar.
     * @param path La ruta relativa (desde resources) o absoluta (desde disco).
     * @return El objeto Image, o null si no se encuentra.
     */
    private Image loadImage(String path) {
        if(path==null||path.isEmpty())return null;
        if(imageCache.containsKey(path))return imageCache.get(path); // Usa caché

        Image i=null;
        try {
            // Intento 1: Cargar como recurso del classpath (lo normal para el juego)
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
            if(is != null){
                i=new Image(is);
                is.close();
            } else {
                // Intento 2: Cargar como archivo local (para 'Buscar...' en el editor)
                File f = new File(path);
                if(f.exists()){
                    i=new Image(f.toURI().toString());
                } else {
                    throw new IOException("Recurso no encontrado: "+path);
                }
            }
        } catch (Exception e) {
            System.err.println("No se pudo cargar imagen: "+e.getMessage());
        }
        imageCache.put(path, i); // Guarda en caché (incluso si es null)
        return i;
    }

    /**
     * Muestra una alerta simple de JavaFX.
     * @param type El tipo de alerta (ERROR, WARNING, INFORMATION).
     * @param title El título de la ventana.
     * @param message El mensaje a mostrar.
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    /**
     * Método main para lanzar la aplicación del editor.
     */
    public static void main(String[] args) {
        launch(args);
    }
}