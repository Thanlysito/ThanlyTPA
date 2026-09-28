package dev.thanly.tpa;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;

/** Escucha lo que pasa en el juego: clics en los menus, moverse, recibir dano, desconectarse. */
public final class TpaListener implements Listener {

    private final ThanlyTPA plugin;

    public TpaListener(ThanlyTPA plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof MenuHolder menu)) {
            return;
        }
        // Nadie puede sacar ni meter objetos en los menus.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return; // clic en el inventario del jugador, no en el menu
        }
        ClickType click = event.getClick();
        // Se hace en el siguiente tick: cerrar o abrir inventarios dentro del evento da problemas.
        Bukkit.getScheduler().runTask(plugin, () -> plugin.menus().handleClick(player, menu, slot, click));
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.settings().cancelOnMove() || !plugin.teleports().isWaiting(player.getUniqueId())) {
            return;
        }
        if (plugin.teleports().movedBlock(player, event.getTo())) {
            plugin.teleports().cancel(player, "teleport-cancel-move");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (plugin.settings().cancelOnDamage() && plugin.teleports().isWaiting(player.getUniqueId())) {
            plugin.teleports().cancel(player, "teleport-cancel-damage");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.teleports().playerLeft(player);
        plugin.requests().clear(player.getUniqueId());
    }
}
