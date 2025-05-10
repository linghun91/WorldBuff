package cn.i7mc.worldbuff.listeners;

import cn.i7mc.worldbuff.WorldBuff;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerListener implements Listener {

    private final WorldBuff plugin;

    public PlayerListener(WorldBuff plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        // 应用世界增益效果
        if (plugin.getConfigManager().hasBuffsForWorld(worldName)) {
            plugin.getBuffManager().applyWorldBuffs(player, worldName);
        }

        // 检查玩家所在区域
        if (plugin.getRegionManager().isWorldGuardEnabled()) {
            plugin.getRegionManager().checkPlayerRegions(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // 移除世界增益效果
        plugin.getBuffManager().removeWorldBuffs(player);

        // 移除区域增益效果
        if (plugin.getRegionManager().isWorldGuardEnabled()) {
            plugin.getRegionManager().clearPlayerEffects(player);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String fromWorld = event.getFrom().getName();
        String toWorld = player.getWorld().getName();

        // 如果玩家从有增益效果的世界离开，发送消息
        if (plugin.getConfigManager().hasBuffsForWorld(fromWorld)) {
            plugin.getMessageManager().sendMessage(player, "leave-world", "{world}", fromWorld);
        }

        // 应用新世界的增益效果
        if (plugin.getConfigManager().hasBuffsForWorld(toWorld)) {
            plugin.getBuffManager().applyWorldBuffs(player, toWorld);
        }

        // 检查玩家所在区域
        if (plugin.getRegionManager().isWorldGuardEnabled()) {
            plugin.getRegionManager().checkPlayerRegions(player);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // 只有当玩家移动到新的区块时才检查区域
        if (event.getFrom().getBlockX() >> 4 == event.getTo().getBlockX() >> 4 &&
            event.getFrom().getBlockZ() >> 4 == event.getTo().getBlockZ() >> 4) {
            return;
        }

        // 检查玩家所在区域
        if (plugin.getRegionManager().isWorldGuardEnabled()) {
            plugin.getRegionManager().checkPlayerRegions(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // 检查玩家所在区域
        if (plugin.getRegionManager().isWorldGuardEnabled()) {
            plugin.getRegionManager().checkPlayerRegions(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // 检查玩家所在区域
        if (plugin.getRegionManager().isWorldGuardEnabled()) {
            plugin.getRegionManager().checkPlayerRegions(event.getPlayer());
        }
    }
}
