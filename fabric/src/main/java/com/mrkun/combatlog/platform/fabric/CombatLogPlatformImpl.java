package com.mrkun.combatlog.platform.fabric;

import com.mrkun.combatlog.CombatLog;
import com.mrkun.combatlog.capture.ClientDamageTracker;
import com.mrkun.combatlog.capture.CombatLogService;
import com.mrkun.combatlog.platform.CombatLogPlatform;
import com.mrkun.combatlog.ui.CombatLogScreen;
import com.mrkun.combatlog.ui.DamageMeterScreen;
import com.mrkun.combatlog.ui.HudOverlay;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/**
 * {@link CombatLogPlatform} 的 Fabric 实现。
 * 包名必须是 common 包 + ".fabric"、类名必须是 Common 类 + "Impl"，@ExpectPlatform 才能注入。
 */
public final class CombatLogPlatformImpl {
    private static KeyMapping openLogKey;
    private static KeyMapping toggleHudKey;
    private static KeyMapping openMeterKey;

    public static void init() {
        CombatLog.init();
        registerHud();
        registerKeybindings();
    }

    public static void registerHud() {
        // 1.21.x：HudRenderCallback 第二参从 float 改为 DeltaTracker，需取出 partial tick
        HudRenderCallback.EVENT.register((graphics, deltaTracker) ->
                HudOverlay.render(graphics, deltaTracker.getRealtimeDeltaTicks()));
    }

    public static void registerKeybindings() {
        openLogKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.combatlog.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.category.combatlog"));
        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.combatlog.togglehud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.category.combatlog"));
        openMeterKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.combatlog.meter", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.category.combatlog"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openLogKey.consumeClick()) CombatLogPlatform.openLogScreen();
            while (toggleHudKey.consumeClick()) CombatLogPlatform.toggleHud();
            while (openMeterKey.consumeClick()) CombatLogPlatform.openMeterScreen();
            // 多人服伤害捕获：结算 handleDamageEvent 事件的血量差分
            ClientDamageTracker.getInstance().tick();
        });
    }

    public static void openLogScreen() {
        Minecraft.getInstance().setScreen(new CombatLogScreen());
    }

    public static void toggleHud() {
        CombatLogService svc = CombatLogService.getInstance();
        svc.config().enableHud = !svc.config().enableHud;
        svc.config().save();
    }

    public static void openMeterScreen() {
        Minecraft.getInstance().setScreen(new DamageMeterScreen(null));
    }
}
