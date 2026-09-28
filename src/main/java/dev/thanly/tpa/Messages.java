package dev.thanly.tpa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Textos del plugin. Se leen de plugins/ThanlyTPA/lang/&lt;idioma&gt;.yml y se
 * convierten con MiniMessage (colores, negritas, etc.).
 */
public final class Messages {

    private static final String[] BUNDLED = {"en", "es"};

    private final ThanlyTPA plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private YamlConfiguration lang = new YamlConfiguration();
    private YamlConfiguration fallback = new YamlConfiguration();

    public Messages(ThanlyTPA plugin) {
        this.plugin = plugin;
    }

    public void load(String language) {
        // Copia los idiomas incluidos la primera vez, para que el dueno del servidor los pueda editar.
        for (String code : BUNDLED) {
            File file = new File(plugin.getDataFolder(), "lang/" + code + ".yml");
            if (!file.exists()) {
                plugin.saveResource("lang/" + code + ".yml", false);
            }
        }
        fallback = readBundled("en");
        File file = new File(plugin.getDataFolder(), "lang/" + language + ".yml");
        if (file.exists()) {
            lang = YamlConfiguration.loadConfiguration(file);
        } else {
            plugin.getLogger().warning("Language file lang/" + language + ".yml not found, using English.");
            lang = fallback;
        }
    }

    private YamlConfiguration readBundled(String code) {
        InputStream in = plugin.getResource("lang/" + code + ".yml");
        if (in == null) {
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    private String raw(String key) {
        String value = lang.getString(key);
        if (value == null) {
            value = fallback.getString(key, key);
        }
        return value;
    }

    /** Un texto sin el prefijo (para menus, botones y la barra de accion). */
    public Component get(String key, TagResolver... resolvers) {
        return mini.deserialize(raw(key), resolvers);
    }

    /** Un texto con el prefijo "TPA »" delante (para el chat). */
    public Component withPrefix(String key, TagResolver... resolvers) {
        return mini.deserialize(raw("prefix")).append(get(key, resolvers));
    }

    public void send(CommandSender to, String key, TagResolver... resolvers) {
        to.sendMessage(withPrefix(key, resolvers));
    }

    public static TagResolver player(String name) {
        return Placeholder.unparsed("player", name);
    }

    public static TagResolver seconds(long seconds) {
        return Placeholder.unparsed("seconds", String.valueOf(seconds));
    }
}
