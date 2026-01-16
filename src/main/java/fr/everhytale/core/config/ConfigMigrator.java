package fr.everhytale.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.everhytale.core.util.CoreLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Handles configuration loading with automatic migration and merging of default values.
 * 
 * <p>This class provides a robust configuration management system that:</p>
 * <ul>
 *   <li>Loads existing configurations from disk</li>
 *   <li>Merges missing fields from default configurations</li>
 *   <li>Applies migrations for version upgrades</li>
 *   <li>Creates backups before making changes</li>
 * </ul>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * ConfigMigrator migrator = new ConfigMigrator(logger);
 * 
 * // Register migrations
 * migrator.registerMigration(new MigrationV1ToV2());
 * migrator.registerMigration(new MigrationV2ToV3());
 * 
 * // Load config with automatic migration
 * MyConfig config = migrator.loadAndMigrate(
 *     configPath,
 *     MyConfig.class,
 *     "config.json",
 *     getClass().getClassLoader()
 * );
 * }</pre>
 */
public class ConfigMigrator {
    
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    
    private static final DateTimeFormatter BACKUP_FORMAT = 
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    private final CoreLogger logger;
    private final List<ConfigMigration> migrations = new ArrayList<>();
    
    /**
     * Creates a new ConfigMigrator.
     *
     * @param logger The logger to use for migration messages
     */
    public ConfigMigrator(CoreLogger logger) {
        this.logger = logger;
    }
    
    /**
     * Registers a migration to be applied when loading configurations.
     *
     * @param migration The migration to register
     * @return This migrator for chaining
     */
    public ConfigMigrator registerMigration(ConfigMigration migration) {
        migrations.add(migration);
        // Sort by fromVersion to ensure sequential application
        migrations.sort(Comparator.comparingInt(ConfigMigration::getFromVersion));
        return this;
    }
    
    /**
     * Loads a configuration, applying migrations and merging defaults as needed.
     * 
     * <p>This method will:</p>
     * <ol>
     *   <li>Check if the config file exists</li>
     *   <li>If not, copy the default from resources</li>
     *   <li>Load the existing config as JSON</li>
     *   <li>Merge any missing fields from the default config</li>
     *   <li>Apply any necessary migrations</li>
     *   <li>Save the updated config if changes were made</li>
     *   <li>Deserialize and return the config object</li>
     * </ol>
     *
     * @param <T> The configuration type
     * @param configPath The path to the configuration file
     * @param clazz The configuration class
     * @param resourcePath The path to the default config in resources
     * @param classLoader The class loader for loading resources
     * @return The loaded and migrated configuration
     * @throws IOException If an I/O error occurs
     */
    public <T extends VersionedConfig> T loadAndMigrate(
            Path configPath,
            Class<T> clazz,
            String resourcePath,
            ClassLoader classLoader) throws IOException {
        
        // Create parent directories if needed
        Path parent = configPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        
        // Load default config from resources
        JsonObject defaultConfig = loadDefaultConfig(resourcePath, classLoader);
        
        // If config file doesn't exist, create from default
        if (!Files.exists(configPath)) {
            logger.info("Creating default configuration: %s", configPath.getFileName());
            saveJson(configPath, defaultConfig);
            return GSON.fromJson(defaultConfig, clazz);
        }
        
        // Load existing config
        JsonObject existingConfig = loadJsonFromFile(configPath);
        
        // Check if migration or merge is needed
        int existingVersion = getVersion(existingConfig);
        int defaultVersion = getVersion(defaultConfig);
        
        boolean modified = false;
        
        // Merge missing fields from default
        if (mergeDefaults(existingConfig, defaultConfig)) {
            logger.info("Added new configuration fields from defaults");
            modified = true;
        }
        
        // Apply migrations if needed
        if (existingVersion < defaultVersion) {
            createBackup(configPath);
            applyMigrations(existingConfig, existingVersion, defaultVersion);
            modified = true;
        }
        
        // Save if modified
        if (modified) {
            saveJson(configPath, existingConfig);
            logger.info("Configuration updated and saved");
        }
        
        return GSON.fromJson(existingConfig, clazz);
    }
    
