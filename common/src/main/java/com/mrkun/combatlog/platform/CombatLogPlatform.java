package com.mrkun.combatlog.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;

/**
 * 平台相关能力的抽象。方法签名在 common 中声明，实现位于
 * fabric 与 neoforge 子模块的对应 {@code ...platform.CombatLogPlatformImpl} 中。
 */
public final class CombatLogPlatform {
    private CombatLogPlatform() {
    }

    @ExpectPlatform
    public static void init() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerHud() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerKeybindings() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void openLogScreen() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void toggleHud() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void openMeterScreen() {
        throw new AssertionError();
    }
}
