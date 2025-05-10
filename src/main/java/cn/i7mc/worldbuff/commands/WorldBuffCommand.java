package cn.i7mc.worldbuff.commands;

import cn.i7mc.worldbuff.WorldBuff;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WorldBuffCommand implements CommandExecutor, TabCompleter {

    private final WorldBuff plugin;

    public WorldBuffCommand(WorldBuff plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // 显示帮助信息
            sender.sendMessage("§b===== WorldBuff 帮助 =====");
            sender.sendMessage("§a/worldbuff reload §7- 重载插件配置");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("worldbuff.reload")) {
                plugin.getMessageManager().sendMessage((Player) sender, "no-permission");
                return true;
            }

            // 重载配置
            plugin.getConfigManager().reloadConfig();
            plugin.getMessageManager().reloadMessages();

            // 重载区域管理器
            if (plugin.getRegionManager() != null) {
                plugin.getRegionManager().reload();
            }

            // 重新应用所有在线玩家的增益效果
            for (Player player : Bukkit.getOnlinePlayers()) {
                String worldName = player.getWorld().getName();
                if (plugin.getConfigManager().hasBuffsForWorld(worldName)) {
                    plugin.getBuffManager().applyWorldBuffs(player, worldName);
                } else {
                    plugin.getBuffManager().removeWorldBuffs(player);
                }

                // 检查玩家所在区域
                if (plugin.getRegionManager().isWorldGuardEnabled()) {
                    plugin.getRegionManager().checkPlayerRegions(player);
                }
            }

            plugin.getMessageManager().sendMessage((Player) sender, "reload-success");
            return true;
        }

        // 未知命令，显示帮助信息
        sender.sendMessage("§b===== WorldBuff 帮助 =====");
        sender.sendMessage("§a/worldbuff reload §7- 重载插件配置");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload");
            String input = args[0].toLowerCase();

            completions = subCommands.stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}
