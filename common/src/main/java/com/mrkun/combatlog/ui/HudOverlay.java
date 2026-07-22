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

        int padding = 6;
        int lineH = 12;
        int titleH = 14;
        int lines = 4;
        int totalH = titleH + lines * lineH + padding * 2;
        int textW = 200;
        int bgW = textW + padding * 2;

        int x = mc.getWindow().getGuiScaledWidth() - bgW - 8;
        int y = 8;

        graphics.fill(x, y, x + bgW, y + totalH, 0x80000000);
        graphics.fill(x, y, x + bgW, y + titleH, 0x80003366);
        graphics.fill(x, y + titleH - 1, x + bgW, y + titleH, 0xFF00CCFF);

        int tx = x + padding;
        int ty = y + padding;

        graphics.drawString(mc.font, "Combat Log", tx, ty, 0x00CCFF, false);

        ty += titleH;
        graphics.drawString(mc.font, "Damage Dealt: " + fmt(stats.totalDealt) + "  (" + fmt(stats.dpsDealt) + "/s)", tx, ty, 0xFFFFAA, false);
        ty += lineH;
        graphics.drawString(mc.font, "Damage Taken: " + fmt(stats.totalTaken) + "  (" + fmt(stats.dpsTaken) + "/s)", tx, ty, 0xFF6666, false);
        ty += lineH;
        graphics.drawString(mc.font, "Entries: " + stats.entries, tx, ty, 0xAAAAAA, false);

        graphics.drawString(mc.font, "[K] Open  [H] Toggle", tx, ty + lineH, 0x666666, false);
    }

    private static String fmt(float v) {
        return String.format("%.1f", v);
    }
}