    /**
     * Loads a configuration with default merging but without versioned migrations.
     * Use this for simple configs that don't implement VersionedConfig.
     *
     * @param <T> The configuration type
     * @param configPath The path to the configuration file
     * @param clazz The configuration class
     * @param resourcePath The path to the default config in resources
     * @param classLoader The class loader for loading resources
     * @return The loaded configuration with defaults merged
     * @throws IOException If an I/O error occurs
     */
    public <T> T loadWithDefaults(
            Path configPath,
            Class<T> clazz,
            String resourcePath,
            ClassLoader classLoader) throws IOException {
        
        // Create parent directories if needed
        Path parent = configPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        
        // Load default config from resources
        JsonObject defaultConfig = loadDefaultConfig(resourcePath, classLoader);
        
        // If config file doesn't exist, create from default
        if (!Files.exists(configPath)) {
            logger.info("Creating default configuration: %s", configPath.getFileName());
            saveJson(configPath, defaultConfig);
            return GSON.fromJson(defaultConfig, clazz);
        }
        
        // Load existing config
        JsonObject existingConfig = loadJsonFromFile(configPath);
        
        // Merge missing fields from default
        if (mergeDefaults(existingConfig, defaultConfig)) {
            logger.info("Added new configuration fields from defaults");
            saveJson(configPath, existingConfig);
        }
        
        return GSON.fromJson(existingConfig, clazz);
    }
    
    /**
     * Loads the default configuration from resources.
     */
    private JsonObject loadDefaultConfig(String resourcePath, ClassLoader classLoader) throws IOException {
        try (InputStream is = classLoader.getResourceAsStream(resourcePath)) {
            if (is == null) {
                logger.warn("Default config resource not found: %s", resourcePath);
                return new JsonObject();
            }
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                JsonElement element = GSON.fromJson(reader, JsonElement.class);
                return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
            }
        }
    }
    
    /**
     * Loads JSON from a file.
     */
    private JsonObject loadJsonFromFile(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = GSON.fromJson(reader, JsonElement.class);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        }
    }
    
    /**
     * Saves JSON to a file.
     */
    private void saveJson(Path path, JsonObject json) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        }
    }
    
    /**
     * Gets the config version from a JSON object.
     */
    private int getVersion(JsonObject config) {
        if (config.has("configVersion")) {
            return config.get("configVersion").getAsInt();
        }
        // Default to version 1 if not specified
        return 1;
    }
    
    /**
     * Recursively merges missing fields from defaults into the existing config.
     *
     * @param existing The existing configuration
     * @param defaults The default configuration
     * @return true if any fields were added
     */
    private boolean mergeDefaults(JsonObject existing, JsonObject defaults) {
        boolean modified = false;
        
        for (Map.Entry<String, JsonElement> entry : defaults.entrySet()) {
            String key = entry.getKey();
            JsonElement defaultValue = entry.getValue();
            
            if (!existing.has(key)) {
                // Add missing field
                existing.add(key, deepCopy(defaultValue));
                logger.debug("Added missing config field: %s", key);
                modified = true;
            } else if (defaultValue.isJsonObject() && existing.get(key).isJsonObject()) {
                // Recursively merge nested objects
                if (mergeDefaults(existing.getAsJsonObject(key), defaultValue.getAsJsonObject())) {
                    modified = true;
                }
            }
            // Note: We don't overwrite existing values, only add missing ones
        }
        
        return modified;
    }
    
    /**
     * Creates a deep copy of a JSON element.
     */
    private JsonElement deepCopy(JsonElement element) {
        return GSON.fromJson(GSON.toJson(element), JsonElement.class);
    }
    
    /**
     * Creates a backup of the configuration file.
     */
    private void createBackup(Path configPath) throws IOException {
        if (!Files.exists(configPath)) {
            return;
        }
        
        String timestamp = LocalDateTime.now().format(BACKUP_FORMAT);
        String fileName = configPath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String backupName;
        if (dotIndex > 0) {
            backupName = fileName.substring(0, dotIndex) + "_backup_" + timestamp + fileName.substring(dotIndex);
        } else {
            backupName = fileName + "_backup_" + timestamp;
        }
        
        Path backupPath = configPath.resolveSibling(backupName);
        Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Created configuration backup: %s", backupName);
    }
    
    /**
     * Applies migrations sequentially from the current version to the target version.
     */
    private void applyMigrations(JsonObject config, int fromVersion, int toVersion) {
        int currentVersion = fromVersion;
        
        for (ConfigMigration migration : migrations) {
            if (migration.getFromVersion() == currentVersion && migration.getToVersion() <= toVersion) {
                logger.info("Applying migration: %s", migration.getDescription());
                try {
                    migration.migrate(config);
                    currentVersion = migration.getToVersion();
                    config.addProperty("configVersion", currentVersion);
                } catch (Exception e) {
                    logger.error("Migration failed: %s", e, migration.getDescription());
                    throw new RuntimeException("Configuration migration failed", e);
                }
            }
        }
        
        // Update to final version
        config.addProperty("configVersion", toVersion);
        logger.info("Configuration migrated from v%d to v%d", fromVersion, toVersion);
    }
    
    /**
     * Gets the shared Gson instance.
     *
     * @return The Gson instance
     */
    public static Gson getGson() {
        return GSON;
    }
}
