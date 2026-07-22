package com.mrkun.combatlog.platform.neoforge;

import com.mrkun.combatlog.CombatLog;
import com.mrkun.combatlog.capture.CombatLogService;
import com.mrkun.combatlog.platform.CombatLogPlatform;
import com.mrkun.combatlog.ui.CombatLogScreen;
import com.mrkun.combatlog.ui.HudOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/**
 * {@link CombatLogPlatform} 的 NeoForge 实现。
 * 包名必须是 common 包 + ".neoforge"、类名必须是 Common 类 + "Impl"，@ExpectPlatform 才能注入。
 */
public final class CombatLogPlatformImpl {
    public static final KeyMapping OPEN_LOG_KEY = new KeyMapping(
            "key.combatlog.open", KeyConflictContext.IN_GAME, GLFW.GLFW_KEY_K, "key.category.combatlog");
    public static final KeyMapping TOGGLE_HUD_KEY = new KeyMapping(
            "key.combatlog.togglehud", KeyConflictContext.IN_GAME, GLFW.GLFW_KEY_H, "key.category.combatlog");

    public static void init() {
        CombatLog.init();
        registerHud();
        registerKeybindings();
    }

    public static void registerHud() {
        MinecraftForge.EVENT_BUS.register(new HudRenderer());
    }

    public static void registerKeybindings() {
        MinecraftForge.EVENT_BUS.register(new KeyHandler());
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_LOG_KEY);
        event.register(TOGGLE_HUD_KEY);
    }

    public static void openLogScreen() {
        Minecraft.getInstance().setScreen(new CombatLogScreen());
    }

    public static void toggleHud() {
        CombatLogService svc = CombatLogService.getInstance();
        svc.config().enableHud = !svc.config().enableHud;
        svc.config().save();
    }

    public static class HudRenderer {
        @SubscribeEvent
        public void onRenderGui(RenderGuiEvent.Post e) {
            HudOverlay.render(e.getGuiGraphics(), 0f);
        }
    }

    public static class KeyHandler {
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (OPEN_LOG_KEY.consumeClick()) CombatLogPlatform.openLogScreen();
            if (TOGGLE_HUD_KEY.consumeClick()) CombatLogPlatform.toggleHud();
        }
    }
}
