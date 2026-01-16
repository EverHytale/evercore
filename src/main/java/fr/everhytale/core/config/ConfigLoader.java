package fr.everhytale.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for loading and saving JSON configuration files.
 */
public class ConfigLoader {
    
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    
    /**
     * Loads a configuration object from a JSON file.
     * If the file doesn't exist, creates it with default values from the resource.
     *
     * @param <T> The type of configuration object
     * @param configPath The path to the configuration file
     * @param clazz The class of the configuration object
     * @param resourcePath The path to the default resource in the JAR
     * @param classLoader The class loader to use for loading resources
     * @return The loaded configuration object
     * @throws IOException If an I/O error occurs
     */
    public static <T> T loadOrCreate(Path configPath, Class<T> clazz, String resourcePath, ClassLoader classLoader) throws IOException {
        if (!Files.exists(configPath)) {
            // Create parent directories if needed
            Path parent = configPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            
            // Copy default config from resources
            try (InputStream is = classLoader.getResourceAsStream(resourcePath)) {
                if (is != null) {
                    Files.copy(is, configPath);
                } else {
                    // Create empty config if no resource exists
                    try {
                        T defaultConfig = clazz.getDeclaredConstructor().newInstance();
                        save(configPath, defaultConfig);
                        return defaultConfig;
                    } catch (ReflectiveOperationException e) {
                        throw new IOException("Failed to create default config", e);
                    }
                }
            }
        }
        
        return load(configPath, clazz);
    }
    
    /**
     * Loads a configuration object from a JSON file.
     *
     * @param <T> The type of configuration object
     * @param configPath The path to the configuration file
     * @param clazz The class of the configuration object
     * @return The loaded configuration object
     * @throws IOException If an I/O error occurs
     */
    public static <T> T load(Path configPath, Class<T> clazz) throws IOException {
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, clazz);
        }
    }
    
    /**
     * Loads a configuration object from an input stream.
     *
     * @param <T> The type of configuration object
     * @param inputStream The input stream to read from
     * @param clazz The class of the configuration object
     * @return The loaded configuration object
     */
    public static <T> T load(InputStream inputStream, Class<T> clazz) {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config from stream", e);
        }
    }
    
    /**
     * Saves a configuration object to a JSON file.
     *
     * @param <T> The type of configuration object
     * @param configPath The path to the configuration file
     * @param config The configuration object to save
     * @throws IOException If an I/O error occurs
     */
    public static <T> void save(Path configPath, T config) throws IOException {
        // Create parent directories if needed
        Path parent = configPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        
        try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            GSON.toJson(config, writer);
        }
    }
    
    /**
     * Gets the shared Gson instance.
     *
     * @return The Gson instance
     */
    public static Gson getGson() {
        return GSON;
    }
}
