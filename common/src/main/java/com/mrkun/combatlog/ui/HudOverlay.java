package com.mrkun.combatlog.ui;

import com.mrkun.combatlog.capture.CombatLogService;
import com.mrkun.combatlog.storage.SessionStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class HudOverlay {
    private HudOverlay() {
    }

    public static void render(GuiGraphics graphics, float tickDelta) {
        CombatLogService svc = CombatLogService.getInstance();
        if (!svc.config().enableHud) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        SessionStore.Stats stats = svc.stats();
        int accent = Theme.accent(svc.config().colorScheme);

        int padding = 8;
        int lineH = 13;
        int titleH = 16;
        int lines = 4;
        int totalH = titleH + lines * lineH + padding * 2;
        int bgW = 210;

        int x = mc.getWindow().getGuiScaledWidth() - bgW - 8;
        int y = 8;

        Theme.panel(graphics, x, y, bgW, totalH, 5, Theme.BORDER, Theme.PANEL);
        // 标题栏
        Theme.vGradient(graphics, x + 1, y + 1, bgW - 2, titleH, Theme.HEADER_TOP, Theme.HEADER_BOTTOM);
        graphics.fill(x + 1, y + titleH, x + bgW - 1, y + titleH + 1, accent);

        int tx = x + padding;
        int ty = y + padding;

        graphics.drawString(mc.font, "战斗日志", tx, ty, accent, false);

        ty += titleH;
        graphics.drawString(mc.font, "造成伤害: " + fmt(stats.totalDealt) + "  (" + fmt(stats.dpsDealt) + "/s)",
                tx, ty, Theme.DEALT, false);
        ty += lineH;
        graphics.drawString(mc.font, "受到伤害: " + fmt(stats.totalTaken) + "  (" + fmt(stats.dpsTaken) + "/s)",
                tx, ty, Theme.TAKEN, false);
        ty += lineH;
        graphics.drawString(mc.font, "记录数: " + stats.entries, tx, ty, Theme.TEXT, false);

        graphics.drawString(mc.font, "[K] 面板  [H] HUD", tx, ty + lineH, Theme.TEXT_DIM, false);
    }

    private static String fmt(float v) {
        return String.format("%.1f", v);
    }
}
