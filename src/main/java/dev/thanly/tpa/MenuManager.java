package dev.thanly.tpa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Los dos menus tipo cofre: elegir jugador, y ver / responder solicitudes. */
public final class MenuManager {

    private static final int MAX_HEADS = 45; // 5 filas de cabezas + 1 fila para el boton de cerrar

    private final ThanlyTPA plugin;

    public MenuManager(ThanlyTPA plugin) {
        this.plugin = plugin;
    }

    /** Menu con las cabezas de los jugadores conectados. Clic = enviar solicitud. */
    public void openPlayers(Player viewer, TpaRequest.Type mode) {
        Messages msg = plugin.messages();
        List<Player> others = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getUniqueId().equals(viewer.getUniqueId()) && viewer.canSee(p)) {
                others.add(p);
            }
        }
        others.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        MenuHolder holder = new MenuHolder(MenuHolder.Kind.PLAYERS, mode);
        Component title = msg.get(mode == TpaRequest.Type.THERE ? "menu-title-players-there" : "menu-title-players-here");
        Inventory inv = Bukkit.createInventory(holder, sizeFor(others.size()), title);
        holder.setInventory(inv);

        if (others.isEmpty()) {
            inv.setItem(4, item(Material.BARRIER, msg.get("menu-no-players"), List.of()));
        }
        int slot = 0;
        for (Player p : others) {
            if (slot >= MAX_HEADS) {
                break;
            }
            inv.setItem(slot, head(p.getUniqueId(), Component.text(p.getName(), NamedTextColor.AQUA),
                    List.of(msg.get("menu-player-lore"))));
            holder.put(slot, p.getUniqueId());
            slot++;
        }
        addClose(inv, holder);
        viewer.openInventory(inv);
    }

    /** Menu con las solicitudes recibidas. Clic izquierdo = aceptar, derecho = rechazar. */
    public void openRequests(Player viewer) {
        Messages msg = plugin.messages();
        List<TpaRequest> list = plugin.requests().incoming(viewer.getUniqueId());
        if (list.isEmpty()) {
            msg.send(viewer, "no-requests");
            viewer.closeInventory();
            return;
        }
        MenuHolder holder = new MenuHolder(MenuHolder.Kind.REQUESTS, null);
        Inventory inv = Bukkit.createInventory(holder, sizeFor(list.size()), msg.get("menu-title-requests"));
        holder.setInventory(inv);

        long now = System.currentTimeMillis();
        int slot = 0;
        for (TpaRequest r : list) {
            if (slot >= MAX_HEADS) {
                break;
            }
            List<Component> lore = List.of(
                    msg.get(r.type() == TpaRequest.Type.THERE ? "menu-request-there" : "menu-request-here"),
                    msg.get("menu-request-expires", Messages.seconds(r.secondsLeft(now))),
                    Component.empty(),
                    msg.get("menu-left-accept"),
                    msg.get("menu-right-deny"));
            inv.setItem(slot, head(r.sender(), Component.text(RequestManager.nameOf(r.sender()), NamedTextColor.AQUA), lore));
            holder.put(slot, r.sender());
            slot++;
        }
        addClose(inv, holder);
        viewer.openInventory(inv);
    }

    /** Lo que pasa al hacer clic dentro de un menu del plugin. */
    public void handleClick(Player viewer, MenuHolder holder, int slot, ClickType click) {
        if (slot == holder.closeSlot()) {
            viewer.closeInventory();
            return;
        }
        UUID target = holder.playerAt(slot);
        if (target == null) {
            return;
        }
        if (holder.kind() == MenuHolder.Kind.PLAYERS) {
            viewer.closeInventory();
            Player other = Bukkit.getPlayer(target);
            if (other == null) {
                plugin.messages().send(viewer, "player-not-found", Messages.player(RequestManager.nameOf(target)));
                return;
            }
            plugin.requests().send(viewer, other, holder.mode());
            return;
        }
        // Menu de solicitudes
        TpaRequest r = plugin.requests().find(viewer.getUniqueId(), target);
        if (r == null) {
            plugin.messages().send(viewer, "request-not-found", Messages.player(RequestManager.nameOf(target)));
        } else if (click.isRightClick()) {
            plugin.requests().deny(viewer, r);
        } else {
            plugin.requests().accept(viewer, r);
        }
        // Si quedan mas solicitudes, se vuelve a abrir el menu actualizado.
        if (plugin.requests().incoming(viewer.getUniqueId()).isEmpty()) {
            viewer.closeInventory();
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> openRequests(viewer));
        }
    }

    private static int sizeFor(int heads) {
        int rows = Math.min(5, Math.max(1, (heads + 8) / 9));
        return (rows + 1) * 9; // + la fila de abajo con el boton de cerrar
    }

    private void addClose(Inventory inv, MenuHolder holder) {
        int slot = inv.getSize() - 5; // centro de la ultima fila
        inv.setItem(slot, item(Material.BARRIER, plugin.messages().get("menu-close"), List.of()));
        holder.setCloseSlot(slot);
    }

    private static ItemStack head(UUID owner, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
        applyText(meta, name, lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack item(Material material, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        applyText(meta, name, lore);
        stack.setItemMeta(meta);
        return stack;
    }

    /** Minecraft pone los nombres de objetos en cursiva; aqui se quita. */
    private static void applyText(ItemMeta meta, Component name, List<Component> lore) {
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        List<Component> clean = new ArrayList<>();
        for (Component line : lore) {
            clean.add(line.decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(clean);
    }
}
