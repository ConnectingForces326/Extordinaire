package extraordinary.core;

/**
 * Global constraints for the layout engine.
 * - Defines the allowed total duration window (min..max).
 * - Controls whether the engine may auto-add sections if the total is short.
 *
 * Keep this class UI-free so both apps can depend on it.
 */
public final class LayoutRule {

    /** Lower bound for total video length, in seconds (inclusive). */
    private final int minTotalSec;

    /** Upper bound for total video length, in seconds (inclusive). */
    private final int maxTotalSec;

    /** If true, the engine may auto-add a SECTION to reach the minimum total. */
    private final boolean allowAutoAdd;

    /**
     * Create a rule with explicit bounds.
     * @param minTotalSec inclusive lower bound (>= 0)
     * @param maxTotalSec inclusive upper bound (>= minTotalSec)
     * @param allowAutoAdd whether the fitter can insert a SECTION when short
     */
    public LayoutRule(int minTotalSec, int maxTotalSec, boolean allowAutoAdd) {
        if (minTotalSec < 0) {
            throw new IllegalArgumentException("minTotalSec < 0");
        }
        if (maxTotalSec < minTotalSec) {
            throw new IllegalArgumentException("maxTotalSec < minTotalSec");
        }
        this.minTotalSec = minTotalSec;
        this.maxTotalSec = maxTotalSec;
        this.allowAutoAdd = allowAutoAdd;
    }

    // ---------- Convenience presets ----------
    /**
     * Shorts preset: target window 30–59s. Auto-add enabled.
     * Use when you’re shaping vertical short-form content.
     */
    public static LayoutRule shortsPreset() {
        return new LayoutRule(30, 59, true);
    }

    /**
     * “Video / 2-minute” preset: target window 90–120s. Auto-add enabled.
     * Tweak these bounds later if your long-form target changes.
     */
    public static LayoutRule videoPreset2min() {
        return new LayoutRule(90, 120, true);
    }

    // ---------- Accessors ----------
    public int minTotalSec()      { return minTotalSec; }
    public int maxTotalSec()      { return maxTotalSec; }
    public boolean allowAutoAdd() { return allowAutoAdd; }

    // ---------- Fluent helpers (optional, nice for chaining) ----------
    /** Returns a copy with a different min/max window. */
    public LayoutRule withWindow(int newMin, int newMax) {
        return new LayoutRule(newMin, newMax, this.allowAutoAdd);
    }

    /** Returns a copy with a different auto-add policy. */
    public LayoutRule withAutoAdd(boolean newAllowAutoAdd) {
        return new LayoutRule(this.minTotalSec, this.maxTotalSec, newAllowAutoAdd);
    }

    @Override public String toString() {
        return "LayoutRule{min=" + minTotalSec + ", max=" + maxTotalSec +
               ", allowAutoAdd=" + allowAutoAdd + "}";
    }
}
