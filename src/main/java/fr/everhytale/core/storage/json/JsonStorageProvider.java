package fr.everhytale.core.storage.json;

import fr.everhytale.core.storage.StorageException;
import fr.everhytale.core.storage.StorageProvider;
import fr.everhytale.core.storage.StorageType;
import fr.everhytale.core.util.CoreLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * JSON file-based storage provider.
 * Stores each entry in a separate JSON file named by UUID.
 *
 * @param <T> The type of data to store
 */
public class JsonStorageProvider<T> implements StorageProvider<T> {
    
    private final Path dataPath;
    private final Class<T> dataClass;
    private final Function<T, String> serializer;
    private final Function<String, T> deserializer;
    private final CoreLogger logger;
    private final ExecutorService executor;
    
    /**
     * Creates a new JsonStorageProvider.
     *
     * @param dataPath The path to store JSON files
     * @param dataClass The class of the data type
     * @param serializer Function to serialize data to JSON string
     * @param deserializer Function to deserialize JSON string to data
     * @param logger The logger to use
     */
    public JsonStorageProvider(
            Path dataPath,
            Class<T> dataClass,
            Function<T, String> serializer,
            Function<String, T> deserializer,
            CoreLogger logger
    ) {
        this.dataPath = dataPath;
        this.dataClass = dataClass;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.logger = logger;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }
    
    @Override
    public void init() throws StorageException {
        try {
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
                logger.debug("Created data directory: %s", dataPath);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to create data directory: " + dataPath, e);
        }
    }
    
    @Override
    public void shutdown() {
        executor.shutdown();
    }
    
    @Override
    public StorageType getType() {
        return StorageType.JSON;
    }
    
    @Override
    public CompletableFuture<Optional<T>> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Path file = getFilePath(uuid);
            if (!Files.exists(file)) {
                return Optional.empty();
            }
            
            try {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                T data = deserializer.apply(json);
                return Optional.ofNullable(data);
            } catch (IOException e) {
                logger.error("Failed to load data for UUID %s", e, uuid);
                return Optional.empty();
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> save(UUID uuid, T data) {
        return CompletableFuture.runAsync(() -> {
            Path file = getFilePath(uuid);
            try {
                String json = serializer.apply(data);
                Files.writeString(file, json, StandardCharsets.UTF_8);
                logger.debug("Saved data for UUID %s", uuid);
            } catch (IOException e) {
                logger.error("Failed to save data for UUID %s", e, uuid);
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> delete(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            Path file = getFilePath(uuid);
            try {
                if (Files.exists(file)) {
                    Files.delete(file);
                    logger.debug("Deleted data for UUID %s", uuid);
                }
            } catch (IOException e) {
                logger.error("Failed to delete data for UUID %s", e, uuid);
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Boolean> exists(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Path file = getFilePath(uuid);
            return Files.exists(file);
        }, executor);
    }
    
    @Override
    public CompletableFuture<List<UUID>> getAllKeys() {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> keys = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataPath, "*.json")) {
                for (Path file : stream) {
                    String fileName = file.getFileName().toString();
                    String uuidStr = fileName.substring(0, fileName.length() - 5); // Remove .json
                    try {
                        keys.add(UUID.fromString(uuidStr));
                    } catch (IllegalArgumentException e) {
                        // Skip files that don't have valid UUID names
                        logger.debug("Skipping non-UUID file: %s", fileName);
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to list data files", e);
            }
            return keys;
        }, executor);
    }
    
    /**
     * Gets the file path for a UUID.
     *
     * @param uuid The UUID
     * @return The file path
     */
    private Path getFilePath(UUID uuid) {
        return dataPath.resolve(uuid.toString() + ".json");
    }
}
