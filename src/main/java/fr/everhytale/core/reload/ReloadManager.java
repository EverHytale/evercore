package fr.everhytale.core.reload;

import fr.everhytale.core.util.CoreLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages reloadable components and provides hot-reload functionality.
 */
public class ReloadManager {
    
    private final List<IReloadable> reloadables = new ArrayList<>();
    private final CoreLogger logger;
    
    /**
     * Creates a new ReloadManager.
     *
     * @param logger The logger to use
     */
    public ReloadManager(CoreLogger logger) {
        this.logger = logger;
    }
    
    /**
     * Registers a reloadable component.
     *
     * @param reloadable The component to register
     */
    public void register(IReloadable reloadable) {
        reloadables.add(reloadable);
        logger.debug("Registered reloadable: %s", reloadable.getReloadableName());
    }
    
    /**
     * Unregisters a reloadable component.
     *
     * @param reloadable The component to unregister
     * @return true if the component was registered
     */
    public boolean unregister(IReloadable reloadable) {
        boolean removed = reloadables.remove(reloadable);
        if (removed) {
            logger.debug("Unregistered reloadable: %s", reloadable.getReloadableName());
        }
        return removed;
    }
    
    /**
     * Reloads all registered components.
     */
    public void reloadAll() {
        logger.info("Reloading all components...");
        int success = 0;
        int failed = 0;
        
        for (IReloadable reloadable : reloadables) {
            try {
                reloadable.reload();
                logger.info("Reloaded: %s", reloadable.getReloadableName());
                success++;
            } catch (Exception e) {
                logger.error("Failed to reload: %s", e, reloadable.getReloadableName());
                failed++;
            }
        }
        
        logger.info("Reload complete. Success: %d, Failed: %d", success, failed);
    }
    
    /**
     * Reloads a specific component by name.
     *
     * @param name The name of the component to reload
     * @return true if the component was found and reloaded successfully
     */
    public boolean reload(String name) {
        for (IReloadable reloadable : reloadables) {
            if (reloadable.getReloadableName().equalsIgnoreCase(name)) {
                try {
                    reloadable.reload();
                    logger.info("Reloaded: %s", reloadable.getReloadableName());
                    return true;
                } catch (Exception e) {
                    logger.error("Failed to reload: %s", e, reloadable.getReloadableName());
                    return false;
                }
            }
        }
        return false;
    }
    
    /**
     * Gets a list of all reloadable component names.
     *
     * @return Unmodifiable list of component names
     */
    public List<String> getReloadableNames() {
        List<String> names = new ArrayList<>();
        for (IReloadable reloadable : reloadables) {
            names.add(reloadable.getReloadableName());
        }
        return Collections.unmodifiableList(names);
    }
    
    /**
     * Gets the number of registered reloadable components.
     *
     * @return The count
     */
    public int getCount() {
        return reloadables.size();
    }
    
    /**
     * Clears all registered reloadable components.
     */
    public void clear() {
        reloadables.clear();
    }
}
