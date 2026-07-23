package com.mrkun.combatlog.mixin;

import com.mrkun.combatlog.capture.ClientDamageTracker;
import com.mrkun.combatlog.capture.CombatLogService;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 伤害捕获 mixin（Fabric / NeoForge 共用，仅在客户端环境应用，见 combatlog.mixins.json 的 client 段）。
 *
 * <p>双路径捕获：</p>
 * <ul>
 *   <li><b>单人局</b>：集成服务端与客户端同 JVM，注入
 *       {@code LivingEntity.hurtServer(ServerLevel,DamageSource,float)} 可拿到精确伤害数值。</li>
 *   <li><b>多人服（远程服务器）</b>：{@code hurtServer} 只在服务端运行，本地不会触发。
 *       改为注入 {@code handleDamageEvent(DamageSource)}——1.19.4+ 服务器通过
 *       {@code ClientboundDamageEventPacket} 向客户端广播的伤害事件入口。该包不含伤害数值，
 *       由 {@link ClientDamageTracker} 用血量差分补齐后再落盘。</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    /** 单人局路径：集成服务端内可拿到精确伤害值。 */
    @Inject(
            method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("RETURN")
    )
    private void combatlog$onDamage(ServerLevel level, DamageSource source, float amount,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        CombatLogService.getInstance().onDamage(self, source, amount);
    }

    /** 多人服路径：客户端收到服务器广播的伤害事件（无数值，交由血量差分结算）。 */
    @Inject(method = "handleDamageEvent", at = @At("HEAD"))
    private void combatlog$onClientDamageEvent(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level() != null && self.level().isClientSide) {
            ClientDamageTracker.getInstance().onDamageEvent(self, source);
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void combatlog$onDie(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        // 单人局中客户端侧的 die（实体事件 3 触发）与服务端侧的 die 会各跑一次，
        // 服务端路径已记录，跳过客户端侧调用避免重复；远程服则只有客户端路径。
        if (self.level() != null && self.level().isClientSide
                && Minecraft.getInstance().hasSingleplayerServer()) {
            return;
        }
        CombatLogService.getInstance().onDeath(self, source);
    }
}
