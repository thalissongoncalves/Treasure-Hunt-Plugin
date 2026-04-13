package com.premierstudios.treasurehunt.command;

import com.premierstudios.treasurehunt.config.ConfigManager;
import com.premierstudios.treasurehunt.gui.TreasureGUI;
import com.premierstudios.treasurehunt.model.PendingTreasure;
import com.premierstudios.treasurehunt.treasure.Treasure;
import com.premierstudios.treasurehunt.treasure.TreasureManager;
import com.premierstudios.treasurehunt.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles all /treasure subcommands: create, delete, completed, list, gui, reload.
 */
public class TreasureCommand implements CommandExecutor {

    private final TreasureManager treasureManager;
    private final ConfigManager configManager;
    private final TreasureGUI treasureGUI;
    private final Map<UUID, PendingTreasure> pendingTreasures;

    public TreasureCommand(TreasureManager treasureManager, ConfigManager configManager,
                           TreasureGUI treasureGUI, Map<UUID, PendingTreasure> pendingTreasures) {
        this.treasureManager = treasureManager;
        this.configManager = configManager;
        this.treasureGUI = treasureGUI;
        this.pendingTreasures = pendingTreasures;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("treasurehunt.admin") && !player.isOp()) {
            MessageUtil.sendMessage(player, configManager.getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            return handleGui(player);
        }

        switch (args[0].toLowerCase()) {
            case "create":    return handleCreate(player, args);
            case "delete":    return handleDelete(player, args);
            case "completed": return handleCompleted(player, args);
            case "list":      return handleList(player);
            case "gui":       return handleGui(player);
            case "reload":    return handleReload(player);
            default:
                sendHelpMessage(player);
                return true;
        }
    }

    // -------------------------------------------------------------------------
    // Subcommand handlers
    // -------------------------------------------------------------------------

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendMessage(player, configManager.getMessage("usage-create"));
            return true;
        }

        String id = args[1];

        if (!id.matches("[a-zA-Z0-9_-]+")) {
            MessageUtil.sendMessage(player, configManager.getMessage("invalid-treasure-id"));
            return true;
        }

        if (treasureManager.treasureExists(id)) {
            MessageUtil.sendMessage(player, configManager.getMessage("duplicate-id", "id", id));
            return true;
        }

        StringBuilder cmd = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) cmd.append(' ');
            cmd.append(args[i]);
        }

        if (cmd.toString().trim().isEmpty()) {
            MessageUtil.sendMessage(player, configManager.getMessage("usage-create"));
            return true;
        }

        pendingTreasures.put(player.getUniqueId(), new PendingTreasure(id, cmd.toString()));
        MessageUtil.sendMessage(player, configManager.getMessage("waiting-for-block"));
        return true;
    }

    private boolean handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, configManager.getMessage("usage-delete"));
            return true;
        }

        String id = args[1];

        if (!treasureManager.treasureExists(id)) {
            MessageUtil.sendMessage(player, configManager.getMessage("treasure-not-found", "id", id));
            return true;
        }

        if (treasureManager.deleteTreasure(id)) {
            MessageUtil.sendMessage(player, configManager.getMessage("treasure-deleted"));
        } else {
            MessageUtil.sendMessage(player, configManager.getMessage("delete-error"));
        }
        return true;
    }

    private boolean handleCompleted(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, configManager.getMessage("usage-completed"));
            return true;
        }

        String id = args[1];

        if (!treasureManager.treasureExists(id)) {
            MessageUtil.sendMessage(player, configManager.getMessage("treasure-not-found", "id", id));
            return true;
        }

        List<String> completions = treasureManager.getTreasureCompletions(id);

        if (completions.isEmpty()) {
            MessageUtil.sendMessage(player, configManager.getMessage("no-completions"));
            return true;
        }

        sendLine(player);
        MessageUtil.sendMessage(player, configManager.getMessage("header-completed", "count", completions.size()));
        sendLine(player);

        for (String uuid : completions) {
            try {
                org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                String name = op.getName();
                if (name != null && !name.isEmpty()) {
                    MessageUtil.sendMessage(player, configManager.getMessage("completed-player", "player", name));
                } else {
                    MessageUtil.sendMessage(player, configManager.getMessage("completed-unknown", "uuid", uuid.substring(0, 8)));
                }
            } catch (IllegalArgumentException e) {
                MessageUtil.sendMessage(player, configManager.getMessage("completed-invalid"));
            }
        }

        sendLine(player);
        return true;
    }

    private boolean handleList(Player player) {
        List<Treasure> treasures = treasureManager.getAllTreasures();

        if (treasures.isEmpty()) {
            MessageUtil.sendMessage(player, configManager.getMessage("no-treasures"));
            return true;
        }

        sendLine(player);
        MessageUtil.sendMessage(player, configManager.getMessage("header-list", "count", treasures.size()));
        sendLine(player);

        for (Treasure t : treasures) {
            String loc = t.getLocation().getWorld().getName()
                    + " " + t.getLocation().getBlockX()
                    + ", " + t.getLocation().getBlockY()
                    + ", " + t.getLocation().getBlockZ();
            MessageUtil.sendMessage(player, configManager.getMessage("list-item", "id", t.getId(), "location", loc));
        }

        sendLine(player);
        return true;
    }

    private boolean handleGui(Player player) {
        treasureGUI.openInventory(player, 1);
        return true;
    }

    private boolean handleReload(Player player) {
        treasureManager.reload();
        MessageUtil.sendMessage(player, configManager.getMessage("reload-completed"));
        return true;
    }

    // -------------------------------------------------------------------------
    // Help display
    // -------------------------------------------------------------------------

    private void sendHelpMessage(Player player) {
        sendLine(player);
        MessageUtil.sendMessage(player, configManager.getMessage("header-help"));
        sendLine(player);
        MessageUtil.sendMessage(player, configManager.getMessage("help-create"));
        MessageUtil.sendMessage(player, configManager.getMessage("help-delete"));
        MessageUtil.sendMessage(player, configManager.getMessage("help-completed"));
        MessageUtil.sendMessage(player, configManager.getMessage("help-list"));
        MessageUtil.sendMessage(player, configManager.getMessage("help-gui"));
        MessageUtil.sendMessage(player, configManager.getMessage("help-reload"));
        sendLine(player);
    }

    private void sendLine(Player player) {
        MessageUtil.sendMessage(player, configManager.getMessage("header-line"));
    }
}
