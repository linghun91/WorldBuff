package cn.i7mc.worldbuff.managers;

import cn.i7mc.worldbuff.WorldBuff;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public class MessageManager {

    private final WorldBuff plugin;
    private FileConfiguration messageConfig;
    private FileConfiguration debugMessageConfig;
    private File messageFile;
    private File debugMessageFile;

    public MessageManager(WorldBuff plugin) {
        this.plugin = plugin;
    }

    public void loadMessages() {
        // 加载普通消息
        messageFile = new File(plugin.getDataFolder(), "message.yml");
        if (!messageFile.exists()) {
            plugin.saveResource("message.yml", false);
        }
        messageConfig = YamlConfiguration.loadConfiguration(messageFile);

        // 加载调试消息
        debugMessageFile = new File(plugin.getDataFolder(), "debugmessage.yml");
        if (!debugMessageFile.exists()) {
            plugin.saveResource("debugmessage.yml", false);
        }
        debugMessageConfig = YamlConfiguration.loadConfiguration(debugMessageFile);
    }

    public void reloadMessages() {
        loadMessages();
    }

    public String getMessage(String key) {
        String message = messageConfig.getString(key, "Message not found: " + key);
        return translateColorCodes(message);
    }

    public String getDebugMessage(String key) {
        String message = debugMessageConfig.getString(key, "Debug message not found: " + key);
        return translateColorCodes(message);
    }

    /**
     * 将颜色代码转换为实际颜色
     *
     * @param text 包含颜色代码的文本
     * @return 转换后的文本
     */
    @SuppressWarnings("deprecation")
    private String translateColorCodes(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public void sendMessage(Player player, String key) {
        String prefix = getMessage("prefix");
        String message = getMessage(key);
        player.sendMessage(prefix + message);
    }

    public void sendMessage(Player player, String key, String... replacements) {
        String prefix = getMessage("prefix");
        String message = getMessage(key);

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }

        player.sendMessage(prefix + message);
    }
}
