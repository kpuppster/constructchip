package io.github.constructchip.cyberware;

import java.util.Locale;

/**
 * Body slots (Project: Edgerunner's layout, trimmed to slots that have implants). Skeleton has two positions.
 * {@link #index} is the position in the installed-implant list.
 */
public enum CyberSlot {
    OS(0), CORTEX(1), FACE(2), ARMS(3), CIRCULATORY(4), LEGS(5), SKELETON(6);

    public static final int POSITIONS = 8; // SKELETON uses 6 and 7
    public final int index;

    CyberSlot(int index) {
        this.index = index;
    }

    /** Body slot of list position {@code pos}. */
    public static CyberSlot ofPosition(int pos) {
        return pos >= SKELETON.index ? SKELETON : values()[pos];
    }

    public String key() {
        return "cyberslot.constructchip." + name().toLowerCase(Locale.ROOT);
    }
}
