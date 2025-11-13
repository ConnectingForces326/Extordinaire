package extraordinary.logic;

import java.util.ArrayList;
import java.util.List;

import extraordinary.models.VideoPlan;
import extraordinary.models.VideoPlan.SegmentType;

/** Minimal dynamic allocator: FIXED seconds, PERCENT of total, AUTO (weighted). */
public class TimelineAllocator {

    public enum Mode { FIXED, PERCENT, AUTO }

    public static final class Spec {
        public final String id;
        public final SegmentType type;
        public final String text;
        public final Mode mode;
        /** seconds if FIXED, percent 0–100 if PERCENT, weight if AUTO */
        public final double value;

        public Spec(String id, SegmentType type, String text, Mode mode, double value) {
            this.id = id; this.type = type; this.text = text; this.mode = mode; this.value = value;
        }
    }

    /** Build a plan with dynamic timestamps. Optional CTA is appended at the end. */
    public static VideoPlan allocate(
            int totalSeconds,
            List<Spec> specs,
            Spec ctaAtEndOrNull,     // e.g. new Spec("cta", CTA, "...", FIXED, 5)
            int snapToSeconds        // 1 = whole seconds, 0 -> no snap (use integers anyway here)
    ) {
        int T = Math.max(1, totalSeconds);
        int snap = Math.max(1, snapToSeconds);

        double fixedSum = 0, percentSum = 0, autoWeight = 0;
        for (Spec s : specs) {
            switch (s.mode) {
                case FIXED -> fixedSum += s.value;
                case PERCENT -> percentSum += s.value;
                case AUTO -> autoWeight += Math.max(0, s.value);
            }
        }

        double ctaSeconds = (ctaAtEndOrNull != null) ? ctaAtEndOrNull.value : 0;
        double percentSeconds = (percentSum / 100.0) * T;
        double remaining = T - fixedSum - percentSeconds - ctaSeconds;
        if (remaining < 0) remaining = 0;

        // raw durations
        List<Double> durs = new ArrayList<>(specs.size());
        for (Spec s : specs) {
            double d;
            switch (s.mode) {
                case FIXED -> d = s.value;
                case PERCENT -> d = (s.value / 100.0) * T;
                case AUTO -> d = (autoWeight <= 0) ? 0 : (s.value / autoWeight) * remaining;
                default -> d = 0;
            }
            durs.add(d);
        }

        // snap + drift correction
        List<Integer> snapped = new ArrayList<>(durs.size());
        int totalUnits = T / snap * snap; // keep it simple on whole seconds
        int targetUnits = totalUnits - (int)Math.round(ctaSeconds / snap) * snap;
        int used = 0;
        for (double d : durs) {
            int sUnits = (int)Math.round(d / snap) * snap;
            snapped.add(sUnits);
            used += sUnits;
        }
        int diff = targetUnits - used;
        for (int i = 0; diff != 0 && i < snapped.size(); i++) {
            int adjust = diff > 0 ? snap : -snap;
            int cand = snapped.get(i) + adjust;
            if (cand >= 0) { snapped.set(i, cand); diff -= adjust; }
        }

        // assemble plan
        VideoPlan plan = new VideoPlan(T);
        int cursor = 0;
        for (int i = 0; i < specs.size(); i++) {
            int dur = snapped.get(i);
            if (dur <= 0) continue;
            Spec s = specs.get(i);
            int start = cursor;
            int end = Math.min(T, cursor + dur);
            plan.add(s.id, s.type, start, end, s.text);
            cursor = end;
        }
        if (ctaAtEndOrNull != null && ctaSeconds > 0) {
            int ctaDur = (int)Math.round(ctaSeconds / snap) * snap;
            int ctaStart = Math.max(0, T - ctaDur);
            plan.add(ctaAtEndOrNull.id, ctaAtEndOrNull.type, ctaStart, T, ctaAtEndOrNull.text);
        }
        return plan;
    }
}
