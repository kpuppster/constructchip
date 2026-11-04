package io.github.constructchip.capture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Everything about a player from just before capture, restored exactly once on release. Stored as an attachment on
 * the captured player, so it is saved in the same player file write as the capture itself. A crash can't leave a
 * captured player without a snapshot.
 *
 * @param items      non-empty inventory slots (0-35 main, 36-39 armor, 40 offhand)
 * @param capturedAt game time of the capture (for logs and admin info)
 */
public record CaptureSnapshot(List<SlotItem> items, Xp xp, GameType gameMode, Position position, Vitals vitals,
                              long capturedAt) {
    public static final Codec<CaptureSnapshot> CODEC = RecordCodecBuilder.create(i -> i.group(
            SlotItem.CODEC.listOf().fieldOf("items").forGetter(CaptureSnapshot::items),
            Xp.CODEC.fieldOf("xp").forGetter(CaptureSnapshot::xp),
            GameType.CODEC.fieldOf("game_mode").forGetter(CaptureSnapshot::gameMode),
            Position.CODEC.fieldOf("position").forGetter(CaptureSnapshot::position),
            Vitals.CODEC.fieldOf("vitals").forGetter(CaptureSnapshot::vitals),
            Codec.LONG.fieldOf("captured_at").forGetter(CaptureSnapshot::capturedAt)
    ).apply(i, CaptureSnapshot::new));

    /** The attachment value: empty when the player isn't captured. */
    public static final Codec<Optional<CaptureSnapshot>> OPTIONAL_CODEC =
            CODEC.optionalFieldOf("snapshot").codec();

    public record SlotItem(int slot, ItemStack stack) {
        public static final Codec<SlotItem> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("slot").forGetter(SlotItem::slot),
                ItemStack.CODEC.fieldOf("stack").forGetter(SlotItem::stack)
        ).apply(i, SlotItem::new));
    }

    public record Xp(int level, float progress, int total) {
        public static final Codec<Xp> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("level").forGetter(Xp::level),
                Codec.FLOAT.fieldOf("progress").forGetter(Xp::progress),
                Codec.INT.fieldOf("total").forGetter(Xp::total)
        ).apply(i, Xp::new));
    }

    public record Position(ResourceKey<Level> dimension, Vec3 pos, float yRot, float xRot) {
        public static final Codec<Position> CODEC = RecordCodecBuilder.create(i -> i.group(
                Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Position::dimension),
                Vec3.CODEC.fieldOf("pos").forGetter(Position::pos),
                Codec.FLOAT.fieldOf("y_rot").forGetter(Position::yRot),
                Codec.FLOAT.fieldOf("x_rot").forGetter(Position::xRot)
        ).apply(i, Position::new));
    }

    public record Vitals(float health, int food, float saturation, List<MobEffectInstance> effects) {
        public static final Codec<Vitals> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.fieldOf("health").forGetter(Vitals::health),
                Codec.INT.fieldOf("food").forGetter(Vitals::food),
                Codec.FLOAT.fieldOf("saturation").forGetter(Vitals::saturation),
                MobEffectInstance.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(Vitals::effects)
        ).apply(i, Vitals::new));
    }
}
