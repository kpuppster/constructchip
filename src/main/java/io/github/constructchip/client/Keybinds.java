package io.github.constructchip.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class Keybinds {
    public static final String CATEGORY = "key.categories.constructchip";

    public static final KeyMapping USE_HACK = key("use_hack", GLFW.GLFW_KEY_R);
    public static final KeyMapping NEXT_HACK = key("next_hack", GLFW.GLFW_KEY_Z);
    public static final KeyMapping SCAN = key("scan", GLFW.GLFW_KEY_V);
    public static final KeyMapping HOST_INVENTORY = key("host_inventory", GLFW.GLFW_KEY_H);
    public static final KeyMapping SKILL_TREE = key("skill_tree", GLFW.GLFW_KEY_K);
    public static final KeyMapping WHEEL = key("wheel", GLFW.GLFW_KEY_G);

    private Keybinds() {}

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.constructchip." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, CATEGORY);
    }
}
