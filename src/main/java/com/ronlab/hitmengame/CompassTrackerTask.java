package com.ronlab.hitmengame;

import com.ronlab.rga.api.RGASessionControl;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;

/**
 * Periodically updates tracking compass targets for active hitmen to point towards
 * the nearest active, non-spectating speedrunner in the same world.
 */
public class CompassTrackerTask extends BukkitRunnable {

    private final HitmanPlugin plugin;

    public CompassTrackerTask(HitmanPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        Plugin rgaPlugin = Bukkit.getPluginManager().getPlugin("RonlabGameAssistant");
        if (!(rgaPlugin instanceof RGASessionControl sessionControl)) {
            return;
        }

        for (HitmanGameSession session : plugin.getActiveSessions().values()) {
            if (!session.isActive()) {
                continue;
            }

            for (Map.Entry<UUID, Role> entry : session.getRoles().entrySet()) {
                if (entry.getValue() != Role.HITMAN) {
                    continue;
                }

                Player hitman = Bukkit.getPlayer(entry.getKey());
                if (hitman == null || !hitman.isOnline() || sessionControl.isSpectator(hitman)) {
                    continue;
                }

                // Find nearest active, non-spectating speedrunner in the same world
                Player nearestSpeedrunner = null;
                double nearestDistanceSq = Double.MAX_VALUE;

                for (Map.Entry<UUID, Role> targetEntry : session.getRoles().entrySet()) {
                    if (targetEntry.getValue() != Role.SPEEDRUNNER) {
                        continue;
                    }

                    Player speedrunner = Bukkit.getPlayer(targetEntry.getKey());
                    if (speedrunner == null || !speedrunner.isOnline() || sessionControl.isSpectator(speedrunner)) {
                        continue;
                    }

                    if (!speedrunner.getWorld().equals(hitman.getWorld())) {
                        continue;
                    }

                    double distanceSq = hitman.getLocation().distanceSquared(speedrunner.getLocation());
                    if (distanceSq < nearestDistanceSq) {
                        nearestDistanceSq = distanceSq;
                        nearestSpeedrunner = speedrunner;
                    }
                }

                if (nearestSpeedrunner != null) {
                    hitman.setCompassTarget(nearestSpeedrunner.getLocation());
                }
            }
        }
    }
}
