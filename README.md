# 战斗伤害日志 · Combat Damage Log

以**魔兽世界战斗日志**风格记录 Minecraft 战斗中的伤害数据：每次攻击的伤害数值、伤害来源（实体 / 环境）、击杀方式与死亡原因。
纯**客户端**模组，跨 **Fabric + NeoForge** 双加载器，目标区间 **1.20.0 → 1.21.11**（分阶段铺开）。

## 功能
- 记录**玩家造成**与**玩家受到**的伤害（双向），以及击杀 / 死亡原因。
- 滚动战斗日志面板（游戏内按 `K` 打开）：WoW 式着色、滚轮滚动、刷新、导出。
- 屏幕角落实时 HUD 摘要（按 `H` 开关）：造成伤害 / 承受伤害 / DPS / 记录条数。
- 本地存储：`<游戏目录>/combatlog/sessions/<会话ID>.jsonl`，按场存储，可导出为 `.txt`。
- 配置：`<游戏目录>/config/combatlog.json`（HUD 开关、缓冲大小、配色等）。

## 架构（Architectury 单代码库）
```
common/   共享逻辑：数据模型、捕获服务、存储、UI、共享 mixin（无害加载器差异）
fabric/   Fabric 入口 + 元数据 + @ExpectPlatform 实现
neoforge/ NeoForge 入口 + 元数据 + @ExpectPlatform 实现
```
捕获通过 `common` 中的共享 mixin（`LivingEntityDamageMixin`）注入 `LivingEntity.damage/die`，
并用 `ClientLevel` 守卫去重（避免单人局集成服务端 + 客户端实体重复计数），两加载器行为一致。

## 构建

### 环境
- **Java 21**（1.21.x 必需；回退 1.20.x 时切 Java 17）
- **Gradle 8.8**（Loom 1.11 需要 Gradle 8.x，**不要用 9.x** —— 会卡在 Loom 配置）

### ⚠️ 关于 NeoForge 与网络（先读这段）
若你的网络**无法访问 `https://maven.neoforged.net`**（表现为 `Connection reset` / 超时），
**NeoForge 构建必然失败**——它依赖该站点的 installer 工具（installertools / mcinjector / DiffPatch）
与 NeoForge 本体。两条路：
1. **仅构建 Fabric（默认，不碰 neoforged）**：保持 `gradle.properties` 里 `enabled_platforms=fabric`。
2. **构建 NeoForge**：先让 `maven.neoforged.net` 可达（VPN / 代理 / 国内镜像），再把
   `enabled_platforms` 改为 `fabric,neoforge`。

`settings.gradle` 会按 `enabled_platforms` 自动 `include` 对应子项目，无需手改 settings。

### 步骤
```bash
# 0) 安装好 JDK 21，且 PATH 上默认 java 是 21（或在命令前设 JAVA_HOME）
# 1) 生成 wrapper（仓库当前只有 gradle-wrapper.properties，缺 gradle-wrapper.jar）
#    请用「Gradle 8.8」执行；若你装的是其它版本，先下载 8.8：
gradle wrapper --gradle-version 8.8

# 2) 构建（默认仅 fabric，不触碰 neoforged）
./gradlew build                 # 等价于 :fabric:build
# 产物：fabric/build/libs/combatlog-fabric-0.1.0+1.21.8.jar

# 若已启用 neoforge 且网络可达：
./gradlew :neoforge:build
```
> 不想用 wrapper 也行：直接用 Gradle 8.8 的 `bin/gradle build` 即可（效果相同）。

## 运行（开发用客户端）
```bash
./gradlew :fabric:runClient
./gradlew :neoforge:runClient
```

## 版本回退路线（对应计划 §3）
- **C 桶 1.21.0–1.21.11**：核心已实现，仅需改 `gradle.properties` 里 `minecraft_version` 扫描各版本，处理极小 API 漂移。
- **B 桶 1.20.5–1.20.6**：伤害源数据化（`DamageTypes` 注册表）。`DamageSourceInfo.extractTypeId` 已通过 `BuiltInRegistries.DAMAGE_TYPE.getKey(type)` 取稳定 id（1.20.5+），旧版 `getMsgId()` 作为回退；用 JDK 21 构建。
- **A 桶 1.20.0–1.20.4**：旧 `DamageSource.getMsgId()`（字符串）。需为 `DamageSourceInfo` 增加该分支（当 `getRegistryEntry()` 不可用时回退 `getMsgId()`），并切换到 **JDK 17** 构建。

> 注意：`gradle.properties` 中的 `architectury_version` / `fabric_api_version` / `neoforge_version` 需与所选
> `minecraft_version` 在 Maven 上匹配；构建前请核对最新可用版本（尤其是 1.21.8 这类较新版本）。

## 已知限制
- **多人局精度**：纯客户端模组。玩家自身的承伤 / 打出完全准确；其他实体的伤害依赖客户端收到的伤害包，
  1.20.5+ 起伤害类型信息较完整，整体为「尽力而为」。
- 仅记录与玩家相关的伤害（玩家造成 / 受到），他人互殴默认不记（避免刷屏，后续可加配置放宽）。

## 按键
| 按键 | 作用 |
|------|------|
| `K`  | 打开战斗日志面板 |
| `H`  | 切换 HUD 摘要显隐 |
| `ESC`| 关闭面板 |
