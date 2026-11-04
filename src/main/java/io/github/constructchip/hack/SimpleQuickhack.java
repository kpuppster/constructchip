package io.github.constructchip.hack;

/** Fixed numbers + an execute body. Most quickhacks extend this. */
public abstract class SimpleQuickhack implements Quickhack {
    private final float ramCost;
    private final int cooldown;
    private final TargetType targetType;
    private final double range;

    protected SimpleQuickhack(float ramCost, int cooldownTicks, TargetType targetType, double range) {
        this.ramCost = ramCost;
        this.cooldown = cooldownTicks;
        this.targetType = targetType;
        this.range = range;
    }

    @Override public float baseRamCost() { return ramCost; }
    @Override public int cooldownTicks() { return cooldown; }
    @Override public TargetType targetType() { return targetType; }
    @Override public double baseRange() { return range; }
}
