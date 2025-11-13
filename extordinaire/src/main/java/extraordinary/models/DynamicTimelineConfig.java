package extraordinary.models;

import java.util.List;

/**
 * Shared JSON model for the dynamic time-marker layout.
 * Produced by DynamicTimeMarkerPrototype, consumed by the main App.
 */
public class DynamicTimelineConfig {

    /** A label for this layout preset, e.g. "shorts-preset". */
    private String name;

    /** All section markers in order. */
    private List<Marker> markers;

    // Gson needs a no-arg constructor
    public DynamicTimelineConfig() {
    }

    public DynamicTimelineConfig(String name, List<Marker> markers) {
        this.name = name;
        this.markers = markers;
    }

    public String getName() {
        return name;
    }

    public List<Marker> getMarkers() {
        return markers;
    }

    // ---------- Nested marker class ----------

    public static class Marker {

        /** e.g. "HOOK", "SECTION1", "END" */
        private String sectionName;

        /** Start time in seconds. */
        private int startSec;

        /** End time in seconds. */
        private int endSec;

        /** Duration in seconds (endSec - startSec). */
        private int durationSec;

        // no-arg constructor for Gson
        public Marker() {
        }

        public Marker(String sectionName, int startSec, int endSec, int durationSec) {
            this.sectionName = sectionName;
            this.startSec = startSec;
            this.endSec = endSec;
            this.durationSec = durationSec;
        }

        public String getSectionName() {
            return sectionName;
        }

        public int getStartSec() {
            return startSec;
        }

        public int getEndSec() {
            return endSec;
        }

        public int getDurationSec() {
            return durationSec;
        }
    }
}
