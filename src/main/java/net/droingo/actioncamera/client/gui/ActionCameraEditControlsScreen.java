package net.droingo.actioncamera.client.gui;

import net.droingo.actioncamera.client.ActionCameraClientPreferences;
import net.droingo.actioncamera.client.ActionCameraClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class ActionCameraEditControlsScreen extends Screen {
    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 168;

    public ActionCameraEditControlsScreen() {
        super(Component.literal("Action Camera Controls"));
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();

        if (!ActionCameraClientState.isEditingCamera()) {
            return;
        }

        if (minecraft.screen instanceof ActionCameraEditControlsScreen) {
            return;
        }

        minecraft.setScreen(new ActionCameraEditControlsScreen());
    }

    @Override
    protected void init() {
        ActionCameraClientState.setEditControlsOpen(true);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        int buttonWidth = 116;
        int buttonHeight = 20;
        int gap = 8;

        int leftX = panelX + 12;
        int rightX = panelX + 12 + buttonWidth + gap;

        int y = panelY + 38;

        this.addRenderableWidget(Button.builder(
                Component.literal("Rename"),
                button -> {
                    ActionCameraClientState.setEditControlsOpen(false);
                    Minecraft.getInstance().setScreen(new ActionCameraRenameScreen());
                }
        ).bounds(leftX, y, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("HUD: " + overlayModeLabel(ActionCameraClientPreferences.getEditOverlayMode())),
                button -> {
                    ActionCameraClientPreferences.cycleEditOverlayMode();
                    rebuildControls();
                }
        ).bounds(rightX, y, buttonWidth, buttonHeight).build());

        y += buttonHeight + gap;

        this.addRenderableWidget(Button.builder(
                Component.literal(ActionCameraClientState.isExtensionArmEnabledForHud()
                        ? "Detach Pole"
                        : "Attach Pole"),
                button -> {
                    ActionCameraClientState.toggleExtensionArm();
                    rebuildControls();
                }
        ).bounds(leftX, y, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(ActionCameraClientState.isExtensionPlacementModeForHud()
                        ? "Stop Placing"
                        : "Place Pole"),
                button -> {
                    ActionCameraClientState.toggleExtensionEditMode();
                    rebuildControls();
                }
        ).bounds(rightX, y, buttonWidth, buttonHeight).build());

        y += buttonHeight + gap;

        this.addRenderableWidget(Button.builder(
                Component.literal("Thirds: " + thirdsLabel()),
                button -> {
                    ActionCameraClientPreferences.toggleRuleOfThirdsEnabled();
                    rebuildControls();
                }
        ).bounds(leftX, y, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Save & Exit"),
                button -> {
                    ActionCameraClientState.setEditControlsOpen(false);
                    Minecraft.getInstance().setScreen(null);
                    ActionCameraClientState.stopEditing(true);
                }
        ).bounds(rightX, y, buttonWidth, buttonHeight).build());
    }

    private void rebuildControls() {
        this.clearWidgets();
        this.init();
    }

    private static String overlayModeLabel(ActionCameraClientPreferences.EditOverlayMode mode) {
        return switch (mode) {
            case FULL -> "Full";
            case SIMPLE -> "Simple";
            case OFF -> "Off";
        };
    }

    private static String thirdsLabel() {
        return ActionCameraClientPreferences.isRuleOfThirdsEnabled()
                ? "On"
                : "Off";
    }

    @Override
    public void tick() {
        if (!ActionCameraClientState.isEditingCamera()) {
            closeControls();
            return;
        }

        /*
         * This is a hold-to-use control panel.
         * Release Ctrl and it disappears, returning mouse control to camera aim.
         */
        if (!isControlDown()) {
            closeControls();
        }
    }

    private static boolean isControlDown() {
        Minecraft minecraft = Minecraft.getInstance();
        long window = minecraft.getWindow().getWindow();

        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    private void closeControls() {
        ActionCameraClientState.setEditControlsOpen(false);
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void removed() {
        ActionCameraClientState.setEditControlsOpen(false);
        super.removed();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        /*
         * Escape closes only the controls panel, not the whole camera edit.
         */
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeControls();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /*
     * Prevent the screen from drawing Minecraft's normal blurred/dimmed menu
     * background. We want the world/camera view to stay readable behind the
     * controls panel.
     */
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Intentionally empty.
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        /*
         * No full-screen dim/blur fill here.
         * Only the small panel itself gets a background.
         */
        guiGraphics.fill(
                panelX,
                panelY,
                panelX + PANEL_WIDTH,
                panelY + PANEL_HEIGHT,
                0xCC000000
        );

        guiGraphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                panelY + 10,
                0xFFFFFF
        );

        boolean poleAttached = ActionCameraClientState.isExtensionArmEnabledForHud();
        boolean placingPole = ActionCameraClientState.isExtensionPlacementModeForHud();

        String status = (poleAttached ? "Pole Attached" : "Pole Detached")
                + "  |  "
                + (placingPole ? "Pole Placement Active" : "Camera Aim Mode");

        guiGraphics.drawCenteredString(
                this.font,
                Component.literal(status),
                this.width / 2,
                panelY + 24,
                placingPole ? 0xFFD966 : 0xCCCCCC
        );

        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("Hold Ctrl to keep this panel open"),
                this.width / 2,
                panelY + PANEL_HEIGHT - 22,
                0xAAAAAA
        );

        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("Release Ctrl to return to camera aim"),
                this.width / 2,
                panelY + PANEL_HEIGHT - 11,
                0x888888
        );

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}