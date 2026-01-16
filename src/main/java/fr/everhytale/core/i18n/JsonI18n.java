package fr.everhytale.core.i18n;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fr.everhytale.core.reload.IReloadable;
import fr.everhytale.core.util.CoreLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON-based internationalization (i18n) support for multi-language messages.
 * 
 * <p>Uses JSON files with native UTF-8 encoding, supporting:
 * <ul>
 *   <li>Hierarchical message keys (e.g., "home.teleport")</li>
 *   <li>Named placeholders: {name}, {count}, etc.</li>
 *   <li>TinyMessage color tags: &lt;red&gt;, &lt;green&gt;, etc.</li>
 * </ul>
 * 
 * <h2>JSON Format:</h2>
 * <pre>{@code
 * {
 *   "home": {
 *     "teleport": "<green>Teleporting to '<yellow>{name}</yellow>'...</green>",
 *     "set": "<green>Home '{name}' set!</green>"
 *   }
 * }
 * }</pre>
 * 
 * <h2>Usage:</h2>
 * <pre>{@code
 * String msg = i18n.translate("home.teleport", "name", "mybase");
 * Message coloredMsg = TinyMsg.parse(msg);
 * player.sendMessage(coloredMsg);
 * }</pre>
 */
public class JsonI18n implements IReloadable {
    
    private static final String DEFAULT_FILE_PREFIX = "messages";
    private static final Gson GSON = new GsonBuilder().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>(){}.getType();
    
    /**
     * Pattern to match named placeholders: {name}
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");
    
    private final String filePrefix;
    private final ClassLoader classLoader;
    private final Path externalPath;
    private final CoreLogger logger;
    
    private Locale defaultLocale;
    private Map<String, String> defaultMessages = new HashMap<>();
    private final Map<Locale, Map<String, String>> messagesByLocale = new ConcurrentHashMap<>();
    
    /**
     * Creates a new JsonI18n instance.
     *
     * @param defaultLocale The default locale
     * @param classLoader The class loader for loading bundled resources
     * @param logger The logger
     */
    public JsonI18n(Locale defaultLocale, ClassLoader classLoader, CoreLogger logger) {
        this(DEFAULT_FILE_PREFIX, defaultLocale, classLoader, null, logger);
    }
    
    /**
     * Creates a new JsonI18n instance with external path support.
     *
     * @param defaultLocale The default locale
     * @param classLoader The class loader for loading bundled resources
     * @param externalPath Optional external path for custom message files
     * @param logger The logger
     */
    public JsonI18n(Locale defaultLocale, ClassLoader classLoader, Path externalPath, CoreLogger logger) {
        this(DEFAULT_FILE_PREFIX, defaultLocale, classLoader, externalPath, logger);
    }
    
    /**
     * Creates a new JsonI18n instance with custom file prefix.
     *
     * @param filePrefix The file prefix (e.g., "messages" for messages_en.json)
     * @param defaultLocale The default locale
     * @param classLoader The class loader for loading bundled resources
     * @param externalPath Optional external path for custom message files
     * @param logger The logger
     */
    public JsonI18n(String filePrefix, Locale defaultLocale, ClassLoader classLoader, 
                    Path externalPath, CoreLogger logger) {
        this.filePrefix = filePrefix;
        this.defaultLocale = defaultLocale;
        this.classLoader = classLoader;
        this.externalPath = externalPath;
        this.logger = logger;
        loadMessages();
    }
    
    /**
     * Loads messages for the default locale.
     */
    private void loadMessages() {
        // Load default messages (fallback)
        Map<String, String> fallback = loadMessagesForLocale(Locale.ENGLISH);
        if (fallback != null) {
            defaultMessages = fallback;
            messagesByLocale.put(Locale.ENGLISH, fallback);
        }
        
        // Load default locale if different from English
        if (!defaultLocale.equals(Locale.ENGLISH)) {
            Map<String, String> localeMessages = loadMessagesForLocale(defaultLocale);
            if (localeMessages != null) {
                defaultMessages = localeMessages;
                messagesByLocale.put(defaultLocale, localeMessages);
            }
        }
        
        logger.info("Loaded i18n messages for locale: %s (%d keys)", 
                defaultLocale, defaultMessages.size());
    }
    
