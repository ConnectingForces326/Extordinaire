package extraordinary.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The fitted result of a layout operation.
 * Contains the total duration and an ordered list of SectionTiming.
 */
public final class LayoutResult {

    private final int totalSec;
    private final List<SectionTiming> timeline;

    public LayoutResult(int totalSec, List<SectionTiming> timeline) {
        if (totalSec < 0) throw new IllegalArgumentException("totalSec < 0");
        this.totalSec = totalSec;
        this.timeline = Collections.unmodifiableList(new ArrayList<>(timeline));
    }

    /** Total length in seconds after fitting. */
    public int totalSec() { return totalSec; }

    /** Ordered list of section timings. */
    public List<SectionTiming> timeline() { return timeline; }

    /** Convenience method: find a section by name. */
    public Optional<SectionTiming> find(String name) {
        for (SectionTiming t : timeline) {
            if (t.name().equalsIgnoreCase(name)) return Optional.of(t);
        }
        return Optional.empty();
    }

    @Override public String toString() {
        return "LayoutResult{total=" + totalSec + ", timeline=" + timeline + "}";
    }
}
