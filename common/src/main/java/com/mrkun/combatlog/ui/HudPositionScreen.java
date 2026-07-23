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
 * HUD 位置拖动编辑器（WoW 风格可移动框体）。
 * 非暂停界面：世界在背后渲染，玩家直接按住 HUD 面板拖到任意位置，松开即保存。
 */
public final class HudPositionScreen extends Screen {
    private final Screen parent;
    private final LogConfig config = CombatLogService.getInstance().config();

    private int dragX, dragY;          // 当前面板左上角（屏幕坐标，绝对像素）
    private boolean dragging = false;
    private int grabOffX, grabOffY;    // 按下时鼠标相对面板左上角的偏移

    private Button defaultBtn, centerBtn, doneBtn;

    public HudPositionScreen(Screen parent) {
        super(Component.literal("HUD 位置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int[] pos = HudOverlay.resolvePos(config);
        this.dragX = pos[0];
        this.dragY = pos[1];

        int bw = 90, bh = 22, gap = 8;
        int total = bw * 3 + gap * 2;
        int bx = this.width / 2 - total / 2;
        int by = this.height - 36;
        defaultBtn = Button.builder(Component.literal("恢复默认"), b -> resetDefault())
                .bounds(bx, by, bw, bh).build();
        centerBtn = Button.builder(Component.literal("居中"), b -> resetCenter())
                .bounds(bx + (bw + gap), by, bw, bh).build();
        doneBtn = Button.builder(Component.literal("完成"), b -> saveAndClose())
                .bounds(bx + (bw + gap) * 2, by, bw, bh).build();
        this.addRenderableWidget(defaultBtn);
        this.addRenderableWidget(centerBtn);
        this.addRenderableWidget(doneBtn);
    }

    /** 编辑器本身不暂停世界，方便对照场景摆位。 */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 轻微压暗，突出可拖动的 HUD 面板
        graphics.fill(0, 0, this.width, this.height, 0x55101518);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int[] size = HudOverlay.panelSize();

        // 拖动中高亮面板描边
        int accent = Theme.accent(config.colorScheme);
        if (dragging) {
            graphics.fill(dragX - 2, dragY - 2, dragX + size[0] + 2, dragY - 1, accent);
            graphics.fill(dragX - 2, dragY + size[1] + 1, dragX + size[0] + 2, dragY + size[1] + 2, accent);
        }

        // 用与实时 HUD 完全相同的绘制，所见即所得
        HudOverlay.drawPanel(graphics, dragX, dragY, config, CombatLogService.getInstance().stats());

        graphics.drawString(this.font, "拖动面板到任意位置  ·  松开鼠标保存  ·  [ESC] 完成",
                this.width / 2 - 150, 14, Theme.TEXT, false);
        graphics.drawString(this.font, "当前位置: (" + dragX + ", " + dragY + ")",
                this.width / 2 - 70, 32, Theme.TEXT_DIM, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int[] size = HudOverlay.panelSize();
        if (mouseX >= dragX && mouseX <= dragX + size[0] && mouseY >= dragY && mouseY <= dragY + size[1]) {
            dragging = true;
            grabOffX = (int) mouseX - dragX;
            grabOffY = (int) mouseY - dragY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            int[] size = HudOverlay.panelSize();
            int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            dragX = (int) mouseX - grabOffX;
            dragY = (int) mouseY - grabOffY;
            dragX = Math.max(0, Math.min(dragX, sw - size[0]));
            dragY = Math.max(0, Math.min(dragY, sh - size[1]));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            persist();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void resetDefault() {
        int[] size = HudOverlay.panelSize();
        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        dragX = sw - size[0] - 8;
        dragY = 8;
        config.hudX = -1;
        config.hudY = -1;
        config.save();
    }

    private void resetCenter() {
        int[] size = HudOverlay.panelSize();
        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        dragX = Math.max(0, (sw - size[0]) / 2);
        dragY = Math.max(0, (sh - size[1]) / 2);
        persist();
    }

    private void persist() {
        config.hudX = dragX;
        config.hudY = dragY;
        config.save();
    }

    private void saveAndClose() {
        persist();
        this.onClose();
    }

    @Override
    public void onClose() {
        config.save();
        if (parent != null) Minecraft.getInstance().setScreen(parent);
        else super.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            saveAndClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
