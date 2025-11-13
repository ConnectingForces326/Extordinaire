package extraordinary.core;

/**
 * Output timing for a single section AFTER fitting.
 * Immutable, UI-free. Produced by LayoutEngine.
 *
 * name     : logical name (e.g., "HOOK", "SECTION1", "END")
 * startSec : inclusive start (>= 0)
 * endSec   : exclusive end (>= startSec)
 */
public final class SectionTiming {

    private final String name;
    private final int startSec;
    private final int endSec;

    public SectionTiming(String name, int startSec, int endSec) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("name is null/empty");
        if (startSec < 0)
            throw new IllegalArgumentException("startSec < 0");
        if (endSec < startSec)
            throw new IllegalArgumentException("endSec < startSec");

        this.name = name.trim();
        this.startSec = startSec;
        this.endSec = endSec;
    }

    public String name()   { return name; }
    public int startSec()  { return startSec; }
    public int endSec()    { return endSec; }
    public int duration()  { return endSec - startSec; }

    public SectionTiming withBounds(int newStart, int newEnd) {
        return new SectionTiming(this.name, newStart, newEnd);
    }

    @Override public String toString() {
        return "SectionTiming{name='" + name + "', start=" + startSec + ", end=" + endSec + "}";
    }

    @Override public int hashCode() {
        int h = name.hashCode();
        h = 31*h + startSec;
        h = 31*h + endSec;
        return h;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SectionTiming)) return false;
        SectionTiming that = (SectionTiming) o;
        return this.startSec == that.startSec &&
               this.endSec == that.endSec &&
               this.name.equals(that.name);
    }
}
