package extraordinary.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Core fitter for Extordinaire timelines.
 *
 * Responsibilities:
 *  - Enforce HOOK at index 0 and END at last.
 *  - Allocate around a midpoint, then grow to minTotal, shrink/prune to maxTotal.
 *  - Optionally auto-add a SECTION if total is still below minimum.
 *  - Preserve input order of non-locked sections.
 *
 * Input : ordered List<SectionSpec> and a LayoutRule.
 * Output: LayoutResult with ordered List<SectionTiming>.
 *
 * Notes:
 *  - This class is UI-free. Safe for both apps.
 *  - Per-section min/max from SectionSpec are optional. When null, we use soft defaults.
 */
public final class LayoutEngine {
    private LayoutEngine() {}

    /** Main entry point. */
    public static LayoutResult layout(List<SectionSpec> inputOrdered, LayoutRule rule) {
        if (inputOrdered == null) inputOrdered = Collections.emptyList();
        Objects.requireNonNull(rule, "rule");

        // Defensive copy and ensure HOOK/END exist in the right places.
        final List<Sec> secs = ensureEnds(inputOrdered);

        // 1) Initial allocation toward the midpoint of the target window.
        final int midTarget = midpoint(rule.minTotalSec(), rule.maxTotalSec());
        reCenterAllocate(secs, midTarget);

        // 2) If short, grow up to minTotal.
        int sum = sumAlloc(secs);
        if (sum < rule.minTotalSec()) {
            int deficit = rule.minTotalSec() - sum;
            growRoundRobin(secs, deficit);
            sum = sumAlloc(secs);
        }

        // 3) If long, shrink non-locked, then prune lowest value density sections.
        if (sum > rule.maxTotalSec()) {
            int over = sum - rule.maxTotalSec();
            over = shrinkRoundRobin(secs, over);
            if (over > 0) {
                pruneByValueDensity(secs, over);
            }
        }

        // 4) Still short? Optionally auto-add a SECTION and grow once more.
        sum = sumAlloc(secs);
        if (sum < rule.minTotalSec() && rule.allowAutoAdd()) {
            String name = nextAutoSectionName(secs);
            // Insert before END to keep END last.
            secs.add(secs.size() - 1, new Sec(name, 1.0, DEF_MIN, DEF_MAX, false));
            // Re-allocate around min to bias growth where needed.
            reCenterAllocate(secs, rule.minTotalSec());
            int deficit = rule.minTotalSec() - sumAlloc(secs);
            if (deficit > 0) growRoundRobin(secs, deficit);
        }

        // 5) Build final, ordered timeline.
        List<SectionTiming> timeline = buildTimeline(secs);
        int total = timeline.isEmpty() ? 0 : timeline.get(timeline.size()-1).endSec();

        return new LayoutResult(total, timeline);
    }

    // ===== Internal representation =====
    private static final int DEF_MIN = 3;   // default per-section min when unspecified
    private static final int DEF_MAX = 15;  // default per-section max when unspecified
    private static final int LOCKED_MIN_HOOK = 3;
    private static final int LOCKED_MAX_HOOK = 10;
    private static final int LOCKED_MIN_END  = 2;
    private static final int LOCKED_MAX_END  = 6;

    private static final class Sec {
        final String name;
        final double weight;
        final int min;
        final int max;
        final boolean locked;
        int alloc;

        Sec(String name, double weight, int min, int max, boolean locked) {
            this.name   = name;
            this.weight = (Double.isNaN(weight) || Double.isInfinite(weight) || weight <= 0) ? 1.0 : weight;
            this.min    = Math.max(0, min);
            this.max    = Math.max(this.min, max);
            this.locked = locked;
            this.alloc  = 0;
        }
    }

