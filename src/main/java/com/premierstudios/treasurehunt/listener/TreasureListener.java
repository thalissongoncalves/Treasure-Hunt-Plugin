package com.premierstudios.treasurehunt.listener;

import com.premierstudios.treasurehunt.config.ConfigManager;
import com.premierstudios.treasurehunt.model.PendingTreasure;
import com.premierstudios.treasurehunt.treasure.Treasure;
import com.premierstudios.treasurehunt.treasure.TreasureManager;
import com.premierstudios.treasurehunt.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles player interactions with treasure blocks and treasure creation placement.
 * Uses EquipmentSlot.HAND check to prevent Paper's double-fire of PlayerInteractEvent.
 * Includes a 500ms per-player cooldown to prevent click spam on discovery.
 */
public class TreasureListener implements Listener {

    private static final long CLICK_COOLDOWN_MS = 500L;

    private final JavaPlugin plugin;
    private final TreasureManager treasureManager;
    private final ConfigManager configManager;
    private final Map<UUID, PendingTreasure> pendingTreasures;
    private final Map<UUID, Long> lastClickTime = new HashMap<>();

    public TreasureListener(JavaPlugin plugin, TreasureManager treasureManager,
                            ConfigManager configManager, Map<UUID, PendingTreasure> pendingTreasures) {
        this.plugin = plugin;
        this.treasureManager = treasureManager;
        this.configManager = configManager;
        this.pendingTreasures = pendingTreasures;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only process MAIN_HAND to prevent double-fire on Paper
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getClickedBlock() == null) {
            return;
        }

        UUID playerUuid = event.getPlayer().getUniqueId();

        // Creation mode: place the treasure and exit early
        if (pendingTreasures.containsKey(playerUuid)) {
            handleTreasurePlacement(event);
            return;
        }

        // Anti-spam: skip if player clicked too recently
        long now = System.currentTimeMillis();
        Long last = lastClickTime.get(playerUuid);
        if (last != null && now - last < CLICK_COOLDOWN_MS) {
            return;
        }
        lastClickTime.put(playerUuid, now);

        handleTreasureDiscovery(event);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastClickTime.remove(event.getPlayer().getUniqueId());
    }

    // -------------------------------------------------------------------------

    private void handleTreasurePlacement(PlayerInteractEvent event) {
        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        UUID playerUuid = event.getPlayer().getUniqueId();
        PendingTreasure pending = pendingTreasures.get(playerUuid);
        if (pending == null) return;

        if (!event.getClickedBlock().getType().isSolid()) {
            MessageUtil.sendMessage(event.getPlayer(), configManager.getMessage("invalid-block"));
            return;
        }

        boolean created = treasureManager.createTreasure(
                pending.getTreasureId(),
                pending.getCommand(),
                event.getClickedBlock().getLocation()
        );

        if (created) {
            MessageUtil.sendMessage(event.getPlayer(),
                    configManager.getMessage("treasure-created", "id", pending.getTreasureId()));
            plugin.getLogger().info("Treasure '" + pending.getTreasureId()
                    + "' created by " + event.getPlayer().getName());
        } else {
            MessageUtil.sendMessage(event.getPlayer(), configManager.getMessage("already-occupied"));
        }

        pendingTreasures.remove(playerUuid);
    }

    // -------------------------------------------------------------------------

    private void handleTreasureDiscovery(PlayerInteractEvent event) {
        Treasure treasure = treasureManager.getTreasureAtLocation(
                event.getClickedBlock().getLocation());

        if (treasure == null) return;

        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);

        final String playerUuidStr  = event.getPlayer().getUniqueId().toString();
        final String playerName     = event.getPlayer().getName();
        final String treasureId     = treasure.getId();
        final Treasure finalTreasure = treasure;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            if (treasureManager.hasCompletedTreasure(treasureId, playerUuidStr)) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        MessageUtil.sendMessage(event.getPlayer(),
                                configManager.getMessage("already-collected"))
                );
                return;
            }

            if (treasureManager.markTreasureCompleted(treasureId, playerUuidStr)) {
                Bukkit.getScheduler().runTask(plugin, () -> {

                    MessageUtil.sendMessage(event.getPlayer(),
                            configManager.getMessage("treasure-found", "id", treasureId));

                    // Play configured sound
                    if (configManager.getConfig().getBoolean("plugin.enable-treasure-sound", true)) {
                        try {
                            String soundName = configManager.getConfig()
                                    .getString("plugin.treasure-sound", "ENTITY_PLAYER_LEVELUP");
                            Sound sound = Sound.valueOf(soundName);
                            event.getPlayer().playSound(
                                    event.getPlayer().getLocation(), sound, 1.0f, 1.0f);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid sound in config: "
                                    + configManager.getConfig().getString("plugin.treasure-sound"));
                        }
                    }

                    // Spawn celebration particles
                    if (configManager.getConfig().getBoolean("plugin.enable-treasure-particles", true)) {
                        event.getClickedBlock().getWorld().spawnParticle(
                                Particle.HAPPY_VILLAGER,
                                event.getClickedBlock().getLocation().add(0.5, 1, 0.5),
                                15, 0.5, 0.5, 0.5, 0.1
                        );
                    }

                    // Execute the reward command as console
                    String cmd = finalTreasure.getCommand().replace("%player%", playerName);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

                    plugin.getLogger().info(playerName + " found treasure '" + treasureId + "'");
                });
            }
        });
    }
}
