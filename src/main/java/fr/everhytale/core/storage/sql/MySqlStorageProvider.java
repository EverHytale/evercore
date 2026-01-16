package fr.everhytale.core.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import fr.everhytale.core.config.DatabaseConfig;
import fr.everhytale.core.storage.StorageType;
import fr.everhytale.core.util.CoreLogger;

import java.util.function.Function;

/**
 * MySQL database storage provider.
 *
 * @param <T> The type of data to store
 */
public class MySqlStorageProvider<T> extends SqlStorageProvider<T> {
    
    /**
     * Creates a new MySqlStorageProvider.
     *
     * @param config The database configuration
     * @param tableName The table name to use
     * @param serializer Function to serialize data to JSON string
     * @param deserializer Function to deserialize JSON string to data
     * @param logger The logger to use
     */
    public MySqlStorageProvider(
            DatabaseConfig config,
            String tableName,
            Function<T, String> serializer,
            Function<String, T> deserializer,
            CoreLogger logger
    ) {
        super(config, tableName, serializer, deserializer, logger);
    }
    
    @Override
    public StorageType getType() {
        return StorageType.MYSQL;
    }
    
    @Override
    protected String getJdbcUrl() {
        return String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                config.getHost(),
                config.getPort(),
                config.getDatabase()
        );
    }
    
    @Override
    protected void configureDataSource(HikariConfig hikariConfig) {
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        // MySQL specific settings
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
    }
    
    @Override
    protected String getUpsertSql() {
        // MySQL ON DUPLICATE KEY UPDATE
        return """
            INSERT INTO %s (uuid, data, updated_at) 
            VALUES (?, ?, CURRENT_TIMESTAMP) 
            ON DUPLICATE KEY UPDATE data = ?, updated_at = CURRENT_TIMESTAMP
            """.formatted(tableName);
    }
}
