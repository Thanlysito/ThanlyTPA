package dev.thanly.tpa;

import org.bukkit.configuration.file.FileConfiguration;

/** Valores de config.yml, ya leidos y validados. */
public record Settings(
        String language,
        int teleportDelay,
        boolean cancelOnMove,
        boolean cancelOnDamage,
        int requestTimeout,
        int requestCooldown,
        boolean sounds,
        boolean testSelf
) {

    public static Settings from(FileConfiguration config) {
        return new Settings(
                config.getString("language", "en"),
                Math.max(0, config.getInt("teleport-delay", 5)),
                config.getBoolean("cancel-on-move", true),
                config.getBoolean("cancel-on-damage", true),
                Math.max(5, config.getInt("request-timeout", 60)),
                Math.max(0, config.getInt("request-cooldown", 10)),
                config.getBoolean("sounds", true),
                config.getBoolean("test-mode-self-requests", false)
        );
    }
}
