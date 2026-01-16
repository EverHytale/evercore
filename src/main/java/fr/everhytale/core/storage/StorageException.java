package fr.everhytale.core.storage;

/**
 * Exception thrown when a storage operation fails.
 */
public class StorageException extends Exception {
    
    /**
     * Creates a new StorageException with a message.
     *
     * @param message The error message
     */
    public StorageException(String message) {
        super(message);
    }
    
    /**
     * Creates a new StorageException with a message and cause.
     *
     * @param message The error message
     * @param cause The underlying cause
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Creates a new StorageException with a cause.
     *
     * @param cause The underlying cause
     */
    public StorageException(Throwable cause) {
        super(cause);
    }
}
