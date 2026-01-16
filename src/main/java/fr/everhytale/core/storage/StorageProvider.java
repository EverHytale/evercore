package fr.everhytale.core.storage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract interface for storage providers.
 * All storage operations are asynchronous and return CompletableFuture.
 *
 * @param <T> The type of data this provider stores
 */
public interface StorageProvider<T> {
    
    /**
     * Initializes the storage provider.
     * This may create files, tables, connections, etc.
     *
     * @throws StorageException If initialization fails
     */
    void init() throws StorageException;
    
    /**
     * Shuts down the storage provider.
     * This should close connections and release resources.
     */
    void shutdown();
    
    /**
     * Gets the storage type of this provider.
     *
     * @return The storage type
     */
    StorageType getType();
    
    /**
     * Loads data for the specified UUID.
     *
     * @param uuid The UUID to load data for
     * @return CompletableFuture containing the data, or empty if not found
     */
    CompletableFuture<Optional<T>> load(UUID uuid);
    
    /**
     * Saves data for the specified UUID.
     *
     * @param uuid The UUID to save data for
     * @param data The data to save
     * @return CompletableFuture that completes when save is done
     */
    CompletableFuture<Void> save(UUID uuid, T data);
    
    /**
     * Deletes data for the specified UUID.
     *
     * @param uuid The UUID to delete data for
     * @return CompletableFuture that completes when deletion is done
     */
    CompletableFuture<Void> delete(UUID uuid);
    
    /**
     * Checks if data exists for the specified UUID.
     *
     * @param uuid The UUID to check
     * @return CompletableFuture containing true if data exists
     */
    CompletableFuture<Boolean> exists(UUID uuid);
    
    /**
     * Gets all UUIDs that have stored data.
     *
     * @return CompletableFuture containing list of UUIDs
     */
    CompletableFuture<List<UUID>> getAllKeys();
}
