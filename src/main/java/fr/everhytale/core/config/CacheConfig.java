package fr.everhytale.core.config;

/**
 * Configuration for the cache system.
 */
public class CacheConfig {
    
    private boolean enabled = true;
    private int expireMinutes = 30;
    private int cleanupIntervalSeconds = 60;
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getExpireMinutes() {
        return expireMinutes;
    }
    
    public void setExpireMinutes(int expireMinutes) {
        this.expireMinutes = expireMinutes;
    }
    
    public int getCleanupIntervalSeconds() {
        return cleanupIntervalSeconds;
    }
    
    public void setCleanupIntervalSeconds(int cleanupIntervalSeconds) {
        this.cleanupIntervalSeconds = cleanupIntervalSeconds;
    }
}
