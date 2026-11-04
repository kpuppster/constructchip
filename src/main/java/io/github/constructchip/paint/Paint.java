package io.github.constructchip.paint;

import io.github.constructchip.cyberware.CyberwareItem;
import io.github.constructchip.cyberware.Implant;
import io.github.constructchip.registry.ModDataComponents;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import net.minecraft.world.item.ItemStack;

/**
 * Paint on implants (Project: Edgerunner's painting station, our implementation). Per model texture, the item stores
 * a diff over the original texture: RGBA per pixel, alpha 0 = "not painted", deflated and base64'd (so the
 * component compares by value). Painted textures show on the 3D implant model.
 */
public final class Paint {
    /** Max stored length per texture (fits a client → server packet). */
    public static final int MAX_LENGTH = 30000;

    private Paint() {}

    /** Model textures of an implant that can be painted (names under textures/models/implant/). */
    public static List<String> textures(ItemStack stack) {
        return CyberwareItem.implantOf(stack).map(Paint::textures).orElse(List.of());
    }

    public static List<String> textures(Implant implant) {
        return switch (implant) {
            case WARP_DANCER -> List.of("sandevistan_warp_dancer_texture");
            case ZETATECH, DYNALAR, FALCON, APOGEE -> List.of("sandevistan_" + implant.id() + "_texture");
            case GORILLA_ARMS -> List.of("gorilla_arm_texture", "gorilla_arm_right_texture");
            case BLOOD_PUMP -> List.of("blood_pump");
            case FORTIFIED_ANKLES -> List.of("fortified_ankles_texture");
            case REINFORCED_TENDONS -> List.of("reinforced_tendons_texture");
            default -> List.of();
        };
    }

    public static Map<String, String> of(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CYBER_PAINT.get(), Map.of());
    }

    public static void set(ItemStack stack, String texture, String data) {
        Map<String, String> next = new HashMap<>(of(stack));
        if (data == null || data.isEmpty()) next.remove(texture);
        else next.put(texture, data);
        if (next.isEmpty()) stack.remove(ModDataComponents.CYBER_PAINT.get());
        else stack.set(ModDataComponents.CYBER_PAINT.get(), Map.copyOf(next));
    }

    /** ARGB pixels where alpha 0 = unpainted → stored string ("" when nothing is painted). */
    public static String encode(int[] diff) {
        boolean any = false;
        byte[] raw = new byte[diff.length * 4];
        for (int i = 0; i < diff.length; i++) {
            int c = diff[i];
            if ((c >>> 24) != 0) any = true;
            raw[i * 4] = (byte) (c >> 16);
            raw[i * 4 + 1] = (byte) (c >> 8);
            raw[i * 4 + 2] = (byte) c;
            raw[i * 4 + 3] = (byte) (c >>> 24);
        }
        if (!any) return "";
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
        d.setInput(raw);
        d.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        while (!d.finished()) out.write(buf, 0, d.deflate(buf));
        d.end();
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    /** Stored string → ARGB diff of {@code size} pixels (all zero if empty or broken). */
    public static int[] decode(String data, int size) {
        int[] out = new int[size];
        if (data == null || data.isEmpty()) return out;
        try {
            Inflater inf = new Inflater();
            inf.setInput(Base64.getDecoder().decode(data));
            byte[] raw = new byte[size * 4];
            int n = 0;
            while (n < raw.length && !inf.finished()) {
                int r = inf.inflate(raw, n, raw.length - n);
                if (r == 0 && (inf.needsInput() || inf.needsDictionary())) break;
                n += r;
            }
            inf.end();
            for (int i = 0; i < size; i++) {
                out[i] = ((raw[i * 4 + 3] & 0xFF) << 24) | ((raw[i * 4] & 0xFF) << 16) | ((raw[i * 4 + 1] & 0xFF) << 8) | (raw[i * 4 + 2] & 0xFF);
            }
        } catch (IllegalArgumentException | DataFormatException e) {
            return new int[size];
        }
        return out;
    }
}
