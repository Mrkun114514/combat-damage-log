package com.mrkun.combatlog.ui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 统一的 WoW 风格界面主题：圆角面板、渐变标题栏、调色板、配色方案。
 * 三个界面（日志面板 / 设置 / HUD）共用，保证视觉一致。
 */
public final class Theme {
    private Theme() {
    }

    // 基础调色板（ARGB）
    public static final int BG = 0xE012161C;          // 半透明背景
    public static final int PANEL = 0xE612161C;       // 面板底色
    public static final int PANEL_SOLID = 0xFF12161C; // 不透明面板
    public static final int BORDER = 0x803A4250;      // 边框
    public static final int HEADER_TOP = 0xFF1B2230;  // 标题渐变上
    public static final int HEADER_BOTTOM = 0xFF12161C; // 标题渐变下
    public static final int TEXT = 0xFFD8DCE0;        // 主文字
    public static final int TEXT_DIM = 0x8094A0B0;    // 次要文字
    public static final int TAB_BG = 0x40141820;
    public static final int TAB_ACTIVE = 0x8066CCFF;

    // 事件配色
    public static final int DEALT = 0xFFFFD24A;  // 造成伤害（金）
    public static final int TAKEN = 0xFFFF5566;  // 受到伤害（红）
    public static final int DEATH = 0xFFB070FF;  // 死亡（紫）

    /** 配色方案对应的强调色。 */
    public static int accent(String scheme) {
        if (scheme == null) return 0xFF66CCFF;
        return switch (scheme) {
            case "neon" -> 0xFF55FF99;
            case "mono" -> 0xFFCCCCCC;
            case "wow" -> 0xFF66CCFF;
            default -> 0xFF66CCFF;
        };
    }

    /** 绘制带圆角外观的面板（边框 + 内填充），radius 为切角半径。 */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int radius, int border, int fill) {
        round(g, x, y, w, h, radius, border);
        int i = 1;
        round(g, x + i, y + i, w - i * 2, h - i * 2, Math.max(0, radius - 1), fill);
    }

    private static void round(GuiGraphics g, int x, int y, int w, int h, int r, int c) {
        g.fill(x, y + r, x + w, y + h - r, c);
        g.fill(x + r, y, x + w - r, y + h, c);
    }

    /** 竖直渐变（逐行填充，避免依赖具体 API）。 */
    public static void vGradient(GuiGraphics g, int x, int y, int w, int h, int top, int bottom) {
        if (h <= 0) return;
        for (int i = 0; i < h; i++) {
            int c = lerp(top, bottom, (float) i / h);
            g.fill(x, y + i, x + w, y + i + 1, c);
        }
    }

    /** 线性插值两个 ARGB 颜色。 */
    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF, aa = (a >> 24) & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF, ba = (b >> 24) & 0xFF;
        int r = (int) (ar + (br - ar) * t);
        int gg = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        int al = (int) (aa + (ba - aa) * t);
        return (al << 24) | (r << 16) | (gg << 8) | bl;
    }
}
