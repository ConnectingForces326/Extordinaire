package extraordinary.models;

import java.util.ArrayList;
import java.util.List;

public class VideoPlan {

    // ---- Types -------------------------------------------------------------

    public enum SegmentType { HOOK, STEP, CTA, OTHER }

    public static final class Segment {
        public final String id;          // stable cross-ref, e.g., "hook", "step-2"
        public final SegmentType type;
        public final int startSec;
        public final int endSec;
        public final String text;

        public Segment(String id, SegmentType type, int startSec, int endSec, String text) {
            this.id = id;
            this.type = type;
            this.startSec = startSec;
            this.endSec = endSec;
            this.text = text;
        }
    }

    /** Optional metadata that callers may use for display/logging. */
    public static final class Meta {
        public String topic;
        public String niche;
        public String style;
        public String hook;   // optional: final chosen hook line
        public String cta;    // optional: final chosen CTA line
    }

    // ---- Fields ------------------------------------------------------------

    /** Default length used by the legacy (topic,niche,style) constructor. */
    public static final int DEFAULT_TOTAL_SECONDS = 60;

    private final int totalSeconds;
    private final List<Segment> segments = new ArrayList<>();
    private final Meta meta = new Meta();

    // ---- Constructors ------------------------------------------------------

    /** Preferred: explicit totalSeconds (used by TimelineAllocator). */
    public VideoPlan(int totalSeconds) {
        this.totalSeconds = Math.max(1, totalSeconds);
    }

    /**
     * Legacy overload to keep older code compiling:
     * new VideoPlan(topic, niche, style)
     * Uses DEFAULT_TOTAL_SECONDS; you can still override with a new plan later.
     */
    public VideoPlan(String topic, String niche, String style) {
        this(DEFAULT_TOTAL_SECONDS);
        this.meta.topic = topic;
        this.meta.niche = niche;
        this.meta.style = style;
    }

    // ---- API ---------------------------------------------------------------

    public int totalSeconds() { return totalSeconds; }
    public List<Segment> segments() { return segments; }
    public Meta meta() { return meta; }

    public void add(String id, SegmentType type, int startSec, int endSec, String text) {
        segments.add(new Segment(id, type, startSec, endSec, text));
    }

    /**
     * Minimal validator to catch timeline issues early.
     * Throws IllegalArgumentException on error.
     */
    public void validate() {
        int lastEnd = 0;
        for (Segment s : segments) {
            if (s.startSec < 0 || s.endSec < 0 || s.startSec >= s.endSec) {
                throw new IllegalArgumentException("Invalid segment range for id=" + s.id);
            }
            if (s.startSec < lastEnd) {
                throw new IllegalArgumentException("Overlap detected before id=" + s.id);
            }
            if (s.endSec > totalSeconds) {
                throw new IllegalArgumentException("Segment exceeds totalSeconds for id=" + s.id);
            }
            lastEnd = s.endSec;
        }
    }

    // ---- FORMATTED OUTPUT --------------------------------------------------

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("# The Extordinaire Draft\n");
        if (meta.topic != null) sb.append("Topic: ").append(meta.topic).append('\n');
        if (meta.niche != null) sb.append("Niche: ").append(meta.niche).append('\n');
        if (meta.style != null) sb.append("Style: ").append(meta.style).append('\n');
        if (meta.hook != null) sb.append("Hook: ").append(meta.hook).append('\n');
        if (meta.cta != null) sb.append("CTA: ").append(meta.cta).append('\n');
        sb.append("Total: ").append(totalSeconds).append("s\n\n");

        for (Segment s : segments) {
            sb.append(String.format("%02d–%02d [%s] %s — %s\n",
                    s.startSec, s.endSec, s.id, s.type, s.text));
        }
        return sb.toString();
    }

    /** Export caption file (SRT-style). */
    public String toSrt() {
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (Segment s : segments) {
            sb.append(index++).append('\n');
            sb.append(secToTimestamp(s.startSec)).append(" --> ")
              .append(secToTimestamp(s.endSec)).append('\n');
            sb.append(s.text).append("\n\n");
        }
        return sb.toString();
    }

    private static String secToTimestamp(int sec) {
        int h = sec / 3600;
        int m = (sec % 3600) / 60;
        int s = sec % 60;
        return String.format("%02d:%02d:%02d,000", h, m, s);
    }
}
