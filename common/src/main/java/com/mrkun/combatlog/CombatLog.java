package com.mrkun.combatlog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 共享模块入口。平台模块（fabric/neoforge）在客户端初始化时调用 {@link #init()}。
 */
public final class CombatLog {
    public static final String MOD_ID = "combatlog";
    public static final Logger LOGGER = LoggerFactory.getLogger("CombatLog");

    private CombatLog() {
    }

    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;
        // 触发单例构建：打开会话文件、加载配置
        com.mrkun.combatlog.capture.CombatLogService.getInstance();
        LOGGER.info("Combat Damage Log initialized (client-side)");
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
