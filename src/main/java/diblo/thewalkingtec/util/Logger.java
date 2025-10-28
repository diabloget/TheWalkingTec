package diblo.thewalkingtec.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Sistema de logging simple para el juego
 */
public class Logger {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String LOG_FILE = "game.log";
    private static PrintWriter logWriter;
    private static boolean enableFileLogging = false;

    public enum Level {
        DEBUG, INFO, WARNING, ERROR
    }

    static {
        try {
            logWriter = new PrintWriter(new FileWriter(LOG_FILE, true), true);
            enableFileLogging = true;
        } catch (IOException e) {
            System.err.println("No se pudo inicializar el archivo de log: " + e.getMessage());
        }
    }

    /**
     * Registra un mensaje con nivel específico
     */
    public static void log(Level level, String message) {
        String timestamp = DATE_FORMAT.format(new Date());
        String logMessage = String.format("[%s] [%s] %s", timestamp, level, message);

        // Imprimir en consola
        System.out.println(logMessage);

        // Guardar en archivo
        if (enableFileLogging && logWriter != null) {
            logWriter.println(logMessage);
        }
    }

    /**
     * Log nivel DEBUG
     */
    public static void debug(String message) {
        log(Level.DEBUG, message);
    }

    /**
     * Log nivel INFO
     */
    public static void info(String message) {
        log(Level.INFO, message);
    }

    /**
     * Log nivel WARNING
     */
    public static void warning(String message) {
        log(Level.WARNING, message);
    }

    /**
     * Log nivel ERROR
     */
    public static void error(String message) {
        log(Level.ERROR, message);
    }

    /**
     * Log de excepciones
     */
    public static void error(String message, Exception e) {
        log(Level.ERROR, message + " - " + e.getMessage());
        if (enableFileLogging && logWriter != null) {
            e.printStackTrace(logWriter);
        }
    }

    /**
     * Cierra el logger
     */
    public static void close() {
        if (logWriter != null) {
            logWriter.close();
        }
    }
}