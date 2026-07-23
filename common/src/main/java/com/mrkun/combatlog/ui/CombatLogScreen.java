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
import java.util.ArrayList;
import java.util.List;

public final class CombatLogScreen extends Screen {
    private List<CombatLogEntry> all = List.of();
    private String filter = "ALL";   // ALL / DAMAGE_DEALT / DAMAGE_TAKEN / DEATH
    private int scroll = 0;
    private static final int PAD = 10;
    private static final int LINE_H = 15;
    private static final int HEADER_H = 30;

    private static final String[] TABS = {"ALL", "DAMAGE_DEALT", "DAMAGE_TAKEN", "DEATH"};
    private static final String[] TAB_LABEL = {"全部", "造成伤害", "受到伤害", "死亡/击杀"};

    public CombatLogScreen() {
        super(Component.literal("Combat Damage Log"));
    }

    @Override
    protected void init() {
        super.init();
        this.all = CombatLogService.getInstance().getRecent();
        this.scroll = 0;

        int btnW = 96, btnH = 20, gap = 6;
        int total = btnW * 3 + gap * 2;
        int startX = this.width - PAD - total;
        this.addRenderableWidget(Button.builder(Component.literal("刷新"), b -> refresh())
                .bounds(startX, PAD, btnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("导出"), b -> export())
                .bounds(startX + (btnW + gap), PAD, btnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("设置"),
                        b -> Minecraft.getInstance().setScreen(new SettingsScreen(this)))
                .bounds(startX + (btnW + gap) * 2, PAD, btnW, btnH).build());
    }

    private void refresh() {
        this.all = CombatLogService.getInstance().getRecent();
        this.scroll = 0;
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

    private List<CombatLogEntry> filtered() {
        if ("ALL".equals(filter)) return all;
        List<CombatLogEntry> out = new ArrayList<>();
        for (CombatLogEntry e : all) {
            if (e.eventType.equals(filter)) out.add(e);
        }
        return out;
    }

    // 面板与标题栏在背景层绘制（位于按钮等控件之下），避免遮盖控件。
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int x = PAD, y = PAD, w = this.width - PAD * 2, h = this.height - PAD * 2;
        Theme.panel(graphics, x, y, w, h, 6, Theme.BORDER, Theme.PANEL);
        Theme.vGradient(graphics, x + 2, y + 2, w - 4, HEADER_H, Theme.HEADER_TOP, Theme.HEADER_BOTTOM);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int x = PAD, y = PAD, w = this.width - PAD * 2, h = this.height - PAD * 2;
        int accent = Theme.accent(CombatLogService.getInstance().config().colorScheme);

        graphics.drawString(this.font, "战斗伤害日志", x + 12, y + 9, accent, false);
        graphics.drawString(this.font, "[滚轮] 浏览  [ESC] 关闭",
                x + w - 150, y + 10, Theme.TEXT_DIM, false);

        // 筛选标签
        int tabY = y + HEADER_H + 6;
        int tabX = x + 12;
        int tabW = 88, tabH = 20, tabGap = 6;
        for (int i = 0; i < TABS.length; i++) {
            int tx = tabX + i * (tabW + tabGap);
            boolean active = TABS[i].equals(filter);
            graphics.fill(tx, tabY, tx + tabW, tabY + tabH, active ? ctxSchemeBlend(accent) : Theme.TAB_BG);
            graphics.fill(tx, tabY + tabH - 2, tx + tabW, tabY + tabH, active ? accent : Theme.BORDER);
            graphics.drawString(this.font, TAB_LABEL[i], tx + 10, tabY + 6,
                    active ? Theme.TEXT : Theme.TEXT_DIM, false);
        }

        // 内容区
        int contentY = tabY + tabH + 8;
        int contentX = x + 12;
        int contentW = w - 24;
        int contentH = y + h - contentY - PAD;

        List<CombatLogEntry> list = filtered();
        int visible = Math.max(1, contentH / LINE_H);
        if (scroll > list.size() - visible) scroll = Math.max(0, list.size() - visible);
        if (scroll < 0) scroll = 0;

        if (list.isEmpty()) {
            graphics.drawString(this.font, "暂无符合条件的战斗记录",
                    contentX, contentY + contentH / 2 - 4, Theme.TEXT_DIM, false);
        } else {
            for (int i = 0; i < visible; i++) {
                int idx = list.size() - 1 - scroll - i;
                if (idx < 0) break;
                CombatLogEntry e = list.get(idx);
                graphics.drawString(this.font, SessionStore.formatLine(e),
                        contentX, contentY + i * LINE_H, colorFor(e.eventType), false);
            }
            if (list.size() > visible) {
                int barW = 4;
                int barX = x + w - PAD - barW;
                float ratio = (float) scroll / Math.max(1, list.size() - visible);
                int thumbH = Math.max(20, (int) (contentH * ((float) visible / list.size())));
                int thumbY = contentY + (int) (ratio * (contentH - thumbH));
                graphics.fill(barX, contentY, barX + barW, contentY + contentH, 0x40FFFFFF);
                graphics.fill(barX, thumbY, barW + barX, thumbY + thumbH, 0x80FFFFFF);
            }
        }

        // 底部状态栏
        String status = String.format("显示 %d / 共 %d 条  筛选: %s",
                Math.min(visible, list.size()), all.size(), labelOf(filter));
        graphics.drawString(this.font, status, x + 12, y + h - 16, Theme.TEXT_DIM, false);
    }

    private static int ctxSchemeBlend(int accent) {
        return (0x30 << 24) | (accent & 0x00FFFFFF);
    }

    private static String labelOf(String f) {
        for (int i = 0; i < TABS.length; i++) if (TABS[i].equals(f)) return TAB_LABEL[i];
        return "全部";
    }

    private static int colorFor(String eventType) {
        return switch (eventType) {
            case "DAMAGE_TAKEN" -> Theme.TAKEN;
            case "DAMAGE_DEALT" -> Theme.DEALT;
            case "DEATH" -> Theme.DEATH;
            default -> Theme.TEXT;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int y = PAD, x = PAD;
        int tabY = y + HEADER_H + 6;
        int tabX = x + 12;
        int tabW = 88, tabH = 20, tabGap = 6;
        for (int i = 0; i < TABS.length; i++) {
            int tx = tabX + i * (tabW + tabGap);
            if (mouseX >= tx && mouseX <= tx + tabW && mouseY >= tabY && mouseY <= tabY + tabH) {
                filter = TABS[i];
                scroll = 0;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
