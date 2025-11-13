package extraordinary.core;

import java.util.Objects;

/**
 * Describes a single section BEFORE fitting.
 * Immutable, UI-free. Used as input for LayoutEngine.
 *
 * name   : logical name (e.g., "HOOK", "SECTION1", "END")
 * weight : relative importance (>0). Used to apportion time.
 * minSec : optional per-section minimum duration (nullable)
 * maxSec : optional per-section maximum duration (nullable)
 */
public final class SectionSpec {

    private final String name;
    private final double weight;
    private final Integer minSec; // nullable
    private final Integer maxSec; // nullable

    /**
     * Create a section specification.
     * @param name   non-null, non-empty
     * @param weight relative weight (>0). If invalid, defaults to 1.0
     * @param minSec nullable; if negative, treated as null
     * @param maxSec nullable; if non-null and < minSec, it will be ignored (treated as null)
     */
    public SectionSpec(String name, double weight, Integer minSec, Integer maxSec) {
        this.name = Objects.requireNonNull(name, "name").trim();
        if (this.name.isEmpty()) throw new IllegalArgumentException("name is empty");

        this.weight = (Double.isNaN(weight) || Double.isInfinite(weight) || weight <= 0.0) ? 1.0 : weight;

        Integer mn = (minSec != null && minSec >= 0) ? minSec : null;
        Integer mx = (maxSec != null && maxSec >= 0) ? maxSec : null;
        if (mn != null && mx != null && mx < mn) {
            // invalid window; drop the max to avoid contradictory bounds
            mx = null;
        }
        this.minSec = mn;
        this.maxSec = mx;
    }

    // ----- Accessors -----
    public String  name()   { return name; }
    public double  weight() { return weight; }
    public Integer minSec() { return minSec; }
    public Integer maxSec() { return maxSec; }

    // ----- Builders / helpers (optional but handy) -----

    /** Returns a copy with a different name. */
    public SectionSpec withName(String newName) {
        return new SectionSpec(newName, this.weight, this.minSec, this.maxSec);
    }

    /** Returns a copy with a different weight. */
    public SectionSpec withWeight(double newWeight) {
        return new SectionSpec(this.name, newWeight, this.minSec, this.maxSec);
    }

    /** Returns a copy with new min/max per-section bounds. */
    public SectionSpec withBounds(Integer newMin, Integer newMax) {
        return new SectionSpec(this.name, this.weight, newMin, newMax);
    }

    @Override public String toString() {
        return "SectionSpec{name='" + name + "', weight=" + weight +
               ", minSec=" + minSec + ", maxSec=" + maxSec + "}";
    }

    @Override public int hashCode() {
        int h = name.hashCode();
        long w = Double.doubleToLongBits(weight);
        h = 31*h + (int)(w ^ (w>>>32));
        h = 31*h + (minSec == null ? 0 : minSec.hashCode());
        h = 31*h + (maxSec == null ? 0 : maxSec.hashCode());
        return h;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SectionSpec)) return false;
        SectionSpec that = (SectionSpec) o;
        return Double.compare(that.weight, weight) == 0 &&
               name.equals(that.name) &&
               Objects.equals(minSec, that.minSec) &&
               Objects.equals(maxSec, that.maxSec);
        }
}
