package fr.everhytale.core.storage;

/**
 * Enum representing the supported storage backend types.
 */
public enum StorageType {
    
    /**
     * File-based JSON storage.
     * Best for small servers or development.
     */
    JSON("json"),
    
    /**
     * H2 embedded database.
     * Best for medium-sized servers.
     */
    H2("h2"),
    
    /**
     * MySQL external database.
     * Best for large or multi-server setups.
     */
    MYSQL("mysql"),
    
    /**
     * PostgreSQL external database.
     * Alternative to MySQL for large setups.
     */
    POSTGRESQL("postgresql");
    
    private final String identifier;
    
    StorageType(String identifier) {
        this.identifier = identifier;
    }
    
    /**
     * Gets the string identifier for this storage type.
     *
     * @return The identifier
     */
    public String getIdentifier() {
        return identifier;
    }
    
    /**
     * Parses a storage type from its string identifier.
     *
     * @param identifier The string identifier
     * @return The storage type, or JSON as default
     */
    public static StorageType fromString(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return JSON;
        }
        
        String lower = identifier.toLowerCase();
        for (StorageType type : values()) {
            if (type.identifier.equals(lower)) {
                return type;
            }
        }
        
        // Also check enum names
        try {
            return valueOf(identifier.toUpperCase());
        } catch (IllegalArgumentException e) {
            return JSON;
        }
    }
    
    /**
     * Checks if this storage type requires a SQL database.
     *
     * @return true if SQL-based
     */
    public boolean isSql() {
        return this == H2 || this == MYSQL || this == POSTGRESQL;
    }
    
    @Override
    public String toString() {
        return identifier;
    }
}
