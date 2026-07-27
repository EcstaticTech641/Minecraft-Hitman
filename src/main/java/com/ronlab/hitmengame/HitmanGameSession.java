package com.ronlab.hitmengame;

import com.ronlab.rga.api.RGASessionControl;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Thread-safe representation of an active Hitman minigame session.
 */
public class HitmanGameSession {

    private final String minigameId;
    private final String worldName;
    private final Map<UUID, Role> roles;
    private volatile boolean active;

    public HitmanGameSession(String minigameId, String worldName, Map<UUID, Role> initialRoles) {
        this.minigameId = minigameId;
        this.worldName = worldName;
        // Enforce immutability for session configuration roles object
        this.roles = Map.copyOf(initialRoles);
        this.active = true;
    }

    public String getMinigameId() {
        return minigameId;
    }

    public String getWorldName() {
        return worldName;
    }

    public Map<UUID, Role> getRoles() {
        return roles; // Already an immutable copy
    }

    public List<UUID> getPlayerUuids() {
        return List.copyOf(roles.keySet()); // Immutable copy of player UUIDs
    }

    public Role getRole(UUID uuid) {
        return roles.get(uuid);
    }

    public boolean isSpeedrunner(UUID uuid) {
        return roles.get(uuid) == Role.SPEEDRUNNER;
    }

    public boolean isHitman(UUID uuid) {
        return roles.get(uuid) == Role.HITMAN;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Checks whether the session has reached a conclusion state (e.g. all speedrunners or hitmen eliminated).
     * Requests graceful teardown via RGA API if concluded.
     */
    public void checkAndConcludeIfFinished() {
        if (!active) {
            return;
        }

        Plugin rgaPlugin = Bukkit.getPluginManager().getPlugin("RonlabGameAssistant");
        if (!(rgaPlugin instanceof RGASessionControl sessionControl)) {
            return;
        }

        int activeSpeedrunners = 0;
        int activeHitmen = 0;

        for (Map.Entry<UUID, Role> entry : roles.entrySet()) {
            UUID uuid = entry.getKey();
            Role role = entry.getValue();

            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && !sessionControl.isSpectator(player)) {
                if (role == Role.SPEEDRUNNER) {
                    activeSpeedrunners++;
                } else if (role == Role.HITMAN) {
                    activeHitmen++;
                }
            }
        }

        String reason = null;
        Map<UUID, Number> scores = new HashMap<>();

        if (activeSpeedrunners == 0) {
            reason = "Hitmen eliminated all speedrunners";
            for (Map.Entry<UUID, Role> entry : roles.entrySet()) {
                scores.put(entry.getKey(), entry.getValue() == Role.HITMAN ? 100 : 0);
            }
        } else if (activeHitmen == 0 && roles.containsValue(Role.HITMAN)) {
            reason = "Speedrunners survived: all hitmen eliminated";
            for (Map.Entry<UUID, Role> entry : roles.entrySet()) {
                scores.put(entry.getKey(), entry.getValue() == Role.SPEEDRUNNER ? 100 : 0);
            }
        }

        if (reason != null) {
            this.active = false;
            try {
                rgaPlugin.getClass()
                        .getMethod("requestSessionConclude", String.class, String.class, Map.class)
                        .invoke(rgaPlugin, worldName, reason, Map.copyOf(scores));
            } catch (Exception e) {
                Bukkit.getLogger().severe("Failed to invoke requestSessionConclude on RGA: " + e.getMessage());
            }
        }
    }
}
