package io.github.constructchip.hack;

import java.util.Optional;
import net.minecraft.network.chat.Component;

/** Outcome of {@link Quickhack#execute}. Only a success costs RAM and starts the cooldown. */
public record HackResult(boolean success, Optional<Component> message) {
    public static HackResult success() {
        return new HackResult(true, Optional.empty());
    }

    public static HackResult success(Component message) {
        return new HackResult(true, Optional.of(message));
    }

    public static HackResult fail(Component reason) {
        return new HackResult(false, Optional.of(reason));
    }
}
