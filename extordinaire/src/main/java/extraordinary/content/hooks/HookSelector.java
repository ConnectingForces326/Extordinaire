package extraordinary.content.hooks;

/**
 * HookSelector
 * ------------
 * Picks which hook template to use.
 * Implementations can be random, data-driven, trend-aware, etc.
 */
public interface HookSelector {

    /**
     * Choose a HookTemplate for the given inputs.
     * @param topic user/topic focus (may be null)
     * @param niche content niche (may be null)
     * @param style style/tone label (may be null)
     * @param ctx   optional context (seed, user/session info)
     * @return a HookTemplates.HookTemplate to render
     */
    HookTemplates.HookTemplate select(String topic, String niche, String style, SelectorContext ctx);

    /**
     * Minimal selection context. Expand later with metrics, user id, etc.
     */
    record SelectorContext(Long seed) {
        public static SelectorContext none() { return new SelectorContext(null); }
    }
}
