package net.droingo.actioncamera.client;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public final class ActionCameraClientPreferences {
    private static final String FILE_NAME = "droingo_action_camera_client.properties";

    private static final String KEY_EDIT_OVERLAY_MODE = "editOverlayMode";
    private static final String KEY_RULE_OF_THIRDS_ENABLED = "ruleOfThirdsEnabled";

    private static boolean loaded = false;

    /*
     * This now controls the HUD panel only:
     * FULL   = full text panel
     * SIMPLE = small bottom strip
     * OFF    = no HUD text
     *
     * Rule of thirds is controlled separately.
     */
    private static EditOverlayMode editOverlayMode = EditOverlayMode.FULL;
    private static boolean ruleOfThirdsEnabled = true;

    private ActionCameraClientPreferences() {
    }

    public static EditOverlayMode getEditOverlayMode() {
        ensureLoaded();
        return editOverlayMode;
    }

    public static EditOverlayMode cycleEditOverlayMode() {
        ensureLoaded();

        editOverlayMode = editOverlayMode.next();
        save();

        return editOverlayMode;
    }

    public static boolean isRuleOfThirdsEnabled() {
        ensureLoaded();
        return ruleOfThirdsEnabled;
    }

    public static boolean toggleRuleOfThirdsEnabled() {
        ensureLoaded();

        ruleOfThirdsEnabled = !ruleOfThirdsEnabled;
        save();

        return ruleOfThirdsEnabled;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }

        loaded = true;

        Path path = configPath();

        if (!Files.exists(path)) {
            save();
            return;
        }

        Properties properties = new Properties();

        try (InputStream inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);

            String rawMode = properties.getProperty(KEY_EDIT_OVERLAY_MODE, EditOverlayMode.FULL.name());
            editOverlayMode = EditOverlayMode.fromString(rawMode);

            String rawRuleOfThirds = properties.getProperty(KEY_RULE_OF_THIRDS_ENABLED, "true");
            ruleOfThirdsEnabled = Boolean.parseBoolean(rawRuleOfThirds);
        } catch (IOException ignored) {
            editOverlayMode = EditOverlayMode.FULL;
            ruleOfThirdsEnabled = true;
        }
    }

    private static void save() {
        Path path = configPath();

        try {
            Files.createDirectories(path.getParent());

            Properties properties = new Properties();
            properties.setProperty(KEY_EDIT_OVERLAY_MODE, editOverlayMode.name());
            properties.setProperty(KEY_RULE_OF_THIRDS_ENABLED, Boolean.toString(ruleOfThirdsEnabled));

            try (OutputStream outputStream = Files.newOutputStream(path)) {
                properties.store(outputStream, "Droingo's Action Camera client preferences");
            }
        } catch (IOException ignored) {
            /*
             * Client preference only.
             * If saving fails, the mod should keep running normally.
             */
        }
    }

    private static Path configPath() {
        return Minecraft.getInstance()
                .gameDirectory
                .toPath()
                .resolve("config")
                .resolve(FILE_NAME);
    }

    public enum EditOverlayMode {
        FULL,
        SIMPLE,
        OFF;

        public EditOverlayMode next() {
            return switch (this) {
                case FULL -> SIMPLE;
                case SIMPLE -> OFF;
                case OFF -> FULL;
            };
        }

        public static EditOverlayMode fromString(String value) {
            if (value == null || value.isBlank()) {
                return FULL;
            }

            try {
                return EditOverlayMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return FULL;
            }
        }
    }
}