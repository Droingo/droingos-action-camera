package net.droingo.actioncamera.client.gui;

import net.droingo.actioncamera.client.ActionCameraClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class ActionCameraRenameScreen extends Screen {
    private static final int MAX_NAME_LENGTH = 32;

    private EditBox nameBox;

    public ActionCameraRenameScreen() {
        super(Component.literal("Rename Action Camera"));
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();

        if (!ActionCameraClientState.isEditingCamera()) {
            return;
        }

        minecraft.setScreen(new ActionCameraRenameScreen());
    }

    @Override
    protected void init() {
        int panelWidth = 240;
        int x = (this.width - panelWidth) / 2;
        int y = (this.height / 2) - 34;

        this.nameBox = new EditBox(
                this.font,
                x,
                y,
                panelWidth,
                20,
                Component.literal("Camera name")
        );

        this.nameBox.setMaxLength(MAX_NAME_LENGTH);
        this.nameBox.setValue(ActionCameraClientState.getEditingCameraNameForRenameScreen());
        this.nameBox.setFocused(true);

        this.addRenderableWidget(this.nameBox);
        this.setInitialFocus(this.nameBox);

        this.addRenderableWidget(Button.builder(
                Component.literal("Done"),
                button -> finishRename()
        ).bounds(
                x,
                y + 30,
                114,
                20
        ).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                button -> closeWithoutSaving()
        ).bounds(
                x + 126,
                y + 30,
                114,
                20
        ).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x88000000);

        int panelWidth = 260;
        int panelHeight = 88;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        guiGraphics.fill(
                panelX,
                panelY,
                panelX + panelWidth,
                panelY + panelHeight,
                0xCC000000
        );

        guiGraphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                panelY + 10,
                0xFFFFFF
        );

        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("Blank name resets to generated Cam name."),
                this.width / 2,
                panelY + 69,
                0xAAAAAA
        );

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            finishRename();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeWithoutSaving();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void finishRename() {
        if (this.nameBox != null) {
            ActionCameraClientState.renameEditingCamera(this.nameBox.getValue());
        }

        closeScreen();
    }

    private void closeWithoutSaving() {
        closeScreen();
    }

    private void closeScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}