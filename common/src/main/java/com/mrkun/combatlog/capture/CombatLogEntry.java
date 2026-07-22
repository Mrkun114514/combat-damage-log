package com.mrkun.combatlog.capture;

/**
 * 一条战斗日志事件（WoW 风格）。字段对 Gson 友好（全部 public），
 * 序列化后每行一条写入 {@code .jsonl} 会话文件。
 *
 * <p>eventType：{@code DAMAGE_DEALT} / {@code DAMAGE_TAKEN} / {@code DEATH}
 * <p>sourceType：{@code ENTITY} / {@code ENVIRONMENT} / {@code SELF} / {@code UNKNOWN}
 */
public class CombatLogEntry {
    public final long timestamp;
    public final String eventType;
    public final float amount;
    public final String sourceType;
    public final String sourceEntityName;
    public final String sourceEntityType; // 注册表 id，如 minecraft:zombie
    public final String directEntityName; // 直接攻击者（如射出的箭）
    public final String damageTypeId;     // minecraft:mob / minecraft:fall ...
    public final String targetEntityName;
    public final String targetEntityType;
    public final String deathMessage;
    public final String pos;

    public CombatLogEntry(long timestamp, String eventType, float amount,
                          String sourceType, String sourceEntityName, String sourceEntityType,
                          String directEntityName, String damageTypeId,
                          String targetEntityName, String targetEntityType,
                          String deathMessage, String pos) {
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.amount = amount;
        this.sourceType = sourceType;
        this.sourceEntityName = sourceEntityName;
        this.sourceEntityType = sourceEntityType;
        this.directEntityName = directEntityName;
        this.damageTypeId = damageTypeId;
        this.targetEntityName = targetEntityName;
        this.targetEntityType = targetEntityType;
        this.deathMessage = deathMessage;
        this.pos = pos;
    }
}
