package cn.i7mc.worldbuff.managers;

import cn.i7mc.worldbuff.WorldBuff;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BuffManager {

    private final WorldBuff plugin;
    private final Map<UUID, Map<Attribute, AttributeModifier>> playerBuffs = new HashMap<>();
    private final Map<UUID, Map<PotionEffectType, Integer>> playerPotionEffects = new HashMap<>();

    // 为每种属性创建唯一的UUID
    private static final UUID HEALTH_UUID = UUID.fromString("f8b0c130-7c9f-11ee-b962-0242ac120002");
    private static final UUID DAMAGE_UUID = UUID.fromString("f8b0c368-7c9f-11ee-b962-0242ac120002");
    private static final UUID SPEED_UUID = UUID.fromString("f8b0c4a8-7c9f-11ee-b962-0242ac120002");

    // 药水效果持续时间（秒）
    private static final int POTION_DURATION = 60 * 20; // 60秒 * 20 ticks/秒

    public BuffManager(WorldBuff plugin) {
        this.plugin = plugin;
    }

    public void applyWorldBuffs(Player player, String worldName) {
        if (!plugin.getConfigManager().hasBuffsForWorld(worldName)) {
            return;
        }

        Map<String, Double> buffs = plugin.getConfigManager().getWorldBuffs(worldName);

        if (plugin.getConfigManager().isDebug()) {
            String debugMsg = plugin.getMessageManager().getDebugMessage("apply-buff")
                    .replace("{player}", player.getName())
                    .replace("{world}", worldName);
            plugin.getLogger().info(debugMsg);
        }

        // 移除之前的增益效果
        removeWorldBuffs(player);

        // 创建新的增益效果映射
        Map<Attribute, AttributeModifier> modifiers = new HashMap<>();

        // 应用生命值增益
        if (buffs.containsKey("health") && buffs.get("health") != 1.0) {
            AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (healthAttr != null) {
                AttributeModifier healthMod = new AttributeModifier(
                        HEALTH_UUID,
                        "worldbuff_health",
                        buffs.get("health") - 1.0,
                        AttributeModifier.Operation.MULTIPLY_SCALAR_1
                );
                healthAttr.addModifier(healthMod);
                modifiers.put(Attribute.GENERIC_MAX_HEALTH, healthMod);
            }
        }

        // 应用伤害增益
        if (buffs.containsKey("damage") && buffs.get("damage") != 1.0) {
            AttributeInstance damageAttr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (damageAttr != null) {
                AttributeModifier damageMod = new AttributeModifier(
                        DAMAGE_UUID,
                        "worldbuff_damage",
                        buffs.get("damage") - 1.0,
                        AttributeModifier.Operation.MULTIPLY_SCALAR_1
                );
                damageAttr.addModifier(damageMod);
                modifiers.put(Attribute.GENERIC_ATTACK_DAMAGE, damageMod);
            }
        }

        // 应用速度增益
        if (buffs.containsKey("speed") && buffs.get("speed") != 1.0) {
            AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) {
                AttributeModifier speedMod = new AttributeModifier(
                        SPEED_UUID,
                        "worldbuff_speed",
                        buffs.get("speed") - 1.0,
                        AttributeModifier.Operation.MULTIPLY_SCALAR_1
                );
                speedAttr.addModifier(speedMod);
                modifiers.put(Attribute.GENERIC_MOVEMENT_SPEED, speedMod);
            }
        }

        // 应用跳跃增益
        if (buffs.containsKey("jump") && buffs.get("jump") != 1.0) {
            int amplifier = (int) Math.floor(buffs.get("jump") - 1.0);
            if (amplifier >= 0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, POTION_DURATION, amplifier, false, false, true));
            }
        }

        // 应用所有药水效果
        Map<PotionEffectType, Integer> potionEffects = new HashMap<>();

        // 处理所有PotionType
        applyPotionEffect(player, buffs, "NIGHT_VISION", PotionEffectType.NIGHT_VISION, potionEffects);
        applyPotionEffect(player, buffs, "INVISIBILITY", PotionEffectType.INVISIBILITY, potionEffects);
        applyPotionEffect(player, buffs, "FIRE_RESISTANCE", PotionEffectType.FIRE_RESISTANCE, potionEffects);
        applyPotionEffect(player, buffs, "SPEED", PotionEffectType.SPEED, potionEffects);
        applyPotionEffect(player, buffs, "SLOWNESS", PotionEffectType.SLOW, potionEffects);
        applyPotionEffect(player, buffs, "WATER_BREATHING", PotionEffectType.WATER_BREATHING, potionEffects);
        applyPotionEffect(player, buffs, "INSTANT_HEAL", PotionEffectType.HEAL, potionEffects);
        applyPotionEffect(player, buffs, "INSTANT_DAMAGE", PotionEffectType.HARM, potionEffects);
        applyPotionEffect(player, buffs, "POISON", PotionEffectType.POISON, potionEffects);
        applyPotionEffect(player, buffs, "REGEN", PotionEffectType.REGENERATION, potionEffects);
        applyPotionEffect(player, buffs, "STRENGTH", PotionEffectType.INCREASE_DAMAGE, potionEffects);
        applyPotionEffect(player, buffs, "WEAKNESS", PotionEffectType.WEAKNESS, potionEffects);
        applyPotionEffect(player, buffs, "LUCK", PotionEffectType.LUCK, potionEffects);
        applyPotionEffect(player, buffs, "TURTLE_MASTER", PotionEffectType.DAMAGE_RESISTANCE, potionEffects); // 部分效果
        applyPotionEffect(player, buffs, "SLOW_FALLING", PotionEffectType.SLOW_FALLING, potionEffects);

        // 保存玩家的增益效果
        playerBuffs.put(player.getUniqueId(), modifiers);
        playerPotionEffects.put(player.getUniqueId(), potionEffects);

        // 发送消息给玩家
        plugin.getMessageManager().sendMessage(player, "enter-world", "{world}", worldName);
    }

    /**
     * 应用药水效果
     *
     * @param player 玩家
     * @param buffs 增益配置
     * @param configKey 配置键
     * @param effectType 药水效果类型
     * @param potionEffects 药水效果映射
     */
    private void applyPotionEffect(Player player, Map<String, Double> buffs, String configKey,
                                  PotionEffectType effectType, Map<PotionEffectType, Integer> potionEffects) {
        if (buffs.containsKey(configKey) && buffs.get(configKey) != 1.0) {
            int amplifier = (int) Math.floor(buffs.get(configKey) - 1.0);
            if (amplifier >= 0) {
                player.addPotionEffect(new PotionEffect(effectType, POTION_DURATION, amplifier, false, false, true));
                potionEffects.put(effectType, amplifier);
            }
        }
    }

    public void removeWorldBuffs(Player player) {
        if (plugin.getConfigManager().isDebug()) {
            String debugMsg = plugin.getMessageManager().getDebugMessage("remove-buff")
                    .replace("{player}", player.getName())
                    .replace("{world}", player.getWorld().getName());
            plugin.getLogger().info(debugMsg);
        }

        // 移除属性修饰符
        Map<Attribute, AttributeModifier> modifiers = playerBuffs.get(player.getUniqueId());
        if (modifiers != null) {
            // 移除生命值增益
            AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (healthAttr != null && modifiers.containsKey(Attribute.GENERIC_MAX_HEALTH)) {
                healthAttr.removeModifier(modifiers.get(Attribute.GENERIC_MAX_HEALTH));
            }

            // 移除伤害增益
            AttributeInstance damageAttr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (damageAttr != null && modifiers.containsKey(Attribute.GENERIC_ATTACK_DAMAGE)) {
                damageAttr.removeModifier(modifiers.get(Attribute.GENERIC_ATTACK_DAMAGE));
            }

            // 移除速度增益
            AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null && modifiers.containsKey(Attribute.GENERIC_MOVEMENT_SPEED)) {
                speedAttr.removeModifier(modifiers.get(Attribute.GENERIC_MOVEMENT_SPEED));
            }

            // 从映射中移除玩家
            playerBuffs.remove(player.getUniqueId());
        }

        // 移除药水效果
        Map<PotionEffectType, Integer> potionEffects = playerPotionEffects.get(player.getUniqueId());
        if (potionEffects != null) {
            for (PotionEffectType effectType : potionEffects.keySet()) {
                player.removePotionEffect(effectType);
            }
            playerPotionEffects.remove(player.getUniqueId());
        }
    }
}
