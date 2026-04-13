package com.premierstudios.treasurehunt.treasure;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Utility class for serializing and deserializing Bukkit Location objects.
 * Uses only block coordinates (X, Y, Z) for consistency and accuracy.
 * All comparisons are made at the block level to avoid floating-point issues.
 */
public class LocationSerializer {

    private LocationSerializer() {
        // Utility class, no instantiation
    }

    /**
     * Serializes a Location object into a string format for database storage.
     * Format: "worldName,blockX,blockY,blockZ"
     * Only uses block coordinates to ensure consistency.
     *
     * @param location the location to serialize
     * @return the serialized location string
     */
    public static String serialize(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return String.format(
                "%s,%d,%d,%d",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    /**
     * Deserializes a string into a Location object.
     * Expects format: "worldName,blockX,blockY,blockZ"
     * Returns a Location at the center of the block for visual consistency.
     *
     * @param data the serialized location string
     * @return the deserialized Location object, or null if parsing fails
     */
    public static Location deserialize(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        try {
            String[] parts = data.split(",");
            if (parts.length != 4) {
                return null;
            }

            String worldName = parts[0];
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return null;
            }

            // Return location at block center for visual consistency
            return new Location(world, x + 0.5, y, z + 0.5);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Compares two locations at the block level (ignoring floating-point precision).
     * This is the proper way to check if locations are the same treasure block.
     *
     * @param loc1 the first location
     * @param loc2 the second location
     * @return true if both locations refer to the same block
     */
    public static boolean isSameBlock(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) {
            return false;
        }
        if (loc1.getWorld() == null || loc2.getWorld() == null) {
            return false;
        }
        return loc1.getWorld().getName().equals(loc2.getWorld().getName())
                && loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ();
    }
}
