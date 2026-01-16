package fr.everhytale.core.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import fr.everhytale.core.config.DatabaseConfig;
import fr.everhytale.core.storage.StorageType;
import fr.everhytale.core.util.CoreLogger;

import java.nio.file.Path;
import java.util.function.Function;

/**
 * H2 embedded database storage provider.
 *
 * @param <T> The type of data to store
 */
public class H2StorageProvider<T> extends SqlStorageProvider<T> {
    
    private final Path dataPath;
    
    /**
     * Creates a new H2StorageProvider.
     *
     * @param dataPath The path for the H2 database file
     * @param config The database configuration
     * @param tableName The table name to use
     * @param serializer Function to serialize data to JSON string
     * @param deserializer Function to deserialize JSON string to data
     * @param logger The logger to use
     */
    public H2StorageProvider(
            Path dataPath,
            DatabaseConfig config,
            String tableName,
            Function<T, String> serializer,
            Function<String, T> deserializer,
            CoreLogger logger
    ) {
        super(config, tableName, serializer, deserializer, logger);
        this.dataPath = dataPath;
    }
    
    @Override
    public StorageType getType() {
        return StorageType.H2;
    }
    
    @Override
    protected String getJdbcUrl() {
        // H2 file-based database
        Path dbFile = dataPath.resolve("everessentials");
        return "jdbc:h2:" + dbFile.toAbsolutePath() + ";MODE=MySQL";
    }
    
    @Override
    protected void configureDataSource(HikariConfig hikariConfig) {
        hikariConfig.setDriverClassName("org.h2.Driver");
        // H2 specific settings
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    }
    
    @Override
    protected String getUpsertSql() {
        // H2 MERGE statement
        return """
            MERGE INTO %s (uuid, data, updated_at) 
            KEY (uuid) 
            VALUES (?, ?, CURRENT_TIMESTAMP)
            """.formatted(tableName);
    }
}
