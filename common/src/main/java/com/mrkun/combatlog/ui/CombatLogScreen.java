package com.mrkun.combatlog.ui;

import com.mrkun.combatlog.capture.CombatLogEntry;
import com.mrkun.combatlog.capture.CombatLogService;
import com.mrkun.combatlog.storage.SessionStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.List;

public final class CombatLogScreen extends Screen {
    private List<CombatLogEntry> entries = List.of();
    private int scroll = 0;
    private static final int PADDING = 8;
    private static final int LINE_H = 12;

    public CombatLogScreen() {
        super(Component.literal("Combat Damage Log"));
    }

    @Override
    protected void init() {
        super.init();
        this.entries = CombatLogService.getInstance().getRecent();
        this.scroll = 0;

        this.addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> {
            this.entries = CombatLogService.getInstance().getRecent();
            this.scroll = 0;
        }).bounds(this.width - 110, 8, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Export"), b -> this.export())
                .bounds(this.width - 220, 8, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("设置"), b -> Minecraft.getInstance().setScreen(new SettingsScreen(this)))
                .bounds(this.width - 330, 8, 100, 20).build());
    }

    private void export() {
        File out = new File(Minecraft.getInstance().gameDirectory,
                "combatlog/sessions/export-" + System.currentTimeMillis() + ".txt");
        try {
            CombatLogService.getInstance().export(out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 只让 super.render() 做一次背景模糊。
        // Minecraft 1.21.8 的新渲染管线每帧仅允许 blur 一次，手动再调一次会抛
        // IllegalStateException("Can only blur once per frame")。
        // 自定义面板内容放在 super 之后绘制；内容区从 y=32 起，避开顶部按钮行(y=8~28)。
        super.render(graphics, mouseX, mouseY, partialTick);

        int contentX = PADDING;
        int contentY = 32;
        int contentW = this.width - PADDING * 2;
        int contentH = this.height - contentY - PADDING * 2;

        graphics.fill(contentX, contentY, contentX + contentW, contentY + contentH, 0xCC000000);

        int titleY = PADDING;
        // 标题文字画在左侧，与右侧按钮同行但 x 不重叠，故不再铺标题栏底色以免遮挡按钮
        graphics.drawString(this.font, "Combat Damage Log", contentX + PADDING, titleY + 4, 0x00CCFF, false);
        graphics.drawString(this.font, "[Scroll] Navigate  [ESC] Close", contentX + contentW - 160, titleY + 5, 0x666666, false);

        int visible = Math.max(1, contentH / LINE_H);
        if (scroll > entries.size() - visible) scroll = Math.max(0, entries.size() - visible);
        if (scroll < 0) scroll = 0;

        int textX = contentX + PADDING;
        int textY = contentY + PADDING;

        if (entries.isEmpty()) {
            graphics.drawString(this.font, "No combat log entries yet", textX, textY + contentH / 2 - 6, 0x666666, false);
        } else {
            for (int i = 0; i < visible; i++) {
                int idx = entries.size() - 1 - scroll - i;
                if (idx < 0) break;
                CombatLogEntry e = entries.get(idx);
                graphics.drawString(this.font, SessionStore.formatLine(e), textX, textY + i * LINE_H, colorFor(e.eventType), false);
            }

            if (entries.size() > visible) {
                int scrollBarW = 4;
                int scrollBarX = contentX + contentW - scrollBarW - 2;
                int scrollBarH = contentH;
                float scrollRatio = (float) scroll / Math.max(1, entries.size() - visible);
                int thumbH = Math.max(20, (int) (scrollBarH * ((float) visible / entries.size())));
                int thumbY = contentY + (int) (scrollRatio * (scrollBarH - thumbH));

                graphics.fill(scrollBarX, contentY, scrollBarX + scrollBarW, contentY + scrollBarH, 0x40FFFFFF);
                graphics.fill(scrollBarX, thumbY, scrollBarX + scrollBarW, thumbY + thumbH, 0x80FFFFFF);
            }
        }
    }

    private static int colorFor(String eventType) {
        return switch (eventType) {
            case "DAMAGE_TAKEN" -> 0xFF6666;
            case "DAMAGE_DEALT" -> 0xFFFFAA;
            case "DEATH" -> 0xAA66FF;
            default -> 0xAAAAAA;
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        scroll += (deltaY > 0) ? -3 : 3;
        return true;
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