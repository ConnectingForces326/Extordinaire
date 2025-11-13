package extraordinary.content.hooks;

import java.util.Map;

/**
 * PlaceholderFiller
 * -----------------
 * Fills {X}, {Y}, {Z} in a chosen HookTemplate.
 * Implementations can be rule-based or AI-driven.
 */
public interface PlaceholderFiller {
    /**
     * @param tpl   the chosen template (with {X}/{Y}/{Z})
     * @param topic user topic (may be null)
     * @param niche niche label (may be null)
     * @param style style/tone label (may be null)
     * @return map of placeholder → value, e.g., {"X":"creatine","Y":"your recovery"}
     */
    Map<String,String> fill(HookTemplates.HookTemplate tpl, String topic, String niche, String style);
}
