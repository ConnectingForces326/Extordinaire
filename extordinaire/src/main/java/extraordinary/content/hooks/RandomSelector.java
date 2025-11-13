package extraordinary.content.hooks;

import java.util.Random;

/** Picks a random hook template from the whole pool. */
public final class RandomSelector implements HookSelector {
    @Override
    public HookTemplates.HookTemplate select(String topic, String niche, String style, SelectorContext ctx) {
        var list = HookTemplates.all();
        if (list.isEmpty()) throw new IllegalStateException("No hook templates available.");
        Random r = (ctx != null && ctx.seed() != null) ? new Random(ctx.seed()) : new Random();
        return list.get(r.nextInt(list.size()));
    }
}
