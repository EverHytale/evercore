package fr.everhytale.core.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.everhytale.core.config.DatabaseConfig;
import fr.everhytale.core.storage.StorageException;
import fr.everhytale.core.storage.StorageProvider;
import fr.everhytale.core.storage.StorageType;
import fr.everhytale.core.util.CoreLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Abstract SQL-based storage provider using HikariCP connection pool.
 *
 * @param <T> The type of data to store
 */
public abstract class SqlStorageProvider<T> implements StorageProvider<T> {
    
    protected final DatabaseConfig config;
    protected final String tableName;
    protected final Function<T, String> serializer;
    protected final Function<String, T> deserializer;
    protected final CoreLogger logger;
    protected final ExecutorService executor;
    
    protected HikariDataSource dataSource;
    
    /**
     * Creates a new SqlStorageProvider.
     *
     * @param config The database configuration
     * @param tableName The table name to use
     * @param serializer Function to serialize data to JSON string
     * @param deserializer Function to deserialize JSON string to data
     * @param logger The logger to use
     */
    protected SqlStorageProvider(
            DatabaseConfig config,
            String tableName,
            Function<T, String> serializer,
            Function<String, T> deserializer,
            CoreLogger logger
    ) {
        this.config = config;
        this.tableName = config.getTablePrefix() + tableName;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.logger = logger;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }
    
    @Override
    public void init() throws StorageException {
        try {
            initDataSource();
            createTable();
            logger.info("Initialized %s storage with table: %s", getType(), tableName);
        } catch (SQLException e) {
            throw new StorageException("Failed to initialize SQL storage", e);
        }
    }
    
    /**
     * Initializes the HikariCP data source.
     */
    protected void initDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(getJdbcUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setMaximumPoolSize(config.getPool().getMaximumPoolSize());
        hikariConfig.setMinimumIdle(config.getPool().getMinimumIdle());
        hikariConfig.setConnectionTimeout(config.getPool().getConnectionTimeout());
        hikariConfig.setIdleTimeout(config.getPool().getIdleTimeout());
        hikariConfig.setMaxLifetime(config.getPool().getMaxLifetime());
        hikariConfig.setPoolName("EverCore-" + getType());
        
        // Add driver-specific properties
        configureDataSource(hikariConfig);
        
        this.dataSource = new HikariDataSource(hikariConfig);
    }
    
    /**
     * Gets the JDBC URL for this database type.
     *
     * @return The JDBC URL
     */
    protected abstract String getJdbcUrl();
    
    /**
     * Configures driver-specific HikariCP settings.
     *
     * @param hikariConfig The HikariConfig to configure
     */
    protected void configureDataSource(HikariConfig hikariConfig) {
        // Override in subclasses for driver-specific settings
    }
    
    /**
     * Creates the storage table if it doesn't exist.
     *
     * @throws SQLException If table creation fails
     */
    protected void createTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS %s (
                uuid VARCHAR(36) PRIMARY KEY,
                data TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.formatted(tableName);
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        }
    }
    
    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Closed %s connection pool", getType());
        }
        executor.shutdown();
    }
    
    @Override
    public CompletableFuture<Optional<T>> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT data FROM %s WHERE uuid = ?".formatted(tableName);
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, uuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String json = rs.getString("data");
                        T data = deserializer.apply(json);
                        return Optional.ofNullable(data);
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to load data for UUID %s", e, uuid);
            }
            
            return Optional.empty();
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> save(UUID uuid, T data) {
        return CompletableFuture.runAsync(() -> {
            String sql = getUpsertSql();
            String json = serializer.apply(data);
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, uuid.toString());
                stmt.setString(2, json);
                stmt.setString(3, json); // For update
                stmt.executeUpdate();
                
                logger.debug("Saved data for UUID %s", uuid);
            } catch (SQLException e) {
                logger.error("Failed to save data for UUID %s", e, uuid);
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    /**
     * Gets the UPSERT SQL statement for this database type.
     *
     * @return The UPSERT SQL
     */
    protected abstract String getUpsertSql();
    
    @Override
    public CompletableFuture<Void> delete(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM %s WHERE uuid = ?".formatted(tableName);
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, uuid.toString());
                int deleted = stmt.executeUpdate();
                
                if (deleted > 0) {
                    logger.debug("Deleted data for UUID %s", uuid);
                }
            } catch (SQLException e) {
                logger.error("Failed to delete data for UUID %s", e, uuid);
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Boolean> exists(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM %s WHERE uuid = ?".formatted(tableName);
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, uuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                logger.error("Failed to check existence for UUID %s", e, uuid);
                return false;
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<List<UUID>> getAllKeys() {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> keys = new ArrayList<>();
            String sql = "SELECT uuid FROM %s".formatted(tableName);
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    try {
                        keys.add(UUID.fromString(rs.getString("uuid")));
                    } catch (IllegalArgumentException e) {
                        logger.debug("Skipping invalid UUID in database");
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to list all keys", e);
            }
            
            return keys;
        }, executor);
    }
    
    /**
     * Gets a connection from the pool.
     *
     * @return A database connection
     * @throws SQLException If connection fails
     */
    protected Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
