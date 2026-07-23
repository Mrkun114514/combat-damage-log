package com.mrkun.combatlog.capture;

import com.mrkun.combatlog.CombatLog;
import com.mrkun.combatlog.storage.LogConfig;
import com.mrkun.combatlog.storage.SessionStore;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * 战斗日志捕获服务（单例）。由共享 mixin 在客户端调用。
 * 仅记录与玩家相关的伤害（玩家造成 / 玩家受到），避免刷屏。
 */
public final class CombatLogService {
    private static final CombatLogService INSTANCE = new CombatLogService();

    public static CombatLogService getInstance() {
        return INSTANCE;
    }

    private final SessionStore store;
    private final LogConfig config;

    private CombatLogService() {
        this.config = LogConfig.load();
        this.store = new SessionStore(config);
    }

    public void onDamage(LivingEntity entity, DamageSource source, float amount) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        Player player = mc.player;
        if (player == null) return;
        // 单人局里 hurtServer 在服务端线程执行，传入的 entity / 攻击者都是服务端实体（ServerPlayer），
        // 与客户端 Minecraft.getInstance().player（LocalPlayer）不是同一个对象，不能用 == 比较，
        // 否则所有事件都会被误判为“与玩家无关”而被过滤掉。改用 UUID 比对。
        UUID me = player.getUUID();
        boolean isPlayerTarget = isLocalPlayer(entity, me);
        boolean isPlayerAttacker = isLocalPlayer(source.getEntity(), me) || isLocalPlayer(source.getDirectEntity(), me);
        if (!isPlayerTarget && !isPlayerAttacker) return;

        String eventType = isPlayerTarget ? "DAMAGE_TAKEN" : "DAMAGE_DEALT";
        DamageSourceInfo info = DamageSourceInfo.from(source, entity);
        CombatLogEntry entry = new CombatLogEntry(
                System.currentTimeMillis(), eventType, amount,
                info.sourceType, info.sourceEntityName, info.sourceEntityType, info.directEntityName,
                info.damageTypeId,
                entity.getName().getString(),
                entityId(entity),
                "", posString(entity));
        store.append(entry);
    }

    public void onDeath(LivingEntity entity, DamageSource source) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        Player player = mc.player;
        if (player == null) return;
        UUID me = player.getUUID();
        boolean selfDeath = isLocalPlayer(entity, me);
        boolean playerKilled = isLocalPlayer(source.getEntity(), me) || isLocalPlayer(source.getDirectEntity(), me);
        // 只记录与玩家相关的死亡：玩家自己死亡，或玩家击杀的实体（避免刷屏记录满世界怪物的自然死亡）
        if (!selfDeath && !playerKilled) return;

        DamageSourceInfo info = DamageSourceInfo.from(source, entity);
        String deathMsg = source.getLocalizedDeathMessage(entity).getString();
        CombatLogEntry entry = new CombatLogEntry(
                System.currentTimeMillis(), "DEATH", 0f,
                info.sourceType, info.sourceEntityName, info.sourceEntityType, info.directEntityName,
                info.damageTypeId,
                entity.getName().getString(),
                entityId(entity),
                deathMsg, posString(entity));
        store.append(entry);
        CombatLog.LOGGER.info("Combat log: death recorded -> {}", deathMsg);
    }

    public List<CombatLogEntry> getRecent() {
        return store.recent();
    }

    public SessionStore.Stats stats() {
        return store.stats();
    }

    public LogConfig config() {
        return config;
    }

    public File currentSessionFile() {
        return store.sessionFile();
    }

    public void export(File out) throws IOException {
        store.export(out);
    }

    public void flush() {
        store.flush();
    }

    public void clear() {
        store.clear();
    }

    private static boolean isLocalPlayer(Entity e, UUID me) {
        return e instanceof Player p && p.getUUID().equals(me);
    }

    private static String entityId(Entity e) {
        ResourceLocation rl = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return rl != null ? rl.toString() : e.getType().toString();
    }

    private static String posString(Entity e) {
        return String.format("%.0f,%.0f,%.0f", e.getX(), e.getY(), e.getZ());
    }
}
