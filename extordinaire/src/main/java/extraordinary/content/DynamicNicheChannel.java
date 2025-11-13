package extraordinary.content;

import extraordinary.helpers.RandomUtil;
import extraordinary.models.VideoPlan;

public class DynamicNicheChannel {
    private final RandomUtil R = new RandomUtil();

    public VideoPlan assembleVideo(String hook, String script) {
        String cta = R.pick(
            "Follow for more quick 60s tips!",
            "Save this and try it today!",
            "Subscribe for fast, practical breakdowns!",
            "Try this now — and follow for part 2!",
            "Comment ‘TEMPLATE’ and I’ll share the file!"
        );
        return new VideoPlan(hook, script, cta);
    }
}
