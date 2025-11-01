package me.gamerduck.alwaysauth.spigot;

import org.bukkit.plugin.java.JavaPlugin;

public class AlwaysAuthPlugin extends JavaPlugin {

    private static SpigotPlatform spigotPlatform;

    @Override
    public void onEnable() {
        try {
            spigotPlatform = new SpigotPlatform(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        spigotPlatform.onDisable();
    }


}