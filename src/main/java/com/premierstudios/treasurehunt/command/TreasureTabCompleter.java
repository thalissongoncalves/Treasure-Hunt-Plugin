package com.premierstudios.treasurehunt.command;

import com.premierstudios.treasurehunt.treasure.Treasure;
import com.premierstudios.treasurehunt.treasure.TreasureManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides tab completion for TreasureHunt commands.
 * Suggests subcommands and treasure IDs where applicable.
 */
public class TreasureTabCompleter implements TabCompleter {

    private final TreasureManager treasureManager;

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "create",
            "delete",
            "completed",
            "list",
            "gui",
            "reload"
    );

    /**
     * Creates a new TreasureTabCompleter.
     *
     * @param treasureManager the treasure manager
     */
    public TreasureTabCompleter(TreasureManager treasureManager) {
        this.treasureManager = treasureManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // No arguments - suggest subcommands
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(partial))
                    .collect(Collectors.toList());
        }

        // Second argument depends on the subcommand
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String partial = args[1].toLowerCase();

            // For delete and completed, suggest treasure IDs
            if (subCommand.equals("delete") || subCommand.equals("completed")) {
                List<Treasure> treasures = treasureManager.getAllTreasures();
                return treasures.stream()
                        .map(Treasure::getId)
                        .filter(id -> id.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}
