package io.github.constructchip.tracking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/** One registered chip. Immutable; {@link ChipRegistry} swaps whole entries. */
public record ChipEntry(UUID capturedUuid, String capturedName, int serial, ChipLocation location) {
    public static final Codec<ChipEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("captured_uuid").forGetter(ChipEntry::capturedUuid),
            Codec.STRING.fieldOf("captured_name").forGetter(ChipEntry::capturedName),
            Codec.INT.fieldOf("serial").forGetter(ChipEntry::serial),
            ChipLocation.CODEC.fieldOf("location").forGetter(ChipEntry::location)
    ).apply(i, ChipEntry::new));

    public ChipEntry withLocation(ChipLocation newLocation) {
        return new ChipEntry(capturedUuid, capturedName, serial, newLocation);
    }

    public ChipEntry withNextSerial() {
        return new ChipEntry(capturedUuid, capturedName, serial + 1, location);
    }
}
