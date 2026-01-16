package fr.everhytale.core.reload;

/**
 * Interface for components that support hot-reloading.
 * Implementations should reload their configuration/state when reload() is called.
 */
public interface IReloadable {
    
    /**
     * Reloads the component's configuration/state.
     * This method should be safe to call at any time.
     */
    void reload();
    
    /**
     * Gets the name of this reloadable component.
     * Used for logging and the reload command.
     *
     * @return The component name
     */
    String getReloadableName();
}
