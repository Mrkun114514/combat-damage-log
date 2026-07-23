package com.mrkun.combatlog.ui;

import com.mrkun.combatlog.capture.CombatLogService;
import com.mrkun.combatlog.storage.LogConfig;
import com.mrkun.combatlog.storage.SessionStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * WoW 风格伤害统计面板（参考 Recount / Skada）。
 * 两个维度：
 *  - 伤害来源：谁打了多少伤害，并拆分到各个被打的目标；
 *  - 受伤目标：哪个实体受伤了，并拆分来自各个来源的伤害。
 * 行内条形按比例绘制，按总伤害降序排列。
 */
public final class DamageMeterScreen extends Screen {
    private final Screen parent;
    private final LogConfig config = CombatLogService.getInstance().config();

    private enum Tab { SOURCE, TARGET }
    private Tab tab = Tab.SOURCE;

    private Button sourceTabBtn, targetTabBtn, posBtn, refreshBtn, closeBtn;
    private int scroll = 0;
    private final int[] rowHeights = new int[64];

    public DamageMeterScreen(Screen parent) {
        super(Component.literal("伤害统计"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int tabW = 130, bw = 90, bh = 22, gap = 8;
        int tx = 14;
        sourceTabBtn = Button.builder(Component.literal("伤害来源"), b -> { tab = Tab.SOURCE; scroll = 0; })
                .bounds(tx, 10, tabW, bh).build();
        targetTabBtn = Button.builder(Component.literal("受伤目标"), b -> { tab = Tab.TARGET; scroll = 0; })
                .bounds(tx + tabW + gap, 10, tabW, bh).build();
        int rightX = this.width - 14 - bw;
        refreshBtn = Button.builder(Component.literal("刷新"), b -> {})  // 数据实时读取，刷新即重绘
                .bounds(rightX - bw - gap, 10, bw, bh).build();
        posBtn = Button.builder(Component.literal("HUD位置"), b -> openPos())
                .bounds(rightX - (bw + gap) * 2, 10, bw, bh).build();
        closeBtn = Button.builder(Component.literal("关闭"), b -> onClose())
                .bounds(rightX, 10, bw, bh).build();
        this.addRenderableWidget(sourceTabBtn);
        this.addRenderableWidget(targetTabBtn);
        this.addRenderableWidget(posBtn);
        this.addRenderableWidget(refreshBtn);
        this.addRenderableWidget(closeBtn);
    }

    private void openPos() {
        Minecraft.getInstance().setScreen(new HudPositionScreen(this));
    }

    private String playerName() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null ? mc.player.getName().getString() : "";
    }

    private String label(String name) {
        return name != null && name.equals(playerName()) ? "你" : name;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xCC0E1116);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int accent = Theme.accent(config.colorScheme);
        int top = 44;
        int left = 14, right = this.width - 14;
        int contentW = right - left;

        SessionStore store = CombatLogService.getInstance().store();
        SessionStore.Meter meter = store.meter();
        SessionStore.Stats stats = store.stats();
        float dps = tab == Tab.SOURCE ? stats.dpsDealt : stats.dpsTaken;
        float grand = tab == Tab.SOURCE ? meter.totalDealt : meter.totalTaken;
        List<SessionStore.Breakdown> rows = tab == Tab.SOURCE ? meter.bySource : meter.byTarget;

        // 标题与汇总
        String title = tab == Tab.SOURCE ? "伤害来源（谁打的）" : "受伤目标（谁被打）";
        graphics.drawString(this.font, title, left, top, accent, false);
        String summary = String.format("总伤害: %.1f    %.1f/s    记录 %d 条",
                grand, dps, stats.entries);
        graphics.drawString(this.font, summary, left, top + 14, Theme.TEXT_DIM, false);

        int headerH = 36;
        int listTop = top + headerH;
        int listBottom = this.height - 44;
        int viewportH = listBottom - listTop;

        // 行高
        int maxRows = Math.min(rows.size(), rowHeights.length);
        int contentH = 0;
        int barColor = tab == Tab.SOURCE ? Theme.DEALT : Theme.TAKEN;
        for (int i = 0; i < maxRows; i++) {
            int sh = 20 + rows.get(i).split.size() * 12;
            rowHeights[i] = sh;
            contentH += sh + 4;
        }
        int maxScroll = Math.max(0, contentH - viewportH);
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;

        int y = listTop - scroll;
        int rankColor = Theme.TEXT_DIM;
        for (int i = 0; i < maxRows; i++) {
            SessionStore.Breakdown b = rows.get(i);
            int rh = rowHeights[i];
            if (y + rh >= listTop && y <= listBottom) {
                int rowBg = (i % 2 == 0) ? 0x14000000 : 0x08000000;
                graphics.fill(left, y, right, y + rh, rowBg);
                // 排名
                graphics.drawString(this.font, String.valueOf(i + 1), left + 2, y + 4, rankColor, false);
                // 名称
                String nm = label(b.name);
                graphics.drawString(this.font, nm, left + 22, y + 4, Theme.TEXT, false);
                // 总伤害 + 命中
                String val = String.format("%.1f  (%d 击)", b.total, b.hits);
                graphics.drawString(this.font, val, right - this.font.width(val) - 2, y + 4, barColor, false);
                // 条形
                float frac = grand > 0 ? b.total / grand : 0;
                int barMax = contentW - 24;
                int barW = (int) (barMax * frac);
                graphics.fill(left + 22, y + 16, left + 22 + barW, y + 18, barColor);
                // 拆分明细
                int sy = y + 20;
                for (SessionStore.Contrib c : b.split) {
                    String other = tab == Tab.SOURCE ? "→ " + label(c.name) : "← " + label(c.name);
                    String cv = String.format("%.1f", c.amount);
                    graphics.drawString(this.font, other, left + 34, sy, Theme.TEXT_DIM, false);
                    graphics.drawString(this.font, cv, right - this.font.width(cv) - 2, sy, Theme.TEXT_DIM, false);
                    sy += 12;
                }
            }
            y += rh + 4;
        }

        if (rows.isEmpty()) {
            graphics.drawString(this.font, "暂无伤害数据，先去打几只怪吧~", left + 22, listTop + 20, Theme.TEXT_DIM, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double deltaY) {
        scroll -= (int) (deltaY * 18);
        return true;
    }

    @Override
    public void onClose() {
        if (parent != null) Minecraft.getInstance().setScreen(parent);
        else super.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_M) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
