package com.mrkun.combatlog.capture;

import net.minecraft.client.Minecraft;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 多人服务器（远程服）下的客户端伤害捕获。
 *
 * <p>背景：{@code LivingEntity.hurtServer} 只在服务端执行，连接远程服务器时本地客户端
 * 永远不会触发，导致模组在多人服上完全无数据。1.19.4+ 服务器会通过
 * {@code ClientboundDamageEventPacket} 向客户端广播伤害事件（最终调用
 * {@code LivingEntity.handleDamageEvent}），但该包<b>不含伤害数值</b>。</p>
 *
 * <p>做法：mixin 在 {@code handleDamageEvent} 处上报事件，本类记录该实体
 * “事件前血量基线”（取上一 tick 快照，避免血量同步包先于伤害包到达导致差值为 0），
 * 在随后的客户端 tick 中用 <i>基线 - 当前血量</i>（含伤害吸收）差分出伤害值，
 * 再交给 {@link CombatLogService#onDamage} 走统一过滤与落盘。</p>
 *
 * <p>单人局不会走此路径（由 {@code hurtServer} 服务端路径捕获，避免重复记录）。</p>
 */
public final class ClientDamageTracker {
    private static final ClientDamageTracker INSTANCE = new ClientDamageTracker();

    public static ClientDamageTracker getInstance() {
        return INSTANCE;
    }

    /** 血量差分的最长等待 tick 数（超时则按已观测差值结算）。 */
    private static final int RESOLVE_TIMEOUT_TICKS = 3;

    /** 上一 tick 各可见生物的有效血量快照（生命值 + 伤害吸收）。 */
    private final Map<Integer, Float> lastHealth = new HashMap<>();
    /** 等待血量差分结算的伤害事件。 */
    private final List<Pending> pending = new ArrayList<>();

    private ClientDamageTracker() {
    }

    /** 由 mixin 在客户端 {@code handleDamageEvent} 时调用。 */
    public void onDamageEvent(LivingEntity entity, DamageSource source) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        // 单人局：hurtServer 路径已捕获完整数据（含精确伤害值），此处直接跳过防止重复。
        if (mc.hasSingleplayerServer()) return;
        float baseline = lastHealth.getOrDefault(entity.getId(), effectiveHealth(entity));
        pending.add(new Pending(entity, source, baseline));
    }

    /** 每客户端 tick 末调用：结算挂起事件并刷新血量快照。 */
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            // 退出世界 / 断开连接时清空状态
            pending.clear();
            lastHealth.clear();
            return;
        }

        // 1) 结算挂起的伤害事件：血量已下降或超时
        Iterator<Pending> it = pending.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            float cur = p.entity.isRemoved() ? 0f : effectiveHealth(p.entity);
            p.ticksLeft--;
            if (cur < p.baseline || p.ticksLeft <= 0) {
                float amount = Math.max(0f, p.baseline - cur);
                // 差值为 0 通常是服务端插件屏蔽了血量同步或伤害被完全格挡，跳过避免刷 0 伤害噪音
                if (amount > 0f) {
                    CombatLogService.getInstance().onDamage(p.entity, p.source, amount);
                }
                it.remove();
            }
        }

        // 2) 刷新可见生物的血量快照，作为下一次事件的基线
        lastHealth.clear();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof LivingEntity le) {
                lastHealth.put(le.getId(), effectiveHealth(le));
            }
        }
    }

    /** 有效血量 = 生命值 + 伤害吸收（金心），确保吸收盾扣减也计入伤害。 */
    private static float effectiveHealth(LivingEntity e) {
        return e.getHealth() + e.getAbsorptionAmount();
    }

    private static final class Pending {
        final LivingEntity entity;
        final DamageSource source;
        final float baseline;
        int ticksLeft = RESOLVE_TIMEOUT_TICKS;

        Pending(LivingEntity entity, DamageSource source, float baseline) {
            this.entity = entity;
            this.source = source;
            this.baseline = baseline;
        }
    }
}
