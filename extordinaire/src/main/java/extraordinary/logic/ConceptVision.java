package extraordinary.logic;

import extraordinary.helpers.RandomUtil;
import extraordinary.models.ContentRequest;

public class ConceptVision {
    private final RandomUtil R = new RandomUtil();
    private final NicheRegistry registry = new NicheRegistry();

    public String ideateHook(ContentRequest req) {
        String t = req.topic();
        var bank = registry.hooksFor(req.niche());
        String format = R.pick(bank.toArray(new String[0]));
        return String.format(format, t);
    }
}
