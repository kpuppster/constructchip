package io.github.constructchip.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * One node of the skill tree, loaded from {@code data/<ns>/constructchip/skill/<id>.json}.
 * Entries can carry {@code neoforge:conditions} (e.g. Vista skills only exist when Vista is loaded).
 *
 * @param name        display name
 * @param description tooltip text
 * @param branch      column group in the tree screen
 * @param tier        1 to 3. Needs a chip of at least this hardware tier
 * @param cost        skill points to unlock
 * @param requires    every listed skill must be unlocked first (AND). Unknown ids are treated as missing
 * @param icon        item shown in the tree screen
 * @param quickhack   the quickhack this skill grants, if any
 * @param modifiers   passive stat changes while unlocked
 * @param column      horizontal slot in the tree screen; the row is the tier
 */
public record Skill(Component name, Component description, Branch branch, int tier, int cost,
                    List<ResourceLocation> requires, Item icon, Optional<ResourceLocation> quickhack,
                    List<SkillModifier> modifiers, int column) {
    public static final Codec<Skill> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(Skill::name),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(Skill::description),
            Branch.CODEC.fieldOf("branch").forGetter(Skill::branch),
            Codec.intRange(1, 3).fieldOf("tier").forGetter(Skill::tier),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("cost").forGetter(Skill::cost),
            ResourceLocation.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(Skill::requires),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("icon", Items.PAPER).forGetter(Skill::icon),
            ResourceLocation.CODEC.optionalFieldOf("quickhack").forGetter(Skill::quickhack),
            SkillModifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(Skill::modifiers),
            Codec.INT.optionalFieldOf("column", 0).forGetter(Skill::column)
    ).apply(i, Skill::new));

    public enum Branch implements StringRepresentable {
        /** Scanner, Overclock and the capstone. The spine every build passes through. */
        CORE,
        /** Offence against creatures and their cyberware. */
        INTRUSION,
        /** Keeping the host alive. */
        SUPPORT,
        /** RAM capacity and hacking devices (redstone, Vista). */
        SYSTEMS;

        public static final Codec<Branch> CODEC = StringRepresentable.fromEnum(Branch::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
