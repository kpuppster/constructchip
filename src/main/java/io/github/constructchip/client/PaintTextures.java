package io.github.constructchip.client;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.constructchip.ConstructChip;
import io.github.constructchip.paint.Paint;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;

/** Builds (and caches) the painted version of an implant model texture: original pixels + the item's paint. */
public final class PaintTextures {
    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();

    private PaintTextures() {}

    /** The painted texture for {@code stack} on top of {@code base}, or null if unpainted. */
    public static ResourceLocation textureFor(ItemStack stack, ResourceLocation base) {
        String name = base.getPath().substring(base.getPath().lastIndexOf('/') + 1).replace(".png", "");
        String data = Paint.of(stack).get(name);
        if (data == null || data.isEmpty()) return null;
        String key = name + ":" + data.hashCode() + ":" + data.length();
        ResourceLocation cached = CACHE.get(key);
        if (cached != null) return cached;
        NativeImage image = load(base);
        if (image == null) return null;
        int[] diff = Paint.decode(data, image.getWidth() * image.getHeight());
        for (int i = 0; i < diff.length; i++) {
            int c = diff[i];
            if ((c >>> 24) != 0) image.setPixelRGBA(i % image.getWidth(), i / image.getWidth(), argbToAbgr(c));
        }
        ResourceLocation id = ConstructChip.id("painted/" + name + "_" + Integer.toHexString(key.hashCode()));
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
        CACHE.put(key, id);
        return id;
    }

    static NativeImage load(ResourceLocation location) {
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(location);
        if (res.isEmpty()) return null;
        try (InputStream in = res.get().open()) {
            return NativeImage.read(in);
        } catch (Exception e) {
            ConstructChip.LOGGER.warn("Couldn't read {}", location, e);
            return null;
        }
    }

    static int argbToAbgr(int c) {
        return (c & 0xFF00FF00) | ((c & 0xFF) << 16) | ((c >> 16) & 0xFF);
    }

    static int abgrToArgb(int c) {
        return argbToAbgr(c);
    }
}
