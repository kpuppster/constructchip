package io.github.constructchip.hack;

import java.util.Optional;
import net.minecraft.network.chat.Component;

/**
 * Outcome of {@link Quickhack#execute}. Only a success costs RAM and starts the cooldown.
 * Attack hacks succeed silently (their effect is the feedback); only info hacks show text.
 */
public record HackResult(boolean succeeded, Optional<Component> message, Display display) {
    public enum Display { NONE, ACTION_BAR, CHAT }

    /** Silent success. */
    public static HackResult success() {
        return new HackResult(true, Optional.empty(), Display.NONE);
    }

    /** Silent success; the message is kept for the debug command only. */
    public static HackResult success(Component message) {
        return new HackResult(true, Optional.of(message), Display.NONE);
    }

    /** Success with a short line on the construct's action bar (e.g. Ping's count). */
    public static HackResult status(Component message) {
        return new HackResult(true, Optional.of(message), Display.ACTION_BAR);
    }

    /** Success with a readout in the construct's chat (Scanner). */
    public static HackResult report(Component message) {
        return new HackResult(true, Optional.of(message), Display.CHAT);
    }

    /** Failure: the reason goes on the action bar. */
    public static HackResult fail(Component reason) {
        return new HackResult(false, Optional.of(reason), Display.ACTION_BAR);
    }
}
