package fr.everhytale.core.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Generic cache manager with async loading and expiration support.
 * Entries are automatically loaded when accessed and expired entries are cleaned up periodically.
 *
 * @param <K> The type of cache keys
 * @param <V> The type of cached values
 */
public class CacheManager<K, V> {
    
    private final ConcurrentHashMap<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final Function<K, CompletableFuture<V>> loader;
    private final Duration expireAfter;
    private final ScheduledExecutorService scheduler;
    private volatile boolean shutdown = false;
    
    /**
     * Creates a new CacheManager.
     *
     * @param loader Function to load values when not in cache
     * @param expireAfter Duration after which entries expire
     */
    public CacheManager(Function<K, CompletableFuture<V>> loader, Duration expireAfter) {
        this(loader, expireAfter, Duration.ofMinutes(1));
    }
    
    /**
     * Creates a new CacheManager with custom cleanup interval.
     *
     * @param loader Function to load values when not in cache
     * @param expireAfter Duration after which entries expire
     * @param cleanupInterval Interval for cleanup task
     */
    public CacheManager(Function<K, CompletableFuture<V>> loader, Duration expireAfter, Duration cleanupInterval) {
        this.loader = loader;
        this.expireAfter = expireAfter;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CacheManager-Cleanup");
            t.setDaemon(true);
            return t;
        });
        
        // Schedule periodic cleanup
        scheduler.scheduleAtFixedRate(
                this::cleanup,
                cleanupInterval.toSeconds(),
                cleanupInterval.toSeconds(),
                TimeUnit.SECONDS
        );
    }
    
    /**
     * Gets a value from the cache, loading it if not present or expired.
     *
     * @param key The key to get
     * @return CompletableFuture that completes with the value
     */
    public CompletableFuture<V> get(K key) {
        if (shutdown) {
            return CompletableFuture.failedFuture(new IllegalStateException("CacheManager is shutdown"));
        }
        
        CacheEntry<V> entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return CompletableFuture.completedFuture(entry.getValue());
        }
        
        // Load async and cache the result
        return loader.apply(key).thenApply(value -> {
            if (value != null) {
                cache.put(key, new CacheEntry<>(value, expireAfter));
            }
            return value;
        });
    }
    
    /**
     * Gets a value from the cache only if present and not expired.
     *
     * @param key The key to get
     * @return The cached value or null if not present/expired
     */
    public V getIfPresent(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        return null;
    }
    
    /**
     * Puts a value directly into the cache.
     *
     * @param key The key
     * @param value The value to cache
     */
    public void put(K key, V value) {
        if (value != null) {
            cache.put(key, new CacheEntry<>(value, expireAfter));
        }
    }
    
    /**
     * Forces a reload of the value from the loader.
     *
     * @param key The key to reload
     * @return CompletableFuture that completes with the reloaded value
     */
    public CompletableFuture<V> reload(K key) {
        if (shutdown) {
            return CompletableFuture.failedFuture(new IllegalStateException("CacheManager is shutdown"));
        }
        
        return loader.apply(key).thenApply(value -> {
            if (value != null) {
                cache.put(key, new CacheEntry<>(value, expireAfter));
            } else {
                cache.remove(key);
            }
            return value;
        });
    }
    
    /**
     * Preloads a value into the cache.
     *
     * @param key The key to preload
     * @return CompletableFuture that completes when loading is done
     */
    public CompletableFuture<Void> preload(K key) {
        return get(key).thenAccept(v -> {});
    }
    
    /**
     * Evicts an entry from the cache.
     *
     * @param key The key to evict
     * @return The evicted value or null if not present
     */
    public V evict(K key) {
        CacheEntry<V> entry = cache.remove(key);
        return entry != null ? entry.getValue() : null;
    }
    
    /**
     * Checks if a key is present in the cache and not expired.
     *
     * @param key The key to check
     * @return true if present and not expired
     */
    public boolean contains(K key) {
        CacheEntry<V> entry = cache.get(key);
        return entry != null && !entry.isExpired();
    }
    
    /**
     * Returns the number of entries in the cache (including expired).
     *
     * @return The cache size
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * Clears all entries from the cache.
     */
    public void clear() {
        cache.clear();
    }
    
    /**
     * Cleans up expired entries from the cache.
     */
    public void cleanup() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    /**
     * Shuts down the cache manager and cleans up resources.
     */
    public void shutdown() {
        shutdown = true;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        cache.clear();
    }
    
    /**
     * Gets all cached entries (for debugging/admin purposes).
     *
     * @return Unmodifiable map of all entries
     */
    public Map<K, V> getAll() {
        var result = new ConcurrentHashMap<K, V>();
        cache.forEach((k, v) -> {
            if (!v.isExpired()) {
                result.put(k, v.getValue());
            }
        });
        return result;
    }
    
    /**
     * Internal cache entry with expiration tracking.
     */
    private static class CacheEntry<V> {
        private final V value;
        private final Instant expiresAt;
        
        CacheEntry(V value, Duration ttl) {
            this.value = value;
            this.expiresAt = Instant.now().plus(ttl);
        }
        
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
        
        V getValue() {
            return value;
        }
        
        Instant getExpiresAt() {
            return expiresAt;
        }
    }
}
