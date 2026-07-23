package com.mrkun.combatlog.ui;

import com.mrkun.combatlog.capture.CombatLogService;
import com.mrkun.combatlog.storage.LogConfig;
import com.mrkun.combatlog.storage.SessionStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class HudOverlay {
    private HudOverlay() {
    }

    public static final int PADDING = 8;
    public static final int LINE_H = 13;
    public static final int TITLE_H = 16;
    public static final int LINES = 4;
    public static final int BG_W = 210;

    /** 计算面板尺寸（宽、高）。供实时 HUD 与位置编辑器复用。 */
    public static int[] panelSize() {
        int totalH = TITLE_H + LINES * LINE_H + PADDING * 2;
        return new int[]{BG_W, totalH};
    }

    /**
     * 解析面板左上角坐标。hudX/hudY < 0 时回退到「贴右上角」；
     * 任意情况下都夹紧在屏幕内，避免被移出可视区域。
     */
    public static int[] resolvePos(LogConfig config) {
        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int[] size = panelSize();
        int x, y;
        if (config.hudX >= 0 && config.hudY >= 0) {
            x = config.hudX;
            y = config.hudY;
        } else {
            x = sw - BG_W - 8;
            y = 8;
        }
        x = Math.max(0, Math.min(x, sw - size[0]));
        y = Math.max(0, Math.min(y, sh - size[1]));
        return new int[]{x, y};
    }

    public static void render(GuiGraphics graphics, float tickDelta) {
        CombatLogService svc = CombatLogService.getInstance();
        if (!svc.config().enableHud) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        int[] pos = resolvePos(svc.config());
        drawPanel(graphics, pos[0], pos[1], svc.config(), svc.stats());
    }

    /** 在指定坐标绘制 HUD 面板（实时显示与位置编辑器共用同一绘制逻辑）。 */
    public static void drawPanel(GuiGraphics graphics, int x, int y, LogConfig config, SessionStore.Stats stats) {
        int accent = Theme.accent(config.colorScheme);
        int[] size = panelSize();
        int bgW = size[0], totalH = size[1];

        Theme.panel(graphics, x, y, bgW, totalH, 5, Theme.BORDER, Theme.PANEL);
        // 标题栏
        Theme.vGradient(graphics, x + 1, y + 1, bgW - 2, TITLE_H, Theme.HEADER_TOP, Theme.HEADER_BOTTOM);
        graphics.fill(x + 1, y + TITLE_H, x + bgW - 1, y + TITLE_H + 1, accent);

        int tx = x + PADDING;
        int ty = y + PADDING;

        graphics.drawString(Minecraft.getInstance().font, "战斗日志", tx, ty, accent, false);

        ty += TITLE_H;
        graphics.drawString(Minecraft.getInstance().font, "造成伤害: " + fmt(stats.totalDealt) + "  (" + fmt(stats.dpsDealt) + "/s)",
                tx, ty, Theme.DEALT, false);
        ty += LINE_H;
        graphics.drawString(Minecraft.getInstance().font, "受到伤害: " + fmt(stats.totalTaken) + "  (" + fmt(stats.dpsTaken) + "/s)",
                tx, ty, Theme.TAKEN, false);
        ty += LINE_H;
        graphics.drawString(Minecraft.getInstance().font, "记录数: " + stats.entries, tx, ty, Theme.TEXT, false);

        graphics.drawString(Minecraft.getInstance().font, "[K] 面板  [H] HUD  [M] 统计",
                tx, ty + LINE_H, Theme.TEXT_DIM, false);
    }

    private static String fmt(float v) {
        return String.format("%.1f", v);
    }
}
