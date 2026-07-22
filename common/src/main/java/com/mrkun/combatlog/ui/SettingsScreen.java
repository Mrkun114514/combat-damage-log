package com.mrkun.combatlog.ui;

import com.mrkun.combatlog.capture.CombatLogService;
import com.mrkun.combatlog.storage.LogConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * 战斗日志设置界面（无外部依赖，纯 Minecraft GUI）。
 * 既可从 K 日志面板的「设置」按钮打开，也可从 Mod Menu 的齿轮按钮打开。
 */
public final class SettingsScreen extends Screen {
    private final Screen parent;
    private final LogConfig config = CombatLogService.getInstance().config();

    private Button hudButton;
    private Button bufferLabel;

    public SettingsScreen(Screen parent) {
        super(Component.literal("Combat Log 设置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int y = 44;

        // HUD 显示开关
        hudButton = Button.builder(Component.literal(hudText()), b -> {
            config.enableHud = !config.enableHud;
            b.setMessage(Component.literal(hudText()));
        }).bounds(cx - 100, y, 200, 20).build();
        this.addRenderableWidget(hudButton);
        y += 28;

        // 缓冲上限：− / 数值 / +
        this.addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            config.maxBufferSize = Math.max(100, config.maxBufferSize - 500);
            bufferLabel.setMessage(Component.literal(bufferText()));
        }).bounds(cx - 100, y, 40, 20).build());
        bufferLabel = Button.builder(Component.literal(bufferText()), b -> { }).bounds(cx - 55, y, 110, 20).build();
        this.addRenderableWidget(bufferLabel);
        this.addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            config.maxBufferSize = Math.min(10000, config.maxBufferSize + 500);
            bufferLabel.setMessage(Component.literal(bufferText()));
        }).bounds(cx + 60, y, 40, 20).build());
        y += 28;

        // 清空当前记录
        this.addRenderableWidget(Button.builder(Component.literal("清空当前记录"), b -> CombatLogService.getInstance().clear())
                .bounds(cx - 100, y, 200, 20).build());
        y += 36;

        // 保存并关闭
        this.addRenderableWidget(Button.builder(Component.literal("保存并关闭"), b -> this.saveAndClose())
                .bounds(cx - 100, this.height - 40, 200, 20).build());
    }

    private String hudText() {
        return "HUD 显示: " + (config.enableHud ? "开" : "关");
    }

    private String bufferText() {
        return "缓冲上限: " + config.maxBufferSize;
    }

    private void saveAndClose() {
        config.save();
        this.onClose();
    }

    @Override
    public void onClose() {
        config.save();
        if (parent != null) Minecraft.getInstance().setScreen(parent);
        else super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 只让 super.render() 做一次背景模糊（1.21.8 每帧仅允许 blur 一次）
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        graphics.drawString(this.font, "Combat Damage Log 设置", cx - 95, 16, 0x00CCFF, false);
        graphics.drawString(this.font, "[ESC] 返回", cx + 50, 16, 0x666666, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
