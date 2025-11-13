package extraordinary.content.hooks;

import java.util.HashMap;
import java.util.Map;

/**
 * SimpleFiller
 * ------------
 * Minimal rule-based filler for {X},{Y},{Z}.
 * - X defaults to topic (or "this")
 * - Y defaults to niche  (or "your results")
 * - Z unused for now (left out unless provided)
 *
 * AI can replace this later with smarter values.
 */
public final class SimpleFiller implements PlaceholderFiller {

    @Override
    public Map<String, String> fill(HookTemplates.HookTemplate tpl, String topic, String niche, String style) {
        Map<String,String> m = new HashMap<>();
        m.put("X", topic != null && !topic.isBlank() ? topic : "this");
        m.put("Y", niche  != null && !niche.isBlank()  ? niche  : "your results");
        // m.put("Z", ...); // add when you need it
        return m;
    }
}
