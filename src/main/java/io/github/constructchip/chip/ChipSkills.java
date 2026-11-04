package io.github.constructchip.chip;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

/**
 * Skill progression stored on the chip. Skill ids point into the (future) datapack skill registry; ids that no
 * longer resolve (e.g. Vista removed) are kept so they come back if the mod returns, and are simply ignored.
 *
 * @param tier     chip hardware tier (Mk.1 to Mk.3). It caps which skill tiers can be unlocked
 * @param points   unspent skill points
 * @param unlocked unlocked skill ids (immutable)
 */
public record ChipSkills(int tier, int points, Set<ResourceLocation> unlocked) {
    /** Scanner is the tree's root: free and always unlocked on a fresh chip. */
    public static final ResourceLocation ROOT_SKILL = ResourceLocation.fromNamespaceAndPath("constructchip", "scanner");
    public static final int MAX_TIER = 3;
    public static final ChipSkills DEFAULT = new ChipSkills(1, 0, Set.of(ROOT_SKILL));

    public static final Codec<ChipSkills> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("tier", 1).forGetter(ChipSkills::tier),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("points", 0).forGetter(ChipSkills::points),
            ResourceLocation.CODEC.listOf().<Set<ResourceLocation>>xmap(Set::copyOf, List::copyOf)
                    .optionalFieldOf("unlocked", Set.of()).forGetter(ChipSkills::unlocked)
    ).apply(i, ChipSkills::new));

    public static final StreamCodec<ByteBuf, ChipSkills> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ChipSkills::tier,
            ByteBufCodecs.VAR_INT, ChipSkills::points,
            ByteBufCodecs.<ByteBuf, ResourceLocation, HashSet<ResourceLocation>>collection(
                            HashSet::new, ResourceLocation.STREAM_CODEC)
                    .<Set<ResourceLocation>>map(Set::copyOf, HashSet::new), ChipSkills::unlocked,
            ChipSkills::new);

    public ChipSkills {
        unlocked = Set.copyOf(unlocked); // components must be immutable
    }

    public boolean has(ResourceLocation skill) {
        return unlocked.contains(skill);
    }

    public ChipSkills withTier(int newTier) {
        return new ChipSkills(newTier, points, unlocked);
    }

    public ChipSkills withPoints(int newPoints) {
        return new ChipSkills(tier, newPoints, unlocked);
    }

    public ChipSkills unlock(ResourceLocation skill) {
        if (has(skill)) return this;
        Set<ResourceLocation> next = new HashSet<>(unlocked);
        next.add(skill);
        return new ChipSkills(tier, points, next);
    }
}
