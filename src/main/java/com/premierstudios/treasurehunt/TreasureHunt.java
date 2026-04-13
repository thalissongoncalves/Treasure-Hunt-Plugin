package com.premierstudios.treasurehunt;

import com.premierstudios.treasurehunt.command.TreasureCommand;
import com.premierstudios.treasurehunt.command.TreasureTabCompleter;
import com.premierstudios.treasurehunt.config.ConfigManager;
import com.premierstudios.treasurehunt.database.DatabaseManager;
import com.premierstudios.treasurehunt.gui.TreasureGUI;
import com.premierstudios.treasurehunt.listener.TreasureListener;
import com.premierstudios.treasurehunt.model.PendingTreasure;
import com.premierstudios.treasurehunt.treasure.TreasureManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Main plugin class for TreasureHunt.
 * Initializes all components and registers commands, listeners, and events.
 */
public class TreasureHunt extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private TreasureManager treasureManager;
    private TreasureGUI treasureGUI;
    private final Map<UUID, PendingTreasure> pendingTreasures = new HashMap<>();

    @Override
    public void onEnable() {
        getLogger().info("=== TreasureHunt Plugin Enabled ===");

        // Initialize configuration
        configManager = new ConfigManager(this);
        getLogger().info("Configuration loaded.");

        // Initialize database
        databaseManager = new DatabaseManager(this, configManager);
        if (!databaseManager.initialize()) {
            getLogger().severe("Failed to initialize database! Disabling plugin.");
            setEnabled(false);
            return;
        }

        // Initialize treasure manager
        treasureManager = new TreasureManager(this, databaseManager);
        treasureManager.reload();

        // Initialize GUI
        treasureGUI = new TreasureGUI(this, treasureManager, configManager);

        // Register commands
        registerCommands();

        // Register listeners
        registerListeners();

        getLogger().info("=== TreasureHunt Plugin Fully Loaded ===");
    }

    @Override
    public void onDisable() {
        getLogger().info("=== TreasureHunt Plugin Disabled ===");

        // Close database connection
        if (databaseManager != null) {
            databaseManager.close();
        }

        // Clear pending treasures
        pendingTreasures.clear();

        getLogger().info("Resources cleaned up.");
    }

    /**
     * Registers all commands for the plugin.
     */
    private void registerCommands() {
        TreasureCommand treasureCommand = new TreasureCommand(
                treasureManager,
                configManager,
                treasureGUI,
                pendingTreasures
        );

        TreasureTabCompleter tabCompleter = new TreasureTabCompleter(treasureManager);

        getCommand("treasure").setExecutor(treasureCommand);
        getCommand("treasure").setTabCompleter(tabCompleter);

        getLogger().info("Commands registered.");
    }

    /**
     * Registers all event listeners for the plugin.
     */
    private void registerListeners() {
        TreasureListener treasureListener = new TreasureListener(
                this,
                treasureManager,
                configManager,
                pendingTreasures
        );

        getServer().getPluginManager().registerEvents(treasureListener, this);
        getServer().getPluginManager().registerEvents(treasureGUI, this);

        getLogger().info("Listeners registered.");
    }

    /**
     * Gets the treasure manager instance.
     *
     * @return the treasure manager
     */
    public TreasureManager getTreasureManager() {
        return treasureManager;
    }

    /**
     * Gets the database manager instance.
     *
     * @return the database manager
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * Gets the configuration manager instance.
     *
     * @return the configuration manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Gets the treasure GUI instance.
     *
     * @return the treasure GUI
     */
    public TreasureGUI getTreasureGUI() {
        return treasureGUI;
    }
}
