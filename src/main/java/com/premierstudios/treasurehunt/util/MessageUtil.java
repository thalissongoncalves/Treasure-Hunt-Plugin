package com.premierstudios.treasurehunt.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Utility class for sending colored messages to players and command senders.
 * Handles color code translation (&-codes) for consistent messaging.
 */
public class MessageUtil {

    private MessageUtil() {
        // Utility class, no instantiation
    }

    /**
     * Sends a colored message to a player.
     * Translates & color codes.
     *
     * @param player the player to send the message to
     * @param message the message with & color codes
     */
    public static void sendMessage(Player player, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        player.sendMessage(coloredMessage);
    }

    /**
     * Sends a colored message to a command sender (console or player).
     * Translates & color codes.
     *
     * @param sender the command sender
     * @param message the message with & color codes
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        sender.sendMessage(coloredMessage);
    }

    /**
     * Broadcasts a colored message to all online players.
     * Translates & color codes.
     *
     * @param message the message to broadcast
     */
    public static void broadcast(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            player.sendMessage(coloredMessage);
        }
    }

    /**
     * Strips all color codes from a message.
     *
     * @param message the message to strip
     * @return the message without color codes
     */
    public static String stripColor(String message) {
        return ChatColor.stripColor(message);
    }
}
