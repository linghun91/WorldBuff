package cn.i7mc.worldbuff.managers;

import cn.i7mc.worldbuff.WorldBuff;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    
    private final WorldBuff plugin;
    private FileConfiguration config;
    private boolean debug;
    private final Map<String, Map<String, Double>> worldBuffs = new HashMap<>();
    
    public ConfigManager(WorldBuff plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        
        // 加载调试模式
        debug = config.getBoolean("debug", false);
        
        // 加载世界增益配置
        loadWorldBuffs();
        
        if (debug) {
            plugin.getLogger().info(plugin.getMessageManager().getDebugMessage("config-loaded"));
        }
    }
    
    private void loadWorldBuffs() {
        worldBuffs.clear();
        ConfigurationSection section = config.getConfigurationSection("world-buffs");
        
        if (section != null) {
            for (String worldName : section.getKeys(false)) {
                ConfigurationSection worldSection = section.getConfigurationSection(worldName);
                if (worldSection != null) {
                    Map<String, Double> buffs = new HashMap<>();
                    buffs.put("health", worldSection.getDouble("health", 1.0));
                    buffs.put("damage", worldSection.getDouble("damage", 1.0));
                    buffs.put("speed", worldSection.getDouble("speed", 1.0));
                    buffs.put("jump", worldSection.getDouble("jump", 1.0));
                    
                    worldBuffs.put(worldName, buffs);
                    
                    if (debug) {
                        String debugMsg = plugin.getMessageManager().getDebugMessage("buff-values")
                                .replace("{world}", worldName)
                                .replace("{health}", String.valueOf(buffs.get("health")))
                                .replace("{damage}", String.valueOf(buffs.get("damage")))
                                .replace("{speed}", String.valueOf(buffs.get("speed")))
                                .replace("{jump}", String.valueOf(buffs.get("jump")));
                        plugin.getLogger().info(debugMsg);
                    }
                }
            }
        }
    }
    
    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
    }
    
    public boolean isDebug() {
        return debug;
    }
    
    public Map<String, Double> getWorldBuffs(String worldName) {
        return worldBuffs.getOrDefault(worldName, new HashMap<>());
    }
    
    public boolean hasBuffsForWorld(String worldName) {
        return worldBuffs.containsKey(worldName);
    }
}
