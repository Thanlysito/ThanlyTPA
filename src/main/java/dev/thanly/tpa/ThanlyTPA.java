package dev.thanly.tpa;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Clase principal del plugin. Paper la carga al encender el servidor (ver plugin.yml).
 */
public final class ThanlyTPA extends JavaPlugin {

    private Settings settings;
    private Messages messages;
    private RequestManager requests;
    private TeleportManager teleports;
    private MenuManager menus;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messages = new Messages(this);
        loadSettings();

        requests = new RequestManager(this);
        teleports = new TeleportManager(this);
        menus = new MenuManager(this);

        TpaCommands commands = new TpaCommands(this);
        for (String name : new String[]{"tpa", "tpahere", "tpaccept", "tpadeny", "tpamenu", "tpareload"}) {
            PluginCommand command = getCommand(name);
            if (command != null) {
                command.setExecutor(commands);
                command.setTabCompleter(commands);
            }
        }

        getServer().getPluginManager().registerEvents(new TpaListener(this), this);
        requests.startExpiryTask();
        getLogger().info("ThanlyTPA enabled (language: " + settings.language() + ").");
    }

    @Override
    public void onDisable() {
        if (teleports != null) {
            teleports.cancelAll();
        }
    }

    /** Lee config.yml y el archivo de idioma otra vez. */
    public void loadSettings() {
        reloadConfig();
        settings = Settings.from(getConfig());
        messages.load(settings.language());
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public RequestManager requests() {
        return requests;
    }

    public TeleportManager teleports() {
        return teleports;
    }

    public MenuManager menus() {
        return menus;
    }
}
