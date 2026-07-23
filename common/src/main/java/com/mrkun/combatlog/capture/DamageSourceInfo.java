package com.mrkun.combatlog.capture;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 从 {@link DamageSource} 中稳定提取「伤害类型 id + 来源分类 + 实体信息」。
 * 该抽象屏蔽了 1.20.x 与 1.21.x 之间伤害源 API 的差异（见计划文件 §5）。
 */
public final class DamageSourceInfo {
    public final String damageTypeId;
    public final String sourceType;      // ENTITY / ENVIRONMENT / SELF / UNKNOWN
    public final String sourceEntityName;
    public final String sourceEntityType;
    public final String directEntityName;

    private DamageSourceInfo(String damageTypeId, String sourceType, String sourceEntityName,
                             String sourceEntityType, String directEntityName) {
        this.damageTypeId = damageTypeId;
        this.sourceType = sourceType;
        this.sourceEntityName = sourceEntityName;
        this.sourceEntityType = sourceEntityType;
        this.directEntityName = directEntityName;
    }

    public static DamageSourceInfo from(DamageSource source, LivingEntity target) {
        String typeId = extractTypeId(source, target);
        Entity cause = source.getEntity();
        Entity direct = source.getDirectEntity();

        String sourceType;
        if (cause != null && cause != target) {
            sourceType = "ENTITY";
        } else if (cause == target) {
            sourceType = "SELF";
        } else {
            sourceType = "ENVIRONMENT";
        }

        String causeName = cause != null ? cause.getName().getString() : "";
        String causeType = cause != null ? entityId(cause) : "";
        String directName = (direct != null && direct != cause) ? direct.getName().getString() : "";
        return new DamageSourceInfo(typeId, sourceType, causeName, causeType, directName);
    }

    private static String extractTypeId(DamageSource source, LivingEntity target) {
        DamageType type = source.type();
        // 1.20.5+：DAMAGE_TYPE 是数据驱动注册表，已不在 BuiltInRegistries 上。
        // RegistryAccess 只提供 lookup(...)（运行时对象即 Registry<DamageType>），
        // 向下转型后调用其 getKey(T) 可定位伤害类型 id（如 minecraft:fall）。
        var lookup = target.registryAccess().lookup(Registries.DAMAGE_TYPE).orElse(null);
        if (lookup instanceof Registry<DamageType> registry) {
            ResourceLocation key = registry.getKey(type);
            if (key != null) {
                return key.toString();
            }
        }
        // 回退：message id（旧版本 / 未注册类型）
        return type.msgId();
    }

    private static String entityId(Entity e) {
        ResourceLocation rl = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return rl != null ? rl.toString() : e.getType().toString();
    }
}
