package fr.everhytale.core.config;

import com.google.gson.JsonObject;

/**
 * Represents a migration from one configuration version to another.
 * 
 * <p>Migrations are applied sequentially to upgrade configurations from
 * older versions to newer ones.</p>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * public class MigrationV1ToV2 implements ConfigMigration {
 *     @Override
 *     public int getFromVersion() {
 *         return 1;
 *     }
 *     
 *     @Override
 *     public int getToVersion() {
 *         return 2;
 *     }
 *     
 *     @Override
 *     public void migrate(JsonObject config) {
 *         // Add new field with default value
 *         if (!config.has("newSetting")) {
 *             config.addProperty("newSetting", "defaultValue");
 *         }
 *         
 *         // Rename a field
 *         if (config.has("oldName")) {
 *             config.add("newName", config.get("oldName"));
 *             config.remove("oldName");
 *         }
 *         
 *         // Update version
 *         config.addProperty("configVersion", 2);
 *     }
 * }
 * }</pre>
 */
public interface ConfigMigration {
    
    /**
     * Gets the version this migration starts from.
     *
     * @return The source version
     */
    int getFromVersion();
    
    /**
     * Gets the version this migration upgrades to.
     *
     * @return The target version
     */
    int getToVersion();
    
    /**
     * Applies the migration to the configuration JSON.
     * 
     * <p>This method should modify the JsonObject in place, adding new fields,
     * renaming fields, restructuring data, etc.</p>
     * 
     * <p>The implementation should also update the configVersion field to
     * match {@link #getToVersion()}.</p>
     *
     * @param config The configuration JSON object to migrate
     */
    void migrate(JsonObject config);
    
    /**
     * Gets a description of what this migration does.
     *
     * @return A human-readable description
     */
    default String getDescription() {
        return "Migrate from v" + getFromVersion() + " to v" + getToVersion();
    }
}
