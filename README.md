# WorldBuff

WorldBuff是一个Minecraft Paper插件，为世界和WorldGuard区域提供自定义药水效果功能。

## 功能特点

- 为不同的世界配置不同的增益效果（生命值、伤害、速度、跳跃等）
- 为WorldGuard自定义区域添加自定义药水效果
- 玩家进入/离开世界或区域时自动应用/移除增益效果
- 支持重载配置
- 完全可自定义的消息系统
- 调试模式，可以输出详细的调试信息

## 配置文件

### config.yml
```yaml
# 调试模式
debug: false

# WorldGuard集成
worldguard:
  enabled: true  # 是否启用WorldGuard集成
  check-interval: 20  # 检查玩家是否在区域内的间隔（刻）

# 世界增益配置
world-buffs:
  world:  # 世界名称
    NIGHT_VISION: 1.0  # 夜视效果
    SPEED: 1.0  # 速度效果
    JUMP: 1.0  # 跳跃提升效果
    # 更多效果...

# WorldGuard区域增益配置
region-buffs:
  example_region:  # 区域ID
    NIGHT_VISION: 2.0  # 夜视效果
    SPEED: 2.0  # 速度效果
    # 更多效果...
```

### message.yml
包含所有插件消息，可自定义。

### debugmessage.yml
包含所有调试信息，仅在debug: true时输出。

## 命令

- `/worldbuff reload` - 重载插件配置（需要权限：worldbuff.reload）

## 权限

- `worldbuff.command` - 允许使用WorldBuff命令
- `worldbuff.reload` - 允许重载插件配置

## 依赖

- Paper 1.20.1
- WorldGuard (可选，用于区域增益功能)
- WorldEdit (可选，WorldGuard的依赖)

## 作者

- Saga

## 许可证

MIT License
