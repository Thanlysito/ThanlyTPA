package dev.thanly.tpa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Guarda las solicitudes pendientes y hace enviar / aceptar / rechazar. */
public final class RequestManager {

    private final ThanlyTPA plugin;
    /** destino -> (quien envia -> solicitud). Un jugador solo tiene una solicitud por cada otro jugador. */
    private final Map<UUID, LinkedHashMap<UUID, TpaRequest>> byTarget = new HashMap<>();
    /** ultimo envio de cada jugador, para el tiempo de espera entre solicitudes */
    private final Map<UUID, Long> lastSent = new HashMap<>();

    public RequestManager(ThanlyTPA plugin) {
        this.plugin = plugin;
    }

    public void send(Player sender, Player target, TpaRequest.Type type) {
        Messages msg = plugin.messages();
        Settings cfg = plugin.settings();
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            msg.send(sender, "cannot-self");
            return;
        }
        long now = System.currentTimeMillis();
        if (!sender.hasPermission("thanlytpa.bypass.cooldown")) {
            Long last = lastSent.get(sender.getUniqueId());
            long waitMs = cfg.requestCooldown() * 1000L;
            if (last != null && now - last < waitMs) {
                long left = (waitMs - (now - last) + 999) / 1000;
                msg.send(sender, "cooldown", Messages.seconds(left));
                return;
            }
        }
        lastSent.put(sender.getUniqueId(), now);

        TpaRequest request = new TpaRequest(sender.getUniqueId(), target.getUniqueId(), type,
                now + cfg.requestTimeout() * 1000L);
        byTarget.computeIfAbsent(target.getUniqueId(), k -> new LinkedHashMap<>())
                .put(sender.getUniqueId(), request);

        boolean there = type == TpaRequest.Type.THERE;
        msg.send(sender, there ? "request-sent-there" : "request-sent-here",
                Messages.player(target.getName()), Messages.seconds(cfg.requestTimeout()));
        msg.send(target, there ? "request-received-there" : "request-received-here",
                Messages.player(sender.getName()));
        target.sendMessage(buttons(sender.getName()));
        if (cfg.sounds()) {
            target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.4f);
        }
    }

    /** "[ACEPTAR] [RECHAZAR]" que se pueden clicar en el chat. */
    private Component buttons(String senderName) {
        Messages msg = plugin.messages();
        Component accept = msg.get("button-accept")
                .clickEvent(ClickEvent.runCommand("/tpaccept " + senderName))
                .hoverEvent(HoverEvent.showText(msg.get("button-accept-hover")));
        Component deny = msg.get("button-deny")
                .clickEvent(ClickEvent.runCommand("/tpadeny " + senderName))
                .hoverEvent(HoverEvent.showText(msg.get("button-deny-hover")));
        return Component.text("   ").append(accept).append(Component.text("  ")).append(deny);
    }

    /** Solicitudes que le han enviado a este jugador (las vencidas no cuentan). */
    public List<TpaRequest> incoming(UUID target) {
        LinkedHashMap<UUID, TpaRequest> map = byTarget.get(target);
        List<TpaRequest> list = new ArrayList<>();
        if (map == null) {
            return list;
        }
        long now = System.currentTimeMillis();
        for (TpaRequest r : map.values()) {
            if (!r.expired(now)) {
                list.add(r);
            }
        }
        return list;
    }

    public TpaRequest find(UUID target, UUID sender) {
        LinkedHashMap<UUID, TpaRequest> map = byTarget.get(target);
        if (map == null) {
            return null;
        }
        TpaRequest r = map.get(sender);
        return r == null || r.expired(System.currentTimeMillis()) ? null : r;
    }

    private void remove(TpaRequest r) {
        LinkedHashMap<UUID, TpaRequest> map = byTarget.get(r.target());
        if (map != null) {
            map.remove(r.sender());
            if (map.isEmpty()) {
                byTarget.remove(r.target());
            }
        }
    }

    public void accept(Player target, TpaRequest r) {
        remove(r);
        Player sender = Bukkit.getPlayer(r.sender());
        Messages msg = plugin.messages();
        if (sender == null) {
            msg.send(target, "player-not-found", Messages.player(nameOf(r.sender())));
            return;
        }
        msg.send(target, "accepted-target", Messages.player(sender.getName()));
        msg.send(sender, "accepted-sender", Messages.player(target.getName()));
        // /tpa: el que envio viaja. /tpahere: el que acepta viaja.
        if (r.type() == TpaRequest.Type.THERE) {
            plugin.teleports().start(sender, target);
        } else {
            plugin.teleports().start(target, sender);
        }
    }

    public void deny(Player target, TpaRequest r) {
        remove(r);
        Messages msg = plugin.messages();
        msg.send(target, "denied-target", Messages.player(nameOf(r.sender())));
        Player sender = Bukkit.getPlayer(r.sender());
        if (sender != null) {
            msg.send(sender, "denied-sender", Messages.player(target.getName()));
        }
    }

    /** Cuando un jugador se va, se borran sus solicitudes (enviadas y recibidas). */
    public void clear(UUID player) {
        byTarget.remove(player);
        for (LinkedHashMap<UUID, TpaRequest> map : byTarget.values()) {
            map.remove(player);
        }
        byTarget.values().removeIf(Map::isEmpty);
        lastSent.remove(player);
    }

    /** Cada segundo revisa las solicitudes vencidas y avisa a los dos jugadores. */
    public void startExpiryTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            Messages msg = plugin.messages();
            Iterator<LinkedHashMap<UUID, TpaRequest>> outer = byTarget.values().iterator();
            while (outer.hasNext()) {
                LinkedHashMap<UUID, TpaRequest> map = outer.next();
                Iterator<TpaRequest> inner = map.values().iterator();
                while (inner.hasNext()) {
                    TpaRequest r = inner.next();
                    if (!r.expired(now)) {
                        continue;
                    }
                    inner.remove();
                    Player sender = Bukkit.getPlayer(r.sender());
                    Player target = Bukkit.getPlayer(r.target());
                    if (sender != null) {
                        msg.send(sender, "expired-sender", Messages.player(nameOf(r.target())));
                    }
                    if (target != null) {
                        msg.send(target, "expired-target", Messages.player(nameOf(r.sender())));
                    }
                }
                if (map.isEmpty()) {
                    outer.remove();
                }
            }
        }, 20L, 20L);
    }

    public static String nameOf(UUID id) {
        Player online = Bukkit.getPlayer(id);
        if (online != null) {
            return online.getName();
        }
        String name = Bukkit.getOfflinePlayer(id).getName();
        return name != null ? name : "?";
    }
}
