package extraordinary.content.hooks;

/**
 * HookService
 * -----------
 * One-button pipeline: select a template → fill placeholders → render text.
 */
public final class HookService {

    private final HookSelector selector;
    private final PlaceholderFiller filler;

    /** Result DTO for the generated hook line. */
    public record HookLine(String id, String text) {}

    public HookService(HookSelector selector, PlaceholderFiller filler) {
        this.selector = selector;
        this.filler = filler;
    }

    /**
     * Generates a hook line from inputs.
     * @param topic topic keyword (e.g., "creatine")
     * @param niche niche label (e.g., "fitness")
     * @param style style label (e.g., "educational")
     * @param ctx   optional selection context (seed, etc.)
     * @return HookLine with template id and rendered text
     */
    public HookLine generateHookLine(String topic, String niche, String style, HookSelector.SelectorContext ctx) {
        var tpl = selector.select(topic, niche, style, ctx);
        var values = filler.fill(tpl, topic, niche, style);
        var text = HookTemplates.render(tpl, values);
        return new HookLine(tpl.id(), text);
    }
}
