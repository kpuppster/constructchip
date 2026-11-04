package io.github.constructchip.attachment;

import com.mojang.serialization.Codec;
import io.github.constructchip.ConstructChip;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ConstructChip.MODID);

    /**
     * On the HOST: the installed chip, or EMPTY. Not copied on death; the death handler moves it into the drops.
     * After mutating the stack, call setData again so the attachment is marked dirty.
     */
    public static final Supplier<AttachmentType<ItemStack>> INSTALLED_CHIP = REGISTER.register("installed_chip",
            () -> AttachmentType.builder(() -> ItemStack.EMPTY)
                    .serialize(ItemStack.OPTIONAL_CODEC)
                    .build());

    /** On the CAPTURED player. Survives death (constructs are invulnerable, but /kill exists). */
    public static final Supplier<AttachmentType<ConstructState>> CONSTRUCT_STATE = REGISTER.register("construct_state",
            () -> AttachmentType.builder(() -> ConstructState.NONE)
                    .serialize(ConstructState.CODEC)
                    .copyOnDeath()
                    .build());

    /** On any player: refuses capture and chip installs by others (/construct optout). */
    public static final Supplier<AttachmentType<Boolean>> OPTED_OUT = REGISTER.register("opted_out",
            () -> AttachmentType.builder(() -> Boolean.FALSE)
                    .serialize(Codec.BOOL)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {}
}
