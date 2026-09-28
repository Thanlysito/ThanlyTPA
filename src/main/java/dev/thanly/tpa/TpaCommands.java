package dev.thanly.tpa;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /tpa, /tpahere, /tpaccept, /tpadeny, /tpamenu y /tpareload. */
public final class TpaCommands implements CommandExecutor, TabCompleter {

    private final ThanlyTPA plugin;

    public TpaCommands(ThanlyTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages msg = plugin.messages();
        String name = command.getName().toLowerCase(Locale.ROOT);

        if (name.equals("tpareload")) {
            plugin.loadSettings();
            msg.send(sender, "reloaded");
            return true;
        }
        if (!(sender instanceof Player player)) {
            msg.send(sender, "players-only");
            return true;
        }

        switch (name) {
            case "tpa" -> sendOrMenu(player, args, TpaRequest.Type.THERE);
            case "tpahere" -> sendOrMenu(player, args, TpaRequest.Type.HERE);
            case "tpaccept" -> answer(player, args, true);
            case "tpadeny" -> answer(player, args, false);
            case "tpamenu" -> plugin.menus().openRequests(player);
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Con nombre: envia la solicitud. Sin nombre: abre el menu de jugadores. */
    private void sendOrMenu(Player player, String[] args, TpaRequest.Type type) {
        if (args.length == 0) {
            plugin.menus().openPlayers(player, type);
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !player.canSee(target)) {
            plugin.messages().send(player, "player-not-found", Messages.player(args[0]));
            return;
        }
        plugin.requests().send(player, target, type);
    }

    /**
     * Con nombre: responde la solicitud de ese jugador.
     * Sin nombre: si hay una sola, la responde; si hay varias, abre el menu.
     */
    private void answer(Player player, String[] args, boolean accept) {
        RequestManager requests = plugin.requests();
        TpaRequest request;
        if (args.length > 0) {
            Player from = Bukkit.getPlayerExact(args[0]);
            request = from == null ? null : requests.find(player.getUniqueId(), from.getUniqueId());
            if (request == null) {
                plugin.messages().send(player, "request-not-found", Messages.player(args[0]));
                return;
            }
        } else {
            List<TpaRequest> list = requests.incoming(player.getUniqueId());
            if (list.isEmpty()) {
                plugin.messages().send(player, "no-requests");
                return;
            }
            if (list.size() > 1) {
                plugin.menus().openRequests(player);
                return;
            }
            request = list.get(0);
        }
        if (accept) {
            requests.accept(player, request);
        } else {
            requests.deny(player, request);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!(sender instanceof Player player) || args.length != 1) {
            return out;
        }
        String typed = args[0].toLowerCase(Locale.ROOT);
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("tpa") || name.equals("tpahere")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p != player && player.canSee(p) && p.getName().toLowerCase(Locale.ROOT).startsWith(typed)) {
                    out.add(p.getName());
                }
            }
        } else if (name.equals("tpaccept") || name.equals("tpadeny")) {
            for (TpaRequest r : plugin.requests().incoming(player.getUniqueId())) {
                String n = RequestManager.nameOf(r.sender());
                if (n.toLowerCase(Locale.ROOT).startsWith(typed)) {
                    out.add(n);
                }
            }
        }
        return out;
    }
}
