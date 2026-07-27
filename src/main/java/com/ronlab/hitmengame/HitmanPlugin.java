package com.ronlab.hitmengame;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main entry point for HitmanGame companion plugin.
 */
public final class HitmanPlugin extends JavaPlugin {

    private final Map<String, HitmanGameSession> activeSessions = new ConcurrentHashMap<>();
    private CompassTrackerTask compassTask;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new HitmanListener(this), this);

        compassTask = new CompassTrackerTask(this);
        compassTask.runTaskTimer(this, 20L, 20L);

        getLogger().info("HitmanGame companion plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (compassTask != null) {
            compassTask.cancel();
        }
        activeSessions.clear();
        getLogger().info("HitmanGame companion plugin disabled.");
    }

    public Map<String, HitmanGameSession> getActiveSessions() {
        return activeSessions;
    }

    public HitmanGameSession getSessionForPlayer(UUID playerUuid) {
        for (HitmanGameSession session : activeSessions.values()) {
            if (session.getRoles().containsKey(playerUuid)) {
                return session;
            }
        }
        return null;
    }
}
