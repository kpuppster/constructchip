package io.github.constructchip;

import net.minecraft.world.level.GameRules;

public final class ModGameRules {
    /** Global kill switch for capturing players and installing chips on other players. Admin overrides ignore it. */
    public static final GameRules.Key<GameRules.BooleanValue> ALLOW_CONSTRUCT_CAPTURE = GameRules.register(
            "allowConstructCapture", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));

    private ModGameRules() {}

    /** Called from the mod constructor so the static initializer runs during mod loading. */
    public static void init() {}
}
