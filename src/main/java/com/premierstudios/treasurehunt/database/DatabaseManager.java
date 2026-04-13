package com.premierstudios.treasurehunt.database;

import com.premierstudios.treasurehunt.config.ConfigManager;
import com.premierstudios.treasurehunt.treasure.LocationSerializer;
import com.premierstudios.treasurehunt.treasure.Treasure;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages all database operations for the TreasureHunt plugin.
 * Uses HikariCP for connection pooling and PreparedStatements for SQL safety.
 * All location comparisons use block-level coordinates only.
 */
public class DatabaseManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;

    /**
     * Creates a new DatabaseManager instance.
     *
     * @param plugin the TreasureHunt plugin instance
     * @param configManager the configuration manager
     */
    public DatabaseManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Initializes the database connection pool and creates tables.
     *
     * @return true if initialization was successful, false otherwise
     */
    public boolean initialize() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(String.format(
                    "jdbc:mysql://%s:%d/%s",
                    configManager.getDatabaseHost(),
                    configManager.getDatabasePort(),
                    configManager.getDatabaseName()
            ));
            config.setUsername(configManager.getDatabaseUsername());
            config.setPassword(configManager.getDatabasePassword());
            config.setMaximumPoolSize(configManager.getMaxPoolSize());
            config.setMinimumIdle(configManager.getMinPoolSize());
            config.setMaxLifetime(configManager.getMaxLifetime());
            config.setIdleTimeout(configManager.getIdleTimeout());
            config.setConnectionTimeout(30000);

            this.dataSource = new HikariDataSource(config);

            // Test the connection
            try (Connection conn = dataSource.getConnection()) {
                plugin.getLogger().info("Database connection successful!");
            }

            // Initialize tables
            if (!DatabaseInitializer.initialize(dataSource)) {
                plugin.getLogger().severe("Failed to initialize database tables!");
                return false;
            }

            plugin.getLogger().info("Database initialized successfully!");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Closes the database connection pool.
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed.");
        }
    }

    /**
     * Saves a treasure to the database.
     * Uses block coordinates only for location storage.
     *
     * @param treasure the treasure to save
     * @return true if save was successful, false otherwise
     */
    public boolean saveTreasure(Treasure treasure) {
        String sql = "INSERT INTO treasures (id, command, world, x, y, z) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, treasure.getId());
            stmt.setString(2, treasure.getCommand());
            stmt.setString(3, treasure.getLocation().getWorld().getName());
            stmt.setInt(4, treasure.getLocation().getBlockX());
            stmt.setInt(5, treasure.getLocation().getBlockY());
            stmt.setInt(6, treasure.getLocation().getBlockZ());

            stmt.executeUpdate();
            plugin.getLogger().info("Treasure '" + treasure.getId() + "' saved to database.");
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save treasure: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a treasure from the database.
     *
     * @param treasureId the ID of the treasure to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteTreasure(String treasureId) {
        String sql = "DELETE FROM treasures WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, treasureId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                plugin.getLogger().info("Treasure '" + treasureId + "' deleted from database.");
                return true;
            }
            return false;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete treasure: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Loads all treasures from the database.
     *
     * @return a list of all treasures
     */
    public List<Treasure> loadAllTreasures() {
        List<Treasure> treasures = new ArrayList<>();
        String sql = "SELECT id, command, world, x, y, z, created_at FROM treasures";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id");
                String command = rs.getString("command");
                String world = rs.getString("world");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

                Location location = new Location(
                        org.bukkit.Bukkit.getWorld(world),
                        x + 0.5,
                        y,
                        z + 0.5
                );

                treasures.add(new Treasure(id, command, location, createdAt));
            }

            plugin.getLogger().info("Loaded " + treasures.size() + " treasures from database.");
            return treasures;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load treasures: " + e.getMessage());
            e.printStackTrace();
            return treasures;
        }
    }

    /**
     * Loads a single treasure by ID.
     *
     * @param treasureId the ID of the treasure
     * @return the treasure, or null if not found
     */
    public Treasure loadTreasure(String treasureId) {
        String sql = "SELECT id, command, world, x, y, z, created_at FROM treasures WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, treasureId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String id = rs.getString("id");
                    String command = rs.getString("command");
                    String world = rs.getString("world");
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

                    Location location = new Location(
                            org.bukkit.Bukkit.getWorld(world),
                            x + 0.5,
                            y,
                            z + 0.5
                    );

                    return new Treasure(id, command, location, createdAt);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load treasure: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Marks a treasure as completed by a player.
     *
     * @param treasureId the ID of the treasure
     * @param playerUuid the UUID of the player
     * @return true if marking was successful, false otherwise
     */
    public boolean markTreasureCompleted(String treasureId, String playerUuid) {
        String sql = "INSERT IGNORE INTO treasure_completed (treasure_id, player_uuid) VALUES (?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, treasureId);
            stmt.setString(2, playerUuid);

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to mark treasure as completed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks if a player has already completed a treasure.
     *
     * @param treasureId the ID of the treasure
     * @param playerUuid the UUID of the player
     * @return true if the treasure was already completed by the player, false otherwise
     */
    public boolean isTreasureCompleted(String treasureId, String playerUuid) {
        String sql = "SELECT 1 FROM treasure_completed WHERE treasure_id = ? AND player_uuid = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, treasureId);
            stmt.setString(2, playerUuid);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check treasure completion: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets all players who have completed a specific treasure.
     *
     * @param treasureId the ID of the treasure
     * @return a list of player UUIDs who completed the treasure
     */
    public List<String> getTreasureCompletions(String treasureId) {
        List<String> completions = new ArrayList<>();
        String sql = "SELECT player_uuid FROM treasure_completed WHERE treasure_id = ? ORDER BY completed_at DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, treasureId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    completions.add(rs.getString("player_uuid"));
                }
            }

            return completions;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get treasure completions: " + e.getMessage());
            e.printStackTrace();
            return completions;
        }
    }

    /**
     * Checks if a treasure exists by ID.
     *
     * @param treasureId the ID of the treasure
     * @return true if the treasure exists, false otherwise
     */
    public boolean treasureExists(String treasureId) {
        String sql = "SELECT 1 FROM treasures WHERE id = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, treasureId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check treasure existence: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks if a treasure exists at a specific location.
     * Uses block coordinates only.
     *
     * @param location the location to check
     * @return true if a treasure exists at this location, false otherwise
     */
    public boolean treasureExistsAtLocation(Location location) {
        String sql = "SELECT 1 FROM treasures WHERE world = ? AND x = ? AND y = ? AND z = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, location.getWorld().getName());
            stmt.setInt(2, location.getBlockX());
            stmt.setInt(3, location.getBlockY());
            stmt.setInt(4, location.getBlockZ());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check location: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Finds a treasure at a specific location.
     * Uses block coordinates only.
     *
     * @param location the location to check
     * @return the treasure at the location, or null if not found
     */
    public Treasure getTreasureAtLocation(Location location) {
        String sql = "SELECT id, command, world, x, y, z, created_at FROM treasures WHERE world = ? AND x = ? AND y = ? AND z = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, location.getWorld().getName());
            stmt.setInt(2, location.getBlockX());
            stmt.setInt(3, location.getBlockY());
            stmt.setInt(4, location.getBlockZ());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String id = rs.getString("id");
                    String command = rs.getString("command");
                    String world = rs.getString("world");
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

                    Location treasureLocation = new Location(
                            org.bukkit.Bukkit.getWorld(world),
                            x + 0.5,
                            y,
                            z + 0.5
                    );

                    return new Treasure(id, command, treasureLocation, createdAt);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get treasure at location: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}
