package com.premierstudios.treasurehunt.treasure;

import org.bukkit.Location;
import java.time.LocalDateTime;

/**
 * Represents a treasure object stored in the database.
 * Contains the unique ID, command to execute, location, and creation timestamp.
 */
public class Treasure {

    private final String id;
    private final String command;
    private final Location location;
    private final LocalDateTime createdAt;

    /**
     * Creates a new Treasure instance.
     *
     * @param id the unique identifier for this treasure
     * @param command the command to execute when treasure is found
     * @param location the location of the treasure block
     * @param createdAt the timestamp when the treasure was created
     */
    public Treasure(String id, String command, Location location, LocalDateTime createdAt) {
        this.id = id;
        this.command = command;
        this.location = location;
        this.createdAt = createdAt;
    }

    /**
     * Gets the unique identifier of this treasure.
     *
     * @return the treasure ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the command to execute when this treasure is found.
     *
     * @return the command string
     */
    public String getCommand() {
        return command;
    }

    /**
     * Gets the location of this treasure.
     *
     * @return the treasure location
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the timestamp when this treasure was created.
     *
     * @return the creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Checks if a given location matches this treasure's location.
     * Comparison is done at the block level using block coordinates.
     *
     * @param location the location to check
     * @return true if the location's block matches this treasure's block, false otherwise
     */
    public boolean isAtLocation(Location location) {
        return LocationSerializer.isSameBlock(this.location, location);
    }

    @Override
    public String toString() {
        return "Treasure{" +
                "id='" + id + '\'' +
                ", command='" + command + '\'' +
                ", location=" + location +
                ", createdAt=" + createdAt +
                '}';
    }
}