    /**
     * Loads messages for a specific locale.
     *
     * @param locale The locale
     * @return Map of flattened message keys to values, or null if not found
     */
    private Map<String, String> loadMessagesForLocale(Locale locale) {
        String fileName = filePrefix + "_" + locale.getLanguage() + ".json";
        
        // Try external path first
        if (externalPath != null) {
            Path externalFile = externalPath.resolve(fileName);
            if (Files.exists(externalFile)) {
                try (var reader = Files.newBufferedReader(externalFile, StandardCharsets.UTF_8)) {
                    Map<String, Object> json = GSON.fromJson(reader, MAP_TYPE);
                    Map<String, String> flattened = flattenMessages(json, "");
                    logger.debug("Loaded external messages from: %s", externalFile);
                    return flattened;
                } catch (IOException e) {
                    logger.error("Failed to load external messages: %s", e, externalFile);
                }
            }
        }
        
        // Try bundled resource
        String resourcePath = fileName;
        try (InputStream is = classLoader.getResourceAsStream(resourcePath)) {
            if (is != null) {
                try (var reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    Map<String, Object> json = GSON.fromJson(reader, MAP_TYPE);
                    Map<String, String> flattened = flattenMessages(json, "");
                    logger.debug("Loaded bundled messages from: %s", resourcePath);
                    return flattened;
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load bundled messages: %s", e, resourcePath);
        }
        
        // Try without locale suffix (messages.json)
        String fallbackPath = filePrefix + ".json";
        try (InputStream is = classLoader.getResourceAsStream(fallbackPath)) {
            if (is != null) {
                try (var reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    Map<String, Object> json = GSON.fromJson(reader, MAP_TYPE);
                    Map<String, String> flattened = flattenMessages(json, "");
                    logger.debug("Loaded fallback messages from: %s", fallbackPath);
                    return flattened;
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load fallback messages: %s", e, fallbackPath);
        }
        
        logger.warn("No messages found for locale: %s", locale);
        return null;
    }
    
    /**
     * Flattens a nested JSON structure into dot-notation keys.
     * 
     * <p>Example: {"home": {"set": "value"}} becomes {"home.set": "value"}</p>
     *
     * @param json The nested JSON map
     * @param prefix Current key prefix
     * @return Flattened map
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> flattenMessages(Map<String, Object> json, String prefix) {
        Map<String, String> result = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                result.putAll(flattenMessages((Map<String, Object>) value, key));
            } else if (value != null) {
                result.put(key, String.valueOf(value));
            }
        }
        
        return result;
    }
    
    /**
     * Gets a translated message for the given key using the default locale.
     *
     * @param key The message key (e.g., "home.teleport")
     * @param args Key-value pairs for placeholders (key1, value1, key2, value2, ...)
     * @return The translated message with placeholders replaced
     */
    public String translate(String key, Object... args) {
        return translate(defaultLocale, key, args);
    }
    
    /**
     * Gets a translated message for the given key and locale.
     *
     * @param locale The locale
     * @param key The message key
     * @param args Key-value pairs for placeholders
     * @return The translated message
     */
    public String translate(Locale locale, String key, Object... args) {
        String message = getRawMessage(locale, key);
        
        if (args.length == 0) {
            return message;
        }
        
        return format(message, args);
    }
    
    /**
     * Gets a translated message using a map of placeholders.
     *
     * @param key The message key
     * @param placeholders Map of placeholder names to values
     * @return The translated message
     */
    public String translate(String key, Map<String, Object> placeholders) {
        return translate(defaultLocale, key, placeholders);
    }
    
    /**
     * Gets a translated message using a map of placeholders.
     *
     * @param locale The locale
     * @param key The message key
     * @param placeholders Map of placeholder names to values
     * @return The translated message
     */
    public String translate(Locale locale, String key, Map<String, Object> placeholders) {
        String message = getRawMessage(locale, key);
        
        if (placeholders == null || placeholders.isEmpty()) {
            return message;
        }
        
        return formatWithMap(message, placeholders);
    }
    
    /**
     * Gets the raw message without formatting.
     *
     * @param locale The locale
     * @param key The message key
     * @return The raw message, or the key if not found
     */
    public String getRawMessage(Locale locale, String key) {
        Map<String, String> messages = getMessagesForLocale(locale);
        
        String message = messages.get(key);
        if (message != null) {
            return message;
        }
        
        // Try default messages
        if (messages != defaultMessages) {
            message = defaultMessages.get(key);
            if (message != null) {
                return message;
            }
        }
        
        logger.debug("Missing translation key: %s", key);
        return key;
    }
    
    /**
     * Gets or loads messages for a locale.
     *
     * @param locale The locale
     * @return The messages map
     */
    private Map<String, String> getMessagesForLocale(Locale locale) {
        return messagesByLocale.computeIfAbsent(locale, l -> {
            Map<String, String> messages = loadMessagesForLocale(l);
            return messages != null ? messages : defaultMessages;
        });
    }
    
    /**
     * Formats a message with named placeholders using varargs.
     *
     * @param pattern The message pattern
     * @param args Key-value pairs
     * @return The formatted message
     */
    private String format(String pattern, Object[] args) {
        if (args.length % 2 != 0) {
            logger.warn("Invalid args count for formatting (should be key-value pairs): %s", pattern);
            return pattern;
        }
        
        Map<String, Object> placeholders = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            String name = String.valueOf(args[i]);
            Object value = args[i + 1];
            placeholders.put(name, value);
        }
        
        return formatWithMap(pattern, placeholders);
    }
    
    /**
     * Formats a message with named placeholders using a map.
     *
     * @param pattern The message pattern
     * @param placeholders Map of placeholder names to values
     * @return The formatted message
     */
    private String formatWithMap(String pattern, Map<String, Object> placeholders) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(pattern);
        StringBuilder result = new StringBuilder();
        
        while (matcher.find()) {
            String name = matcher.group(1);
            Object value = placeholders.get(name);
            
            String replacement;
            if (value != null) {
                replacement = String.valueOf(value);
            } else {
                replacement = matcher.group(0);
                logger.debug("No value for placeholder: {%s}", name);
            }
            
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Sets the default locale.
     *
     * @param locale The new default locale
     */
    public void setDefaultLocale(Locale locale) {
        this.defaultLocale = locale;
        Map<String, String> messages = loadMessagesForLocale(locale);
        if (messages != null) {
            defaultMessages = messages;
            messagesByLocale.put(locale, messages);
        }
    }
    
    /**
     * Gets the default locale.
     *
     * @return The default locale
     */
    public Locale getDefaultLocale() {
        return defaultLocale;
    }
    
    /**
     * Parses a locale from a string.
     *
     * @param localeString The locale string (e.g., "en", "en_US")
     * @return The parsed Locale
     */
    public static Locale parseLocale(String localeString) {
        if (localeString == null || localeString.isEmpty()) {
            return Locale.ENGLISH;
        }
        
        String[] parts = localeString.split("[_\\-.]");
        return switch (parts.length) {
            case 1 -> Locale.of(parts[0]);
            case 2 -> Locale.of(parts[0], parts[1]);
            default -> Locale.of(parts[0], parts[1], parts[2]);
        };
    }
    
    @Override
    public void reload() {
        messagesByLocale.clear();
        loadMessages();
        logger.info("Reloaded i18n messages");
    }
    
    @Override
    public String getReloadableName() {
        return "i18n";
    }
}
