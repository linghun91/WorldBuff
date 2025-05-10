package cn.i7mc.worldbuff.managers;

import cn.i7mc.worldbuff.WorldBuff;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class RegionManager {

    private final WorldBuff plugin;
    private final Map<String, Map<String, Double>> regionBuffs = new HashMap<>();
    private final Map<UUID, Set<String>> playerRegions = new HashMap<>();
    private final Map<UUID, Map<String, Map<PotionEffectType, Integer>>> playerRegionEffects = new HashMap<>();
    private boolean worldGuardEnabled = false;
    private BukkitTask checkTask;
    private int checkInterval;

    // 药水效果持续时间（秒）
    private static final int POTION_DURATION = 60 * 20; // 60秒 * 20 ticks/秒

    public RegionManager(WorldBuff plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化WorldGuard集成
     */
    public void initialize() {
        // 检查WorldGuard插件是否存在
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            if (plugin.getConfigManager().isDebug()) {
                String debugMsg = plugin.getMessageManager().getDebugMessage("worldguard-hook")
                        .replace("{status}", "禁用");
                plugin.getLogger().info(debugMsg);
            }
            return;
        }

        worldGuardEnabled = plugin.getConfig().getBoolean("worldguard.enabled", true);
        checkInterval = plugin.getConfig().getInt("worldguard.check-interval", 20);

        if (!worldGuardEnabled) {
            if (plugin.getConfigManager().isDebug()) {
                String debugMsg = plugin.getMessageManager().getDebugMessage("worldguard-hook")
                        .replace("{status}", "禁用（配置文件中已禁用）");
                plugin.getLogger().info(debugMsg);
            }
            return;
        }

        // 加载区域增益配置
        loadRegionBuffs();

        // 启动检查任务
        startCheckTask();

        if (plugin.getConfigManager().isDebug()) {
            String debugMsg = plugin.getMessageManager().getDebugMessage("worldguard-hook")
                    .replace("{status}", "启用");
            plugin.getLogger().info(debugMsg);
        }
    }

    /**
     * 加载区域增益配置
     */
    private void loadRegionBuffs() {
        regionBuffs.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("region-buffs");

        if (section != null) {
            for (String regionId : section.getKeys(false)) {
                ConfigurationSection regionSection = section.getConfigurationSection(regionId);
                if (regionSection != null) {
                    Map<String, Double> buffs = new HashMap<>();
                    for (String key : regionSection.getKeys(false)) {
                        buffs.put(key, regionSection.getDouble(key, 1.0));
                    }
                    regionBuffs.put(regionId, buffs);

                    if (plugin.getConfigManager().isDebug()) {
                        String debugMsg = plugin.getMessageManager().getDebugMessage("region-buff-values")
                                .replace("{region}", regionId);
                        plugin.getLogger().info(debugMsg);
                    }
                }
            }
        }
    }

    /**
     * 启动检查任务
     */
    private void startCheckTask() {
        if (checkTask != null) {
            checkTask.cancel();
        }

        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                checkPlayerRegions(player);
            }
        }, 20L, checkInterval);
    }

    /**
     * 检查玩家所在的区域
     *
     * @param player 玩家
     */
    public void checkPlayerRegions(Player player) {
        if (!worldGuardEnabled) return;

        Location location = player.getLocation();
        Set<String> currentRegions = getRegionsAt(location);
        Set<String> previousRegions = playerRegions.getOrDefault(player.getUniqueId(), new HashSet<>());

        // 找出玩家离开的区域
        Set<String> leftRegions = new HashSet<>(previousRegions);
        leftRegions.removeAll(currentRegions);

        // 找出玩家进入的区域
        Set<String> enteredRegions = new HashSet<>(currentRegions);
        enteredRegions.removeAll(previousRegions);

        // 处理玩家离开的区域
        for (String regionId : leftRegions) {
            if (regionBuffs.containsKey(regionId)) {
                removeRegionBuffs(player, regionId);
                plugin.getMessageManager().sendMessage(player, "leave-region", "{region}", regionId);
            }
        }

        // 处理玩家进入的区域
        for (String regionId : enteredRegions) {
            if (regionBuffs.containsKey(regionId)) {
                applyRegionBuffs(player, regionId);
                plugin.getMessageManager().sendMessage(player, "enter-region", "{region}", regionId);
            }
        }

        // 更新玩家当前所在区域
        playerRegions.put(player.getUniqueId(), currentRegions);
    }

    /**
     * 获取指定位置的所有区域ID
     *
     * @param location 位置
     * @return 区域ID集合
     */
    private Set<String> getRegionsAt(Location location) {
        Set<String> regions = new HashSet<>();
        
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            ApplicableRegionSet regionSet = query.getApplicableRegions(BukkitAdapter.adapt(location));
            
            for (ProtectedRegion region : regionSet) {
                regions.add(region.getId());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("获取区域信息时发生错误: " + e.getMessage());
        }
        
        return regions;
    }

    /**
     * 应用区域增益效果
     *
     * @param player 玩家
     * @param regionId 区域ID
     */
    private void applyRegionBuffs(Player player, String regionId) {
        if (!regionBuffs.containsKey(regionId)) return;

        if (plugin.getConfigManager().isDebug()) {
            String debugMsg = plugin.getMessageManager().getDebugMessage("apply-region-buff")
                    .replace("{player}", player.getName())
                    .replace("{region}", regionId);
            plugin.getLogger().info(debugMsg);
        }

        Map<String, Double> buffs = regionBuffs.get(regionId);
        Map<PotionEffectType, Integer> potionEffects = new HashMap<>();

        // 应用所有药水效果
        for (Map.Entry<String, Double> entry : buffs.entrySet()) {
            String effectName = entry.getKey();
            double value = entry.getValue();

            if (value <= 1.0) continue;

            int amplifier = (int) Math.floor(value - 1.0);
            PotionEffectType effectType = getPotionEffectType(effectName);

            if (effectType != null) {
                player.addPotionEffect(new PotionEffect(effectType, POTION_DURATION, amplifier, false, false, true));
                potionEffects.put(effectType, amplifier);
            }
        }

        // 保存玩家的区域效果
        UUID playerId = player.getUniqueId();
        Map<String, Map<PotionEffectType, Integer>> playerEffects = playerRegionEffects.getOrDefault(playerId, new HashMap<>());
        playerEffects.put(regionId, potionEffects);
        playerRegionEffects.put(playerId, playerEffects);
    }

    /**
     * 移除区域增益效果
     *
     * @param player 玩家
     * @param regionId 区域ID
     */
    private void removeRegionBuffs(Player player, String regionId) {
        UUID playerId = player.getUniqueId();
        Map<String, Map<PotionEffectType, Integer>> playerEffects = playerRegionEffects.get(playerId);
        
        if (playerEffects == null || !playerEffects.containsKey(regionId)) return;

        if (plugin.getConfigManager().isDebug()) {
            String debugMsg = plugin.getMessageManager().getDebugMessage("remove-region-buff")
                    .replace("{player}", player.getName())
                    .replace("{region}", regionId);
            plugin.getLogger().info(debugMsg);
        }

        // 移除药水效果
        Map<PotionEffectType, Integer> effects = playerEffects.get(regionId);
        for (PotionEffectType effectType : effects.keySet()) {
            player.removePotionEffect(effectType);
        }

        // 移除区域效果记录
        playerEffects.remove(regionId);
        
        // 重新应用其他区域的效果
        for (String otherRegionId : playerEffects.keySet()) {
            Map<PotionEffectType, Integer> otherEffects = playerEffects.get(otherRegionId);
            for (Map.Entry<PotionEffectType, Integer> entry : otherEffects.entrySet()) {
                player.addPotionEffect(new PotionEffect(entry.getKey(), POTION_DURATION, entry.getValue(), false, false, true));
            }
        }
    }

    /**
     * 根据效果名称获取PotionEffectType
     *
     * @param name 效果名称
     * @return PotionEffectType
     */
    private PotionEffectType getPotionEffectType(String name) {
        switch (name.toUpperCase()) {
            case "NIGHT_VISION": return PotionEffectType.NIGHT_VISION;
            case "INVISIBILITY": return PotionEffectType.INVISIBILITY;
            case "JUMP": return PotionEffectType.JUMP;
            case "FIRE_RESISTANCE": return PotionEffectType.FIRE_RESISTANCE;
            case "SPEED": return PotionEffectType.SPEED;
            case "SLOWNESS": return PotionEffectType.SLOW;
            case "WATER_BREATHING": return PotionEffectType.WATER_BREATHING;
            case "INSTANT_HEAL": return PotionEffectType.HEAL;
            case "INSTANT_DAMAGE": return PotionEffectType.HARM;
            case "POISON": return PotionEffectType.POISON;
            case "REGEN": return PotionEffectType.REGENERATION;
            case "STRENGTH": return PotionEffectType.INCREASE_DAMAGE;
            case "WEAKNESS": return PotionEffectType.WEAKNESS;
            case "LUCK": return PotionEffectType.LUCK;
            case "SLOW_FALLING": return PotionEffectType.SLOW_FALLING;
            default: return null;
        }
    }

    /**
     * 清除玩家的所有区域效果
     *
     * @param player 玩家
     */
    public void clearPlayerEffects(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, Map<PotionEffectType, Integer>> playerEffects = playerRegionEffects.get(playerId);
        
        if (playerEffects == null) return;

        // 移除所有药水效果
        for (Map<PotionEffectType, Integer> effects : playerEffects.values()) {
            for (PotionEffectType effectType : effects.keySet()) {
                player.removePotionEffect(effectType);
            }
        }

        // 清除记录
        playerEffects.clear();
        playerRegionEffects.remove(playerId);
        playerRegions.remove(playerId);
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        // 停止当前任务
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }

        // 清除所有玩家的效果
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearPlayerEffects(player);
        }

        // 重新初始化
        initialize();
    }

    /**
     * 检查WorldGuard是否启用
     *
     * @return 是否启用
     */
    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }
}
