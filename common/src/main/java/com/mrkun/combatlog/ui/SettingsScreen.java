package com.mrkun.combatlog.ui;

import com.mrkun.combatlog.capture.CombatLogService;
import com.mrkun.combatlog.storage.LogConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * 战斗日志设置菜单（无外部依赖，纯 Minecraft GUI）。
 * 左侧分类导航 + 右侧控件；既可从 K 日志面板的「设置」打开，也可从 Mod Menu 齿轮打开。
 */
public final class SettingsScreen extends Screen {
    private final Screen parent;
    private final LogConfig config = CombatLogService.getInstance().config();

    private static final String[] CATS = {"显示", "数据", "关于"};
    private static final int PAD = 10;
    private static final int HEADER_H = 30;
    private static final int SIDEBAR_W = 120;
    private int selected = 0;

    private Button hudBtn;
    private Button schemeBtn;
    private Button posBtn;
    private Button bufMinus, bufPlus, bufLabel;
    private Button clearBtn;

    private static final String[] SCHEMES = {"wow", "neon", "mono"};
    private static final String[] SCHEME_LABEL = {"魔兽风格", "霓虹", "单色"};

    public SettingsScreen(Screen parent) {
        super(Component.literal("Combat Log 设置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int x = PAD, y = PAD, w = this.width - PAD * 2, h = this.height - PAD * 2;

        // 左侧分类导航
        int catY = y + HEADER_H + 8;
        for (int i = 0; i < CATS.length; i++) {
            final int idx = i;
            this.addRenderableWidget(Button.builder(Component.literal(CATS[i]), b -> {
                selected = idx;
                updateVisibility();
            }).bounds(x + 10, catY + i * 30, SIDEBAR_W - 16, 24).build());
        }

        int rx = x + SIDEBAR_W + 12;
        int rw = w - SIDEBAR_W - 36;
        int ry = y + HEADER_H + 12;

        // —— 显示 ——
        hudBtn = Button.builder(Component.literal(hudText()), b -> {
            config.enableHud = !config.enableHud;
            b.setMessage(Component.literal(hudText()));
        }).bounds(rx, ry, rw, 22).build();

        schemeBtn = Button.builder(Component.literal(schemeText()), b -> {
            int i = indexOfScheme(config.colorScheme);
            config.colorScheme = SCHEMES[(i + 1) % SCHEMES.length];
            b.setMessage(Component.literal(schemeText()));
        }).bounds(rx, ry + 64, rw, 22).build();

        posBtn = Button.builder(Component.literal("调整 HUD 位置…"), b -> {
            Minecraft.getInstance().setScreen(new HudPositionScreen(this));
        }).bounds(rx, ry + 96, rw, 22).build();

        // —— 数据 ——
        bufMinus = Button.builder(Component.literal("-"), b -> {
            config.maxBufferSize = Math.max(100, config.maxBufferSize - 500);
            bufLabel.setMessage(Component.literal(bufText()));
        }).bounds(rx, ry + 30, 40, 22).build();
        bufLabel = Button.builder(Component.literal(bufText()), b -> { }).bounds(rx + 48, ry + 30, rw - 96, 22).build();
        bufPlus = Button.builder(Component.literal("+"), b -> {
            config.maxBufferSize = Math.min(10000, config.maxBufferSize + 500);
            bufLabel.setMessage(Component.literal(bufText()));
        }).bounds(rx + rw - 40, ry + 30, 40, 22).build();

        clearBtn = Button.builder(Component.literal("清空当前记录"), b -> CombatLogService.getInstance().clear())
                .bounds(rx, ry + 72, rw, 22).build();

        this.addRenderableWidget(hudBtn);
        this.addRenderableWidget(schemeBtn);
        this.addRenderableWidget(posBtn);
        this.addRenderableWidget(bufMinus);
        this.addRenderableWidget(bufLabel);
        this.addRenderableWidget(bufPlus);
        this.addRenderableWidget(clearBtn);

        // 底部保存并关闭
        this.addRenderableWidget(Button.builder(Component.literal("保存并关闭"), b -> saveAndClose())
                .bounds(this.width / 2 - 100, this.height - 36, 200, 22).build());

        updateVisibility();
    }

    private void updateVisibility() {
        // 本版本 Button 没有 setVisible，改用 removeWidget / addRenderableWidget 切换。
        AbstractWidget[] all = {hudBtn, schemeBtn, posBtn, bufMinus, bufLabel, bufPlus, clearBtn};
        for (AbstractWidget w : all) this.removeWidget(w);
        if (selected == 0) {
            this.addRenderableWidget(hudBtn);
            this.addRenderableWidget(schemeBtn);
            this.addRenderableWidget(posBtn);
        } else if (selected == 1) {
            this.addRenderableWidget(bufMinus);
            this.addRenderableWidget(bufLabel);
            this.addRenderableWidget(bufPlus);
            this.addRenderableWidget(clearBtn);
        }
    }

    private String hudText() {
        return "HUD 显示: " + (config.enableHud ? "开" : "关");
    }

    private String schemeText() {
        return "配色方案: " + SCHEME_LABEL[indexOfScheme(config.colorScheme)];
    }

    private String bufText() {
        return "缓冲上限: " + config.maxBufferSize;
    }

    private static int indexOfScheme(String s) {
        for (int i = 0; i < SCHEMES.length; i++) if (SCHEMES[i].equals(s)) return i;
        return 0;
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

    // 面板、标题栏、分类高亮在背景层绘制，位于控件之下。
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int x = PAD, y = PAD, w = this.width - PAD * 2, h = this.height - PAD * 2;
        Theme.panel(graphics, x, y, w, h, 6, Theme.BORDER, Theme.PANEL);
        Theme.vGradient(graphics, x + 2, y + 2, w - 4, HEADER_H, Theme.HEADER_TOP, Theme.HEADER_BOTTOM);
        int accent = Theme.accent(config.colorScheme);

        int catY = y + HEADER_H + 8;
        for (int i = 0; i < CATS.length; i++) {
            int bx = x + 10, by = catY + i * 30;
            if (i == selected) {
                int hi = (0x30 << 24) | (accent & 0x00FFFFFF);
                graphics.fill(bx - 4, by - 2, bx + SIDEBAR_W - 14, by + 26, hi);
                graphics.fill(bx - 4, by - 2, bx - 2, by + 26, accent);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int x = PAD, y = PAD, w = this.width - PAD * 2, h = this.height - PAD * 2;
        int accent = Theme.accent(config.colorScheme);
        graphics.drawString(this.font, "战斗日志设置", x + 12, y + 9, accent, false);
        graphics.drawString(this.font, "[ESC] 返回", x + w - 80, y + 10, Theme.TEXT_DIM, false);

        int rx = x + SIDEBAR_W + 12;
        graphics.drawString(this.font, CATS[selected], rx, y + HEADER_H - 6, Theme.TEXT, false);

        if (selected == 0) {
            graphics.drawString(this.font, "是否在游戏内显示实时战斗 HUD。", rx, y + HEADER_H + 44, Theme.TEXT_DIM, false);
            graphics.drawString(this.font, "界面配色风格。", rx, y + HEADER_H + 128, Theme.TEXT_DIM, false);
        } else if (selected == 1) {
            graphics.drawString(this.font, "内存中保留的最大日志条数（修改即时生效）。", rx, y + HEADER_H + 14, Theme.TEXT_DIM, false);
            graphics.drawString(this.font, "清空当前会话的内存缓冲区（不影响已导出文件）。", rx, y + HEADER_H + 56, Theme.TEXT_DIM, false);
        } else {
            graphics.drawString(this.font, "Combat Damage Log  v1.0.0", rx, y + HEADER_H + 14, Theme.TEXT, false);
            graphics.drawString(this.font, "作者: MrKun", rx, y + HEADER_H + 38, Theme.TEXT_DIM, false);
            graphics.drawString(this.font, "按键 K 打开日志面板，H 切换 HUD。", rx, y + HEADER_H + 62, Theme.TEXT_DIM, false);
        }
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
