package cn.i7mc.worldbuff;

import cn.i7mc.worldbuff.commands.WorldBuffCommand;
import cn.i7mc.worldbuff.listeners.PlayerListener;
import cn.i7mc.worldbuff.managers.BuffManager;
import cn.i7mc.worldbuff.managers.ConfigManager;
import cn.i7mc.worldbuff.managers.MessageManager;
import cn.i7mc.worldbuff.managers.RegionManager;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldBuff extends JavaPlugin {

    private static WorldBuff instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private BuffManager buffManager;
    private RegionManager regionManager;

    @Override
    public void onEnable() {
        instance = this;

        // 初始化配置管理器
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // 初始化消息管理器
        messageManager = new MessageManager(this);
        messageManager.loadMessages();

        // 初始化增益管理器
        buffManager = new BuffManager(this);

        // 初始化区域管理器
        regionManager = new RegionManager(this);
        regionManager.initialize();

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // 注册命令
        getCommand("worldbuff").setExecutor(new WorldBuffCommand(this));

        getLogger().info("WorldBuff 插件已启用！");
    }

    @Override
    public void onDisable() {
        // 清理所有玩家的区域效果
        if (regionManager != null && regionManager.isWorldGuardEnabled()) {
            getServer().getOnlinePlayers().forEach(regionManager::clearPlayerEffects);
        }

        getLogger().info("WorldBuff 插件已禁用！");
    }

    public static WorldBuff getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public BuffManager getBuffManager() {
        return buffManager;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }
}
