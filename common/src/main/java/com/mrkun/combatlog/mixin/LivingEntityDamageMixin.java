package com.mrkun.combatlog.mixin;

import com.mrkun.combatlog.capture.CombatLogService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 共享伤害捕获 mixin（Fabric / NeoForge 共用）。
 *
 * <p>1.21.2+ 之后，实体伤害入口被拆分：{@code Entity.hurt(DamageSource,float)} 在客户端
 * 变成空壳（只在 {@code ServerLevel} 时转发到 {@code hurtServer}），伤害数值仅存在于
 * 服务端的 {@code LivingEntity.hurtServer(ServerLevel,DamageSource,float)}。因此这里改为
 * 注入 {@code hurtServer}：单人局的集成服务端与客户端同处一个 JVM，可拿到全部伤害数值；
 * {@code CombatLogService} 内部再用 {@code Minecraft.getInstance().player} 过滤为玩家相关事件。</p>
 *
 * <p>已知局限：纯客户端连接远程多人服时，{@code hurtServer} 不在本地运行，此路径捕获不到；
 * 多人局的伤害捕获需后续用血量差分（{@code ClientboundSetHealthPacket} / 实体血量同步）补齐。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

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

    @Inject(method = "die", at = @At("HEAD"))
    private void combatlog$onDie(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        CombatLogService.getInstance().onDeath(self, source);
    }
}
