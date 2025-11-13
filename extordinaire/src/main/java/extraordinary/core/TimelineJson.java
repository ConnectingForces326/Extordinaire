package extraordinary.core;

import java.util.List;

/** Tiny helper to serialize a LayoutResult to JSON (no external libs). */
public final class TimelineJson {
    private TimelineJson() {}

    public static String toJson(LayoutResult r) {
        StringBuilder sb = new StringBuilder(128 + r.timeline().size() * 48);
        sb.append("{\"totalSec\":").append(r.totalSec()).append(",\"timeline\":[");
        List<SectionTiming> tl = r.timeline();
        for (int i = 0; i < tl.size(); i++) {
            SectionTiming t = tl.get(i);
            if (i > 0) sb.append(',');
            sb.append('{')
              .append("\"name\":\"").append(escape(t.name())).append("\",")
              .append("\"start\":").append(t.startSec()).append(',')
              .append("\"end\":").append(t.endSec())
              .append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String escape(String s) {
        // Minimal escaping good enough for our section names
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '\"') sb.append('\\').append(c);
            else sb.append(c);
        }
        return sb.toString();
    }
}
