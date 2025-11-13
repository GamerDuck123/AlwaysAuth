package me.gamerduck.alwaysauth.paper;

import org.bukkit.plugin.java.JavaPlugin;

public class AlwaysAuthPlugin extends JavaPlugin {

    private static PaperPlatform paperPlatform;

    @Override
    public void onEnable() {
        try {
            paperPlatform = new PaperPlatform(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.getServer().getPluginManager().registerEvents(paperPlatform, this);
    }

    @Override
    public void onDisable() {
        paperPlatform.onDisable();
    }


}