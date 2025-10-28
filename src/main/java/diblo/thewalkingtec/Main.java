package diblo.thewalkingtec;

import diblo.thewalkingtec.service.ConfigurationManager;
import diblo.thewalkingtec.ui.GameUI;
import diblo.thewalkingtec.util.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Clase principal del juego The Walking TEC
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            Logger.info("=== Iniciando The Walking TEC ===");

            String configPath = handleConfiguration(primaryStage);
            if (configPath == null) {
                Logger.error("No se pudo cargar la configuración. Cerrando la aplicación.");
                Platform.exit();
                return;
            }

            GameUI mainUI = new GameUI();
            mainUI.start(primaryStage);

            Logger.info("Aplicación iniciada correctamente");

        } catch (Exception e) {
            Logger.error("Error fatal al iniciar la aplicación", e);
            e.printStackTrace();
            Platform.exit();
        }
    }

    private static final String DEFAULT_CONFIG_PATH = "config.json";

    private String handleConfiguration(Stage primaryStage) {
        File defaultConfig = new File(DEFAULT_CONFIG_PATH);

        // 1. Asegurarse que el config por defecto exista
        if (!defaultConfig.exists()) {
            try {
                ConfigurationManager.createDefaultConfig(DEFAULT_CONFIG_PATH);
                Logger.info("Archivo de configuración por defecto creado en: " + DEFAULT_CONFIG_PATH);
            } catch (IOException e) {
                Logger.error("Error fatal: No se pudo crear el config por defecto.", e);
                showAlert(primaryStage, Alert.AlertType.ERROR, "Error Crítico", "No se pudo crear 'config.json'. El juego no puede iniciar.");
                return null; // Fallo crítico
            }
        }

        // 2. Preguntar al usuario qué desea hacer
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Configuración del Juego");
        alert.setHeaderText("¿Cómo deseas iniciar el juego?");
        alert.setContentText("Puedes cargar una configuración personalizada (JSON) o usar la de por defecto.");

        ButtonType loadCustomBtn = new ButtonType("Cargar Personalizada", ButtonBar.ButtonData.OK_DONE);
        ButtonType useDefaultBtn = new ButtonType("Usar por Defecto", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(loadCustomBtn, useDefaultBtn);

        Optional<ButtonType> result = alert.showAndWait();

        // 3. Manejar la elección del usuario
        if (result.isPresent() && result.get() == loadCustomBtn) {
            // 3A. El usuario quiere cargar un archivo personalizado
            FileChooser openFileChooser = new FileChooser();
            openFileChooser.setTitle("Seleccionar archivo de configuración");
            openFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            File configFile = openFileChooser.showOpenDialog(primaryStage);

            if (configFile != null) {
                try {
                    ConfigurationManager.loadConfig(configFile.getAbsolutePath());
                    Logger.info("Configuración personalizada cargada: " + configFile.getAbsolutePath());
                    return configFile.getAbsolutePath(); // Éxito (Personalizada)
                } catch (IOException e) {
                    Logger.error("No se pudo cargar el config personalizado. Usando por defecto.", e);
                    showAlert(primaryStage, Alert.AlertType.WARNING, "Error de Carga", "No se pudo leer el archivo seleccionado. Se cargará 'config.json' por defecto.");
                    // Si falla, se carga el default (ver paso 4)
                }
            } else {
                // El usuario canceló el FileChooser, usar por defecto
                Logger.info("Carga personalizada cancelada. Usando por defecto.");
            }
        }

        // 4. Cargar por defecto (si eligió "Usar por Defecto" o si falló la carga personalizada)
        try {
            ConfigurationManager.loadConfig(DEFAULT_CONFIG_PATH);
            Logger.info("Configuración por defecto cargada.");
            return DEFAULT_CONFIG_PATH; // Éxito (Defecto)
        } catch (IOException e) {
            Logger.error("Error fatal: No se pudo cargar el config por defecto 'config.json'.", e);
            showAlert(primaryStage, Alert.AlertType.ERROR, "Error Crítico", "No se pudo cargar 'config.json'. El juego no puede iniciar.");
            return null; // Fallo crítico
        }
    }

    // Agrega este método helper a tu clase Main.java para mostrar alertas
    private void showAlert(Stage owner, Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        Logger.info("=== Cerrando The Walking TEC ===");
        Logger.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}