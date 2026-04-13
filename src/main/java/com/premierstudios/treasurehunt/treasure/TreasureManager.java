package com.premierstudios.treasurehunt.treasure;

import com.premierstudios.treasurehunt.database.DatabaseManager;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TreasureManager {

    private final JavaPlugin plugin;
    private final DatabaseManager db;
    // Dual cache: one by ID for fast lookup, one by location for block hit detection
    private final Map<String, Treasure> byId = new HashMap<>();
    private final Map<String, Treasure> byLocation = new HashMap<>();

    public TreasureManager(JavaPlugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    public void reload() {
        byId.clear();
        byLocation.clear();
        for (Treasure t : db.loadAllTreasures()) cache(t);
        plugin.getLogger().info("Loaded " + byId.size() + " treasures.");
    }

    private void cache(Treasure t) {
        byId.put(t.getId(), t);
        byLocation.put(LocationSerializer.serialize(t.getLocation()), t);
    }

    private void uncache(String id) {
        Treasure t = byId.remove(id);
        if (t != null) byLocation.remove(LocationSerializer.serialize(t.getLocation()));
    }

    public boolean createTreasure(String id, String command, Location location) {
        if (byId.containsKey(id)) {
            plugin.getLogger().warning("Treasure ID '" + id + "' already exists.");
            return false;
        }
        if (isTreasureAtLocation(location)) {
            plugin.getLogger().warning("A treasure already exists at this location.");
            return false;
        }

        Treasure t = new Treasure(id, command, location, java.time.LocalDateTime.now());
        if (db.saveTreasure(t)) {
            cache(t);
            return true;
        }
        return false;
    }

    public boolean deleteTreasure(String id) {
        if (!byId.containsKey(id)) {
            plugin.getLogger().warning("Treasure '" + id + "' not found in cache.");
            return false;
        }
        if (db.deleteTreasure(id)) {
            uncache(id);
            return true;
        }
        return false;
    }

    public Treasure getTreasure(String id) {
        return byId.get(id);
    }

    public List<Treasure> getAllTreasures() {
        return new ArrayList<>(byId.values());
    }

    public List<Treasure> getAllTreasuresSorted() {
        List<Treasure> list = new ArrayList<>(byId.values());
        list.sort((a, b) -> {
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return a.getCreatedAt().compareTo(b.getCreatedAt());
        });
        return list;
    }

    public List<Treasure> searchTreasuresByID(String query) {
        String q = query.toLowerCase();
        List<Treasure> results = new ArrayList<>();
        for (Treasure t : byId.values())
            if (t.getId().toLowerCase().contains(q)) results.add(t);
        results.sort((a, b) -> a.getId().compareToIgnoreCase(b.getId()));
        return results;
    }

    public Treasure getTreasureAtLocation(Location location) {
        return byLocation.get(LocationSerializer.serialize(location));
    }

    public boolean isTreasureAtLocation(Location location) {
        return byLocation.containsKey(LocationSerializer.serialize(location));
    }

    public boolean treasureExists(String id) {
        return byId.containsKey(id);
    }

    public int getTreasureCount() {
        return byId.size();
    }

    public boolean hasCompletedTreasure(String treasureId, String playerUuid) {
        return db.isTreasureCompleted(treasureId, playerUuid);
    }

    public boolean markTreasureCompleted(String treasureId, String playerUuid) {
        return db.markTreasureCompleted(treasureId, playerUuid);
    }

    public List<String> getTreasureCompletions(String treasureId) {
        return db.getTreasureCompletions(treasureId);
    }
}
