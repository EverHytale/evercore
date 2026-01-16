package fr.everhytale.core.config;

/**
 * Interface for configuration classes that support versioning and migration.
 * 
 * <p>Implementing classes should include a version field that is incremented
 * when breaking changes are made to the configuration structure.</p>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * public class MyConfig implements VersionedConfig {
 *     public static final int CURRENT_VERSION = 2;
 *     
 *     private int configVersion = CURRENT_VERSION;
 *     private String setting = "default";
 *     // ... other fields
 *     
 *     @Override
 *     public int getConfigVersion() {
 *         return configVersion;
 *     }
 *     
 *     @Override
 *     public void setConfigVersion(int version) {
 *         this.configVersion = version;
 *     }
 *     
 *     @Override
 *     public int getCurrentVersion() {
 *         return CURRENT_VERSION;
 *     }
 * }
 * }</pre>
 */
public interface VersionedConfig {
    
    /**
     * Gets the version of this configuration instance.
     * This is the version that was saved in the config file.
     *
     * @return The config version from the file
     */
    int getConfigVersion();
    
    /**
     * Sets the version of this configuration instance.
     *
     * @param version The new version
     */
    void setConfigVersion(int version);
    
    /**
     * Gets the current/latest version for this configuration type.
     * This should return a constant value representing the newest version.
     *
     * @return The current/latest version
     */
    int getCurrentVersion();
    
    /**
     * Checks if this configuration needs to be migrated.
     *
     * @return true if the config version is less than the current version
     */
    default boolean needsMigration() {
        return getConfigVersion() < getCurrentVersion();
    }
}
