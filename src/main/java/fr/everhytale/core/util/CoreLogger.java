package fr.everhytale.core.util;

import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Wrapper around HytaleLogger with configurable debug mode.
 * Provides convenient logging methods with printf-style formatting.
 */
public class CoreLogger {
    
    private final HytaleLogger logger;
    private boolean debugEnabled = false;
    
    /**
     * Creates a new CoreLogger for the specified class.
     *
     * @param clazz The class to create the logger for
     */
    public CoreLogger(Class<?> clazz) {
        this.logger = HytaleLogger.get(clazz.getSimpleName());
    }
    
    /**
     * Creates a new CoreLogger with the specified name.
     *
     * @param name The name for the logger
     */
    public CoreLogger(String name) {
        this.logger = HytaleLogger.get(name);
    }
    
    /**
     * Enables or disables debug logging.
     *
     * @param enabled true to enable debug logging
     */
    public void setDebugEnabled(boolean enabled) {
        this.debugEnabled = enabled;
    }
    
    /**
     * Returns whether debug logging is enabled.
     *
     * @return true if debug logging is enabled
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }
    
    /**
     * Logs an info message.
     *
     * @param message The message format string
     * @param args The format arguments
     */
    public void info(String message, Object... args) {
        logger.atInfo().log(message, args);
    }
    
    /**
     * Logs a warning message.
     *
     * @param message The message format string
     * @param args The format arguments
     */
    public void warn(String message, Object... args) {
        logger.atWarning().log(message, args);
    }
    
    /**
     * Logs an error message.
     *
     * @param message The message format string
     * @param args The format arguments
     */
    public void error(String message, Object... args) {
        logger.atSevere().log(message, args);
    }
    
    /**
     * Logs an error message with an exception.
     *
     * @param message The message format string
     * @param cause The exception that caused the error
     * @param args The format arguments
     */
    public void error(String message, Throwable cause, Object... args) {
        logger.atSevere().withCause(cause).log(message, args);
    }
    
    /**
     * Logs a debug message (only if debug mode is enabled).
     *
     * @param message The message format string
     * @param args The format arguments
     */
    public void debug(String message, Object... args) {
        if (debugEnabled) {
            logger.atInfo().log("[DEBUG] " + message, args);
        }
    }
    
    /**
     * Gets the underlying HytaleLogger instance.
     *
     * @return The HytaleLogger instance
     */
    public HytaleLogger getLogger() {
        return logger;
    }
}