    // Ensure HOOK first and END last; copy bounds from SectionSpec where present.
    private static List<Sec> ensureEnds(List<SectionSpec> input) {
        List<Sec> out = new ArrayList<>();
        boolean hasLeadingHook = !input.isEmpty() && "HOOK".equals(input.get(0).name());
        boolean hasTrailingEnd = !input.isEmpty() && "END".equals(input.get(input.size()-1).name());

        if (!hasLeadingHook) {
            out.add(new Sec("HOOK", 2.0, LOCKED_MIN_HOOK, LOCKED_MAX_HOOK, true));
        }
        for (SectionSpec s : input) {
            if ("HOOK".equals(s.name())) {
                out.add(new Sec("HOOK", s.weight(),
                        orDefault(s.minSec(), LOCKED_MIN_HOOK),
                        orDefault(s.maxSec(), LOCKED_MAX_HOOK),
                        true));
            } else if ("END".equals(s.name())) {
                out.add(new Sec("END", s.weight(),
                        orDefault(s.minSec(), LOCKED_MIN_END),
                        orDefault(s.maxSec(), LOCKED_MAX_END),
                        true));
            } else {
                out.add(new Sec(
                        s.name(),
                        s.weight(),
                        orDefault(s.minSec(), DEF_MIN),
                        orDefault(s.maxSec(), DEF_MAX),
                        false
                ));
            }
        }
        if (!hasTrailingEnd) {
            out.add(new Sec("END", 0.7, LOCKED_MIN_END, LOCKED_MAX_END, true));
        }
        return out;
    }

    private static int orDefault(Integer v, int def) {
        return v == null ? def : Math.max(0, v);
    }

    private static int midpoint(int a, int b) { return a + (b - a) / 2; }

    // Initial allocation proportional to weight, clamped to per-section min/max.
    private static void reCenterAllocate(List<Sec> secs, int target) {
        double tw = secs.stream().mapToDouble(s -> s.weight).sum();
        if (tw <= 0) tw = 1.0;
        for (Sec s : secs) {
            int desired = (int)Math.round((s.weight / tw) * target);
            s.alloc = clamp(desired, s.min, s.max);
        }
    }

    // Grow total by distributing +1 in round-robin order across sections that can still grow.
    private static void growRoundRobin(List<Sec> secs, int deficit) {
        List<Sec> cands = new ArrayList<>(secs);
        int idx = 0, cycles = 0;
        while (deficit > 0 && !cands.isEmpty()) {
            Sec s = cands.get(idx % cands.size());
            if (s.alloc < s.max) {
                s.alloc++;
                deficit--;
            }
            idx++;
            if (idx % cands.size() == 0) {
                cycles++;
                cands.removeIf(x -> x.alloc >= x.max);
            }
            if (cycles > 10000) break; // safety
        }
    }

    // Shrink total by distributing -1 in round-robin across non-locked sections that can shrink.
    private static int shrinkRoundRobin(List<Sec> secs, int over) {
        List<Sec> cands = secs.stream().filter(s -> !s.locked).collect(Collectors.toList());
        int idx = 0, cycles = 0;
        while (over > 0 && !cands.isEmpty()) {
            Sec s = cands.get(idx % cands.size());
            if (s.alloc > s.min) {
                s.alloc--;
                over--;
            }
            idx++;
            if (idx % cands.size() == 0) {
                cycles++;
                cands.removeIf(x -> x.alloc <= x.min);
            }
            if (cycles > 10000) break; // safety
        }
        return over;
    }

    // If still long, prune entire lowest value-density sections (weight / alloc), skipping locked.
    private static void pruneByValueDensity(List<Sec> secs, int over) {
        while (over > 0) {
            Optional<Sec> victim = secs.stream()
                    .filter(s -> !s.locked && s.alloc > 0)
                    .min(Comparator.comparingDouble(s -> s.weight / Math.max(1.0, s.alloc)));
            if (victim.isEmpty()) break;
            Sec v = victim.get();
            over -= v.alloc;
            secs.remove(v);
        }
    }

    private static List<SectionTiming> buildTimeline(List<Sec> secs) {
        List<SectionTiming> out = new ArrayList<>();
        int t = 0;
        for (Sec s : secs) {
            int end = t + Math.max(0, s.alloc);
            out.add(new SectionTiming(s.name, t, end));
            t = end;
        }
        return out;
    }

    private static int sumAlloc(List<Sec> secs) {
        int sum = 0;
        for (Sec s : secs) sum += s.alloc;
        return sum;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static String nextAutoSectionName(List<Sec> secs) {
        Set<String> names = secs.stream().map(s -> s.name).collect(Collectors.toSet());
        int i = 1;
        while (names.contains("SECTION" + i)) i++;
        return "SECTION" + i;
        }
}
