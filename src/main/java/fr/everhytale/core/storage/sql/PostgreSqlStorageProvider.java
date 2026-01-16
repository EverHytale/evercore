package fr.everhytale.core.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import fr.everhytale.core.config.DatabaseConfig;
import fr.everhytale.core.storage.StorageType;
import fr.everhytale.core.util.CoreLogger;

import java.util.function.Function;

/**
 * PostgreSQL database storage provider.
 *
 * @param <T> The type of data to store
 */
public class PostgreSqlStorageProvider<T> extends SqlStorageProvider<T> {
    
    /**
     * Creates a new PostgreSqlStorageProvider.
     *
     * @param config The database configuration
     * @param tableName The table name to use
     * @param serializer Function to serialize data to JSON string
     * @param deserializer Function to deserialize JSON string to data
     * @param logger The logger to use
     */
    public PostgreSqlStorageProvider(
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
        return StorageType.POSTGRESQL;
    }
    
    @Override
    protected String getJdbcUrl() {
        return String.format(
                "jdbc:postgresql://%s:%d/%s",
                config.getHost(),
                config.getPort(),
                config.getDatabase()
        );
    }
    
    @Override
    protected void configureDataSource(HikariConfig hikariConfig) {
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        // PostgreSQL specific settings
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    }
    
    @Override
    protected String getUpsertSql() {
        // PostgreSQL ON CONFLICT DO UPDATE
        return """
            INSERT INTO %s (uuid, data, updated_at) 
            VALUES (?, ?, CURRENT_TIMESTAMP) 
            ON CONFLICT (uuid) DO UPDATE SET data = ?, updated_at = CURRENT_TIMESTAMP
            """.formatted(tableName);
    }
}
