package com.premierstudios.treasurehunt.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages configuration and messages for the TreasureHunt plugin.
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private String messagePrefix;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // Prefix must end with &r so the message body always starts with a clean color state.
        // If the admin removes &r from config, we append it defensively here.
        String raw = config.getString("plugin.prefix", "&6[TreasureHunt] &r");
        if (!raw.endsWith("&r") && !raw.endsWith("§r")) {
            raw = raw + "&r";
        }
        this.messagePrefix = ChatColor.translateAlternateColorCodes('&', raw);
    }

    public FileConfiguration getConfig() { return config; }

    public String getDatabaseHost()     { return config.getString("database.host", "localhost"); }
    public int    getDatabasePort()     { return config.getInt("database.port", 3306); }
    public String getDatabaseName()     { return config.getString("database.database", "treasurehunt"); }
    public String getDatabaseUsername() { return config.getString("database.username", "root"); }
    public String getDatabasePassword() { return config.getString("database.password", ""); }
    public int    getMaxPoolSize()      { return config.getInt("database.max-pool-size", 10); }
    public int    getMinPoolSize()      { return config.getInt("database.min-pool-size", 5); }
    public long   getMaxLifetime()      { return config.getLong("database.max-lifetime", 1800000); }
    public long   getIdleTimeout()      { return config.getLong("database.idle-timeout", 600000); }

    /**
     * Returns a fully formatted message: [prefix][translated message with placeholders].
     *
     * The prefix always ends with §r, ensuring the message body starts with a completely
     * clean color state — no bleeding, no leftover formatting from the prefix.
     *
     * @param key          message key under messages.* in config.yml
     * @param replacements alternating key/value pairs: "id", playerId, "player", playerName, …
     */
    public String getMessage(String key, Object... replacements) {
        String raw = config.getString("messages." + key, "");

        if (raw == null || raw.isEmpty()) {
            return messagePrefix + ChatColor.RED + "Missing message: " + key;
        }

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace("%" + replacements[i] + "%", String.valueOf(replacements[i + 1]));
        }

        return messagePrefix + ChatColor.translateAlternateColorCodes('&', raw);
    }

    /**
     * Returns the raw config string for a key — no prefix, no color translation.
     * Used for inventory titles passed to Bukkit.createInventory().
     */
    public String getRawMessage(String key) {
        return config.getString("messages." + key, "");
    }

    /**
     * Returns the config message with color codes translated, but without the plugin prefix.
     * Used for GUI display names and inventory title comparisons.
     */
    public String getTranslatedRawMessage(String key) {
        String raw = config.getString("messages." + key, "");
        return raw.isEmpty() ? "" : ChatColor.translateAlternateColorCodes('&', raw);
    }

    public String getMessagePrefix() { return messagePrefix; }
}
