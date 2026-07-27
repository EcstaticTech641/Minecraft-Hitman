package com.ronlab.hitmengame;

import com.ronlab.rga.api.RGASessionControl;
import com.ronlab.rga.api.event.MinigameConcludeEvent;
import com.ronlab.rga.api.event.MinigameStartEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event listener handling Hitman game lifecycle and elimination triggers.
 */
public class HitmanListener implements Listener {

    private final HitmanPlugin plugin;

    public HitmanListener(HitmanPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onMinigameStart(MinigameStartEvent event) {
        String minigameId = event.getMinigameId();
        if (minigameId == null || (!minigameId.equalsIgnoreCase("hitman") && !minigameId.equalsIgnoreCase("hitmengame"))) {
            return;
        }

        List<UUID> playerUuids = event.getPlayerUuids();
        if (playerUuids.isEmpty()) {
            return;
        }

        // Thread-safe map for player role allocations
        ConcurrentHashMap<UUID, Role> roles = new ConcurrentHashMap<>();

        // 1st player is designated as Speedrunner; remaining players are designated as Hitmen
        roles.put(playerUuids.get(0), Role.SPEEDRUNNER);
        for (int i = 1; i < playerUuids.size(); i++) {
            roles.put(playerUuids.get(i), Role.HITMAN);
        }

        HitmanGameSession session = new HitmanGameSession(minigameId, event.getWorldName(), roles);
        plugin.getActiveSessions().put(event.getWorldName(), session);
        plugin.getLogger().info("Started Hitman session for world " + event.getWorldName() + " with " + playerUuids.size() + " players.");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        HitmanGameSession session = plugin.getSessionForPlayer(victim.getUniqueId());
        if (session == null || !session.isActive()) {
            return;
        }

        Plugin rgaPlugin = Bukkit.getPluginManager().getPlugin("RonlabGameAssistant");
        if (rgaPlugin instanceof RGASessionControl sessionControl) {
            // Set victim to spectator using RGA Session Control
            sessionControl.setSpectator(victim, true);
        }

        // Evaluate if the elimination triggers session conclusion
        session.checkAndConcludeIfFinished();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMinigameConclude(MinigameConcludeEvent event) {
        HitmanGameSession session = plugin.getActiveSessions().remove(event.getWorldName());
        if (session != null) {
            session.setActive(false);
            plugin.getLogger().info("Hitman session concluded for world " + event.getWorldName());
        }
    }
}
