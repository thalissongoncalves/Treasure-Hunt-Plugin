package com.premierstudios.treasurehunt.gui;

import com.premierstudios.treasurehunt.config.ConfigManager;
import com.premierstudios.treasurehunt.treasure.Treasure;
import com.premierstudios.treasurehunt.treasure.TreasureManager;
import com.premierstudios.treasurehunt.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TreasureGUI implements Listener {

    private static final int PAGE_SIZE = 45;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a");

    private enum GuiType { LIST, SEARCH_RESULTS, CONFIRM }

    private final Map<UUID, GuiType> openGui = new HashMap<>();
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<UUID, String> pendingDelete = new HashMap<>();
    // Prevents InventoryCloseEvent from clearing state during screen transitions
    private final Set<UUID> transitioning = new HashSet<>();
    private final Set<UUID> awaitingSearch = new HashSet<>();

    private final JavaPlugin plugin;
    private final TreasureManager manager;
    private final ConfigManager config;

    public TreasureGUI(JavaPlugin plugin, TreasureManager manager, ConfigManager config) {
        this.plugin = plugin;
        this.manager = manager;
        this.config = config;
    }

    public void openInventory(Player p, int page) {
        List<Treasure> list = manager.getAllTreasuresSorted();
        if (list.isEmpty()) {
            MessageUtil.sendMessage(p, config.getMessage("no-treasures"));
            return;
        }

        int total = (int) Math.ceil((double) list.size() / PAGE_SIZE);
        page = Math.max(1, Math.min(page, total));

        playerPages.put(p.getUniqueId(), page);
        pendingDelete.remove(p.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 54, "§6Treasures (Page " + page + "/" + total + ")");

        int start = (page - 1) * PAGE_SIZE;
        for (int i = start; i < Math.min(start + PAGE_SIZE, list.size()); i++)
            inv.addItem(buildChestItem(list.get(i)));

        if (page > 1) inv.setItem(45, btn(Material.ARROW, "§e« Previous"));
        inv.setItem(47, btn(Material.COMPASS, "§e🔎 Search by ID"));
        inv.setItem(49, btn(Material.PAPER, "§7Page " + page + " / " + total));
        if (page < total) inv.setItem(53, btn(Material.ARROW, "§eNext »"));

        openSafe(p, GuiType.LIST, inv);
    }

    public void openSearchResults(Player p, String query) {
        List<Treasure> results = manager.searchTreasuresByID(query);
        if (results.isEmpty()) {
            MessageUtil.sendMessage(p, config.getMessage("no-treasures"));
            openInventory(p, 1);
            return;
        }

        playerPages.put(p.getUniqueId(), 1);
        pendingDelete.remove(p.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 54,
                "§6Results: §e" + query + " §8(" + results.size() + ")");

        for (int i = 0; i < Math.min(PAGE_SIZE, results.size()); i++)
            inv.addItem(buildChestItem(results.get(i)));

        inv.setItem(45, btn(Material.ARROW, "§e« Back to List"));
        inv.setItem(47, btn(Material.COMPASS, "§e🔎 Search Again"));

        openSafe(p, GuiType.SEARCH_RESULTS, inv);
    }

    private void openConfirm(Player p, String id) {
        pendingDelete.put(p.getUniqueId(), id);

        String prefix = config.getTranslatedRawMessage("delete-confirmation-title");
        String title = "§c" + prefix + " §6" + id;
        if (title.length() > 56) title = title.substring(0, 53) + "...";

        Inventory inv = Bukkit.createInventory(null, 27, title);
        for (int i = 0; i < 27; i++) inv.setItem(i, btn(Material.GRAY_STAINED_GLASS_PANE, " "));

        ItemStack yes = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta m = yes.getItemMeta();
        m.setDisplayName(config.getTranslatedRawMessage("delete-confirmation-yes"));
        m.setLore(List.of("§7This action cannot be undone."));
        yes.setItemMeta(m);
        inv.setItem(10, yes);

        ItemStack info = new ItemStack(Material.YELLOW_CONCRETE);
        m = info.getItemMeta();
        m.setDisplayName("§eTreasure: §6" + id);
        m.setLore(List.of("§7Are you sure you want to delete this?"));
        info.setItemMeta(m);
        inv.setItem(13, info);

        ItemStack no = new ItemStack(Material.RED_CONCRETE);
        m = no.getItemMeta();
        m.setDisplayName(config.getTranslatedRawMessage("delete-confirmation-no"));
        m.setLore(List.of("§7Click to go back."));
        no.setItemMeta(m);
        inv.setItem(16, no);

        openSafe(p, GuiType.CONFIRM, inv);
    }

    private void openSafe(Player p, GuiType type, Inventory inv) {
        UUID uuid = p.getUniqueId();
        transitioning.add(uuid);
        openGui.put(uuid, type);
        p.openInventory(inv);
        transitioning.remove(uuid);
    }

    private void startSearch(Player p) {
        UUID uuid = p.getUniqueId();
        transitioning.add(uuid);
        p.closeInventory();
        transitioning.remove(uuid);
        openGui.remove(uuid);
        pendingDelete.remove(uuid);
        awaitingSearch.add(uuid);
        p.sendMessage("§e[TreasureHunt] §7Type the treasure ID to search, or §ccancel §7to go back:");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        GuiType type = openGui.get(p.getUniqueId());
        if (type == null) return;

        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;

        switch (type) {
            case LIST:           handleList(e, p);    break;
            case SEARCH_RESULTS: handleSearch(e, p);  break;
            case CONFIRM:        handleConfirm(e, p); break;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        UUID uuid = e.getPlayer().getUniqueId();
        if (transitioning.contains(uuid)) return;
        openGui.remove(uuid);
        pendingDelete.remove(uuid);
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (!awaitingSearch.contains(uuid)) return;

        e.setCancelled(true);
        awaitingSearch.remove(uuid);

        String query = e.getMessage().trim();
        Player p = e.getPlayer();

        // Must run on the main thread — Bukkit API is not thread-safe
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (query.equalsIgnoreCase("cancel"))
                openInventory(p, playerPages.getOrDefault(uuid, 1));
            else
                openSearchResults(p, query);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        awaitingSearch.remove(uuid);
        openGui.remove(uuid);
        pendingDelete.remove(uuid);
        playerPages.remove(uuid);
    }

    private void handleList(InventoryClickEvent e, Player p) {
        int slot = e.getSlot();
        UUID uuid = p.getUniqueId();
        int page = playerPages.getOrDefault(uuid, 1);

        if (slot == 45 && page > 1) { openInventory(p, page - 1); return; }
        if (slot == 47) { startSearch(p); return; }
        if (slot == 49) return;
        if (slot == 53) {
            int total = (int) Math.ceil((double) manager.getTreasureCount() / PAGE_SIZE);
            if (page < total) openInventory(p, page + 1);
            return;
        }

        ItemStack item = e.getCurrentItem();
        if (item.getType() != Material.CHEST || !item.hasItemMeta()) return;
        String id = item.getItemMeta().getDisplayName().replace("§6", "");
        openConfirm(p, id);
    }

    private void handleSearch(InventoryClickEvent e, Player p) {
        int slot = e.getSlot();
        if (slot == 45) { openInventory(p, 1); return; }
        if (slot == 47) { startSearch(p); return; }

        ItemStack item = e.getCurrentItem();
        if (item.getType() != Material.CHEST || !item.hasItemMeta()) return;
        String id = item.getItemMeta().getDisplayName().replace("§6", "");
        openConfirm(p, id);
    }

    private void handleConfirm(InventoryClickEvent e, Player p) {
        UUID uuid = p.getUniqueId();
        String id = pendingDelete.get(uuid);
        if (id == null) return;

        if (e.getSlot() == 10) {
            if (manager.deleteTreasure(id)) {
                MessageUtil.sendMessage(p, config.getMessage("delete-confirmed"));
                plugin.getLogger().info("Treasure '" + id + "' deleted by " + p.getName());
            } else {
                MessageUtil.sendMessage(p, config.getMessage("delete-error"));
            }
            pendingDelete.remove(uuid);
            openInventory(p, playerPages.getOrDefault(uuid, 1));
        } else if (e.getSlot() == 16) {
            MessageUtil.sendMessage(p, config.getMessage("delete-cancelled"));
            pendingDelete.remove(uuid);
            openInventory(p, playerPages.getOrDefault(uuid, 1));
        }
    }

    private ItemStack buildChestItem(Treasure t) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6" + t.getId());
        String world = t.getLocation().getWorld() != null ? t.getLocation().getWorld().getName() : "Unknown";
        String created = t.getCreatedAt() != null ? t.getCreatedAt().format(DATE_FMT) : "Unknown";
        meta.setLore(List.of(
                "§7World:    §a" + world,
                "§7Position: §a" + t.getLocation().getBlockX() + "§7, §a" + t.getLocation().getBlockY() + "§7, §a" + t.getLocation().getBlockZ(),
                "§7Command:  §a" + t.getCommand(),
                "§7Created:  §a" + created,
                "",
                "§c> Click to delete"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack btn(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}
