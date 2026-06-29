package net.droingo.actioncamera.client;

import dev.ryanhcode.sable.companion.SableCompanion;
import net.droingo.actioncamera.DroingoActionCamera;
import net.droingo.actioncamera.world.ActionCameraKnownCameras;
import net.droingo.actioncamera.world.blockentity.ActionCameraBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(
        modid = DroingoActionCamera.MOD_ID,
        value = Dist.CLIENT
)
public final class ClientGameEvents {
    private static final int CAMERA_CYCLE_CHUNK_RADIUS = 10;

    private ClientGameEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        handleViewKeybind();
        handleExtensionPoleKeybind();
        ActionCameraClientState.clientTick();
    }

    /*
     * FIX:
     * When extension pole edit mode is active, WASD/Space/Ctrl are camera-head
     * controls, not player controls.
     */
    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        if (!ActionCameraClientState.isExtensionEditMode()) {
            return;
        }

        event.getInput().forwardImpulse = 0.0F;
        event.getInput().leftImpulse = 0.0F;

        event.getInput().up = false;
        event.getInput().down = false;
        event.getInput().left = false;
        event.getInput().right = false;

        event.getInput().jumping = false;
        event.getInput().shiftKeyDown = false;
    }

    private static void handleViewKeybind() {
        Minecraft minecraft = Minecraft.getInstance();

        while (ActionCameraKeyMappings.TOGGLE_ACTION_CAMERA_VIEW.consumeClick()) {
            if (minecraft.player == null || minecraft.level == null) {
                return;
            }

            if (ActionCameraClientState.isEditingCamera()) {
                return;
            }

            BlockPos lookedAtCamera = getLookedAtCameraPos(minecraft);

            if (lookedAtCamera != null) {
                ActionCameraKnownCameras.register(lookedAtCamera);
            }

            List<BlockPos> cameras = collectKnownCameraPositions(minecraft);

            if (!ActionCameraClientState.isViewingCamera()) {
                if (lookedAtCamera != null) {
                    ActionCameraClientState.startViewing(
                            lookedAtCamera,
                            labelForCamera(cameras, lookedAtCamera)
                    );
                    return;
                }

                if (cameras.isEmpty()) {
                    minecraft.player.displayClientMessage(Component.literal("No loaded Action Cameras nearby"), true);
                    return;
                }

                BlockPos firstCamera = cameras.get(0);

                ActionCameraClientState.startViewing(
                        firstCamera,
                        labelForCamera(cameras, firstCamera)
                );

                return;
            }

            BlockPos current = ActionCameraClientState.getActiveCameraPos();

            if (current != null) {
                ActionCameraKnownCameras.register(current);
            }

            if (cameras.isEmpty()) {
                minecraft.player.displayClientMessage(Component.literal("No loaded Action Cameras nearby"), true);
                return;
            }

            int currentIndex = current == null ? -1 : cameras.indexOf(current);
            int nextIndex = currentIndex < 0 ? 0 : (currentIndex + 1) % cameras.size();

            BlockPos nextCamera = cameras.get(nextIndex);

            ActionCameraClientState.startViewing(
                    nextCamera,
                    labelForCamera(cameras, nextCamera)
            );
        }
    }

    private static void handleExtensionPoleKeybind() {
        Minecraft minecraft = Minecraft.getInstance();

        while (ActionCameraKeyMappings.TOGGLE_EXTENSION_POLE_EDIT.consumeClick()) {
            if (minecraft.player == null || minecraft.level == null) {
                return;
            }

            if (!ActionCameraClientState.isEditingCamera()) {
                minecraft.player.displayClientMessage(
                        Component.literal("Right-click an Action Camera first, then press V to edit the extension pole."),
                        true
                );
                return;
            }

            if (isAltDown(minecraft)) {
                ActionCameraClientState.toggleExtensionArm();
            } else {
                ActionCameraClientState.toggleExtensionEditMode();
            }
        }
    }

    private static boolean isAltDown(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();

        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }

    private static String labelForCamera(List<BlockPos> cameras, BlockPos pos) {
        int index = cameras.indexOf(pos);

        if (index < 0) {
            return "Cam ?";
        }

        return "Cam " + (index + 1);
    }

    private static BlockPos getLookedAtCameraPos(Minecraft minecraft) {
        if (minecraft.level == null) {
            return null;
        }

        HitResult hitResult = minecraft.hitResult;

        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            return null;
        }

        BlockPos pos = blockHitResult.getBlockPos();

        if (minecraft.level.getBlockEntity(pos) instanceof ActionCameraBlockEntity) {
            return pos.immutable();
        }

        return null;
    }

    private static List<BlockPos> collectKnownCameraPositions(Minecraft minecraft) {
        Set<BlockPos> cameraSet = new LinkedHashSet<>();

        if (minecraft.level == null || minecraft.player == null) {
            return new ArrayList<>();
        }

        cameraSet.addAll(ActionCameraKnownCameras.snapshot());

        ChunkPos playerChunk = new ChunkPos(minecraft.player.blockPosition());

        for (int chunkX = playerChunk.x - CAMERA_CYCLE_CHUNK_RADIUS; chunkX <= playerChunk.x + CAMERA_CYCLE_CHUNK_RADIUS; chunkX++) {
            for (int chunkZ = playerChunk.z - CAMERA_CYCLE_CHUNK_RADIUS; chunkZ <= playerChunk.z + CAMERA_CYCLE_CHUNK_RADIUS; chunkZ++) {
                if (!minecraft.level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }

                LevelChunk chunk = minecraft.level.getChunk(chunkX, chunkZ);

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof ActionCameraBlockEntity) {
                        BlockPos pos = blockEntity.getBlockPos().immutable();
                        cameraSet.add(pos);
                        ActionCameraKnownCameras.register(pos);
                    }
                }
            }
        }

        List<BlockPos> cameras = new ArrayList<>();

        for (BlockPos pos : cameraSet) {
            if (minecraft.level.getBlockEntity(pos) instanceof ActionCameraBlockEntity) {
                cameras.add(pos.immutable());
            } else {
                ActionCameraKnownCameras.unregister(pos);
            }
        }

        Vec3 playerPos = minecraft.player.position();

        cameras.sort(Comparator.comparingDouble(pos -> SableCompanion.INSTANCE.distanceSquaredWithSubLevels(
                minecraft.level,
                Vec3.atCenterOf(pos),
                playerPos
        )));

        return cameras;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }

        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null || event.getEntity() != minecraft.player) {
            return;
        }

        BlockPos pos = event.getPos();

        if (minecraft.level.getBlockEntity(pos) instanceof ActionCameraBlockEntity) {
            ActionCameraKnownCameras.register(pos);
            ActionCameraClientState.startEditing(pos);

            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private static void renderCameraViewingLabel(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();

        String label = ActionCameraClientState.getActiveCameraLabel();

        int x = 8;
        int y = 8;
        int textWidth = minecraft.font.width(label);

        guiGraphics.fill(
                x - 4,
                y - 3,
                x + textWidth + 5,
                y + 11,
                0x90000000
        );

        guiGraphics.drawString(
                minecraft.font,
                label,
                x,
                y,
                0xFFFFFF,
                false
        );
    }

    private static void renderCameraEditOverlay(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        renderRuleOfThirds(guiGraphics, width, height);
        renderEditHud(guiGraphics, minecraft, width, height);
    }

    private static void renderRuleOfThirds(GuiGraphics guiGraphics, int width, int height) {
        int lineColor = 0x55FFFFFF;

        int verticalOne = width / 3;
        int verticalTwo = (width * 2) / 3;

        int horizontalOne = height / 3;
        int horizontalTwo = (height * 2) / 3;

        guiGraphics.fill(verticalOne, 0, verticalOne + 1, height, lineColor);
        guiGraphics.fill(verticalTwo, 0, verticalTwo + 1, height, lineColor);

        guiGraphics.fill(0, horizontalOne, width, horizontalOne + 1, lineColor);
        guiGraphics.fill(0, horizontalTwo, width, horizontalTwo + 1, lineColor);
    }


    private static void renderEditHud(
            GuiGraphics guiGraphics,
            Minecraft minecraft,
            int width,
            int height
    ) {
        boolean poleAttached = ActionCameraClientState.isExtensionArmEnabledForHud();
        boolean polePlacement = ActionCameraClientState.isExtensionPlacementModeForHud();
        double distance = ActionCameraClientState.getExtensionDistanceForHud();

        int x = 8;
        int y = 8;

        String title = "Action Camera Edit";
        String poleStatus = poleAttached
                ? "Extension Pole: Attached"
                : "Extension Pole: Detached";

        String polePlacementStatus = polePlacement
                ? "Pole Placement: ACTIVE"
                : "Pole Placement: Off";

        String distanceText = String.format(java.util.Locale.ROOT, "Pole Distance: %.2f blocks", distance);

        String[] lines = new String[] {
                title,
                poleStatus,
                polePlacementStatus,
                distanceText,
                "",
                "Mouse: Aim camera",
                "V: Enter/exit pole placement",
                "Alt + V: Attach/detach pole",
                "WASD: Move camera head",
                "Space / Ctrl: Up / Down",
                polePlacement ? "Shift: Disabled while placing pole" : "Shift: Save and exit"
        };

        int maxWidth = 0;

        for (String line : lines) {
            maxWidth = Math.max(maxWidth, minecraft.font.width(line));
        }

        int lineHeight = 10;
        int padding = 6;

        int panelWidth = maxWidth + padding * 2;
        int panelHeight = lines.length * lineHeight + padding * 2;

        guiGraphics.fill(
                x - padding,
                y - padding,
                x - padding + panelWidth,
                y - padding + panelHeight,
                0xB0000000
        );

        /*
         * Small left accent strip:
         * green = pole attached
         * grey = pole detached
         */
        guiGraphics.fill(
                x - padding,
                y - padding,
                x - padding + 2,
                y - padding + panelHeight,
                poleAttached ? 0xFF66CC66 : 0xFF777777
        );

        int drawY = y;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            int color;

            if (i == 0) {
                color = 0xFFFFFF;
            } else if (line.startsWith("Extension Pole:")) {
                color = poleAttached ? 0x88FF88 : 0xCCCCCC;
            } else if (line.startsWith("Pole Placement:")) {
                color = polePlacement ? 0xFFD966 : 0xCCCCCC;
            } else if (line.startsWith("Shift: Disabled")) {
                color = 0xFFAA66;
            } else {
                color = 0xDDDDDD;
            }

            guiGraphics.drawString(
                    minecraft.font,
                    line,
                    x,
                    drawY,
                    color,
                    false
            );

            drawY += lineHeight;
        }

        renderEditModeTag(guiGraphics, minecraft, width, height, polePlacement);
    }

    private static void renderEditModeTag(
            GuiGraphics guiGraphics,
            Minecraft minecraft,
            int width,
            int height,
            boolean polePlacement
    ) {
        String text = polePlacement ? "POLE PLACEMENT" : "CAMERA AIM";

        int textWidth = minecraft.font.width(text);
        int x = (width - textWidth) / 2;
        int y = 8;

        guiGraphics.fill(
                x - 6,
                y - 4,
                x + textWidth + 6,
                y + 12,
                0xA0000000
        );

        guiGraphics.drawString(
                minecraft.font,
                text,
                x,
                y,
                polePlacement ? 0xFFD966 : 0xFFFFFF,
                false
        );
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        ActionCameraPose pose = ActionCameraClientState.getLastAppliedPose();

        if (pose == null || !ActionCameraClientState.isActive()) {
            return;
        }

        event.setYaw(pose.yaw());
        event.setPitch(pose.pitch());
        event.setRoll(pose.roll());
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!ActionCameraClientState.isActive()) {
            return;
        }

        float cameraFov = ActionCameraClientState.getActiveFovOverride();

        if (cameraFov > 0.0F) {
            event.setFOV(cameraFov);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        event.setFOV(minecraft.options.fov().get());
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (ActionCameraClientState.isActive()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (ActionCameraClientState.isEditingCamera()) {
            renderCameraEditOverlay(event.getGuiGraphics());
            return;
        }

        if (ActionCameraClientState.isViewingCamera()) {
            renderCameraViewingLabel(event.getGuiGraphics());
        }
    }
}