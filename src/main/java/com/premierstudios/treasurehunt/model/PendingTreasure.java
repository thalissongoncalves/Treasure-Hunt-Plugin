package com.premierstudios.treasurehunt.model;

/**
 * Represents a treasure that is pending placement.
 * Stores the ID and command during the treasure creation process.
 */
public class PendingTreasure {

    private final String treasureId;
    private final String command;

    /**
     * Creates a new PendingTreasure instance.
     *
     * @param treasureId the unique ID for the treasure
     * @param command the command to execute when found
     */
    public PendingTreasure(String treasureId, String command) {
        this.treasureId = treasureId;
        this.command = command;
    }

    /**
     * Gets the treasure ID.
     *
     * @return the treasure ID
     */
    public String getTreasureId() {
        return treasureId;
    }

    /**
     * Gets the command to execute.
     *
     * @return the command
     */
    public String getCommand() {
        return command;
    }
}
