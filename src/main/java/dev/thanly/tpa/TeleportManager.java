package dev.thanly.tpa;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Cuenta regresiva antes de teletransportar, y cancelarla si el jugador se mueve o recibe dano. */
public final class TeleportManager {

    private final ThanlyTPA plugin;
    private final Map<UUID, Pending> pending = new HashMap<>();

    public TeleportManager(ThanlyTPA plugin) {
        this.plugin = plugin;
    }

    /** Un viaje en espera: quien viaja, hacia quien, y en que bloque estaba al empezar. */
    private static final class Pending {
        final UUID destination;
        final Location startBlock;
        BukkitTask task;
        int secondsLeft;

        Pending(UUID destination, Location startBlock, int secondsLeft) {
            this.destination = destination;
            this.startBlock = startBlock;
            this.secondsLeft = secondsLeft;
        }
    }

    public void start(Player traveler, Player destination) {
        cancelSilently(traveler.getUniqueId());
        Settings cfg = plugin.settings();
        int delay = cfg.teleportDelay();
        if (delay <= 0 || traveler.hasPermission("thanlytpa.bypass.delay")) {
            teleport(traveler, destination);
            return;
        }

        Pending p = new Pending(destination.getUniqueId(), traveler.getLocation().toBlockLocation(), delay);
        pending.put(traveler.getUniqueId(), p);
        plugin.messages().send(traveler, "teleport-start", Messages.seconds(delay));

        p.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player t = Bukkit.getPlayer(traveler.getUniqueId());
            Player d = Bukkit.getPlayer(p.destination);
            if (t == null) {
                cancelSilently(traveler.getUniqueId());
                return;
            }
            if (d == null) {
                cancel(t, "teleport-cancel-offline", Messages.player(RequestManager.nameOf(p.destination)));
                return;
            }
            if (p.secondsLeft <= 0) {
                cancelSilently(t.getUniqueId());
                teleport(t, d);
                return;
            }
            t.sendActionBar(plugin.messages().get("teleport-countdown", Messages.seconds(p.secondsLeft)));
            if (cfg.sounds()) {
                t.playSound(t.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.2f);
            }
            p.secondsLeft--;
        }, 0L, 20L);
    }

    private void teleport(Player traveler, Player destination) {
        traveler.teleportAsync(destination.getLocation()).thenAccept(ok -> {
            if (!ok) {
                return;
            }
            plugin.messages().send(traveler, "teleported", Messages.player(destination.getName()));
            if (plugin.settings().sounds()) {
                traveler.playSound(traveler.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            }
        });
    }

    public boolean isWaiting(UUID player) {
        return pending.containsKey(player);
    }

    /** true si el jugador salio del bloque donde empezo la cuenta regresiva. */
    public boolean movedBlock(Player player, Location to) {
        Pending p = pending.get(player.getUniqueId());
        if (p == null || to == null) {
            return false;
        }
        Location s = p.startBlock;
        return s.getWorld() != to.getWorld()
                || s.getBlockX() != to.getBlockX()
                || s.getBlockY() != to.getBlockY()
                || s.getBlockZ() != to.getBlockZ();
    }

    public void cancel(Player player, String reasonKey, TagResolver... resolvers) {
        if (cancelSilently(player.getUniqueId())) {
            plugin.messages().send(player, reasonKey, resolvers);
            if (plugin.settings().sounds()) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
            }
        }
    }

    private boolean cancelSilently(UUID player) {
        Pending p = pending.remove(player);
        if (p == null) {
            return false;
        }
        if (p.task != null) {
            p.task.cancel();
        }
        return true;
    }

    /** Cuando un jugador se desconecta: se cancela su viaje y los viajes hacia el. */
    public void playerLeft(Player left) {
        cancelSilently(left.getUniqueId());
        for (Map.Entry<UUID, Pending> e : Map.copyOf(pending).entrySet()) {
            if (e.getValue().destination.equals(left.getUniqueId())) {
                Player traveler = Bukkit.getPlayer(e.getKey());
                if (traveler != null) {
                    cancel(traveler, "teleport-cancel-offline", Messages.player(left.getName()));
                } else {
                    cancelSilently(e.getKey());
                }
            }
        }
    }

    public void cancelAll() {
        for (Pending p : pending.values()) {
            if (p.task != null) {
                p.task.cancel();
            }
        }
        pending.clear();
    }
}
