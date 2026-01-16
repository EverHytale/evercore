package fr.everhytale.core.storage;

import fr.everhytale.core.config.DatabaseConfig;
import fr.everhytale.core.storage.json.JsonStorageProvider;
import fr.everhytale.core.storage.sql.H2StorageProvider;
import fr.everhytale.core.storage.sql.MySqlStorageProvider;
import fr.everhytale.core.storage.sql.PostgreSqlStorageProvider;
import fr.everhytale.core.util.CoreLogger;

import java.nio.file.Path;
import java.util.function.Function;

/**
 * Factory for creating storage providers based on configuration.
 */
public class StorageFactory {
    
    /**
     * Creates a storage provider based on the storage type.
     *
     * @param <T> The data type
     * @param type The storage type
     * @param dataPath The base path for data storage
     * @param subFolder The subfolder for this data type
     * @param dataClass The class of the data type
     * @param dbConfig The database configuration (for SQL types)
     * @param tableName The table name (for SQL types)
     * @param serializer Function to serialize data to JSON string
     * @param deserializer Function to deserialize JSON string to data
     * @param logger The logger to use
     * @return The created storage provider
     */
    public static <T> StorageProvider<T> create(
            StorageType type,
            Path dataPath,
            String subFolder,
            Class<T> dataClass,
            DatabaseConfig dbConfig,
            String tableName,
            Function<T, String> serializer,
            Function<String, T> deserializer,
            CoreLogger logger
    ) {
        return switch (type) {
            case JSON -> new JsonStorageProvider<>(
                    dataPath.resolve(subFolder),
                    dataClass,
                    serializer,
                    deserializer,
                    logger
            );
            case H2 -> new H2StorageProvider<>(
                    dataPath,
                    dbConfig,
                    tableName,
                    serializer,
                    deserializer,
                    logger
            );
            case MYSQL -> new MySqlStorageProvider<>(
                    dbConfig,
                    tableName,
                    serializer,
                    deserializer,
                    logger
            );
            case POSTGRESQL -> new PostgreSqlStorageProvider<>(
                    dbConfig,
                    tableName,
                    serializer,
                    deserializer,
                    logger
            );
        };
    }
}
