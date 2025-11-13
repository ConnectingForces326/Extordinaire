package extraordinary.logic;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import extraordinary.models.Niche;
import extraordinary.models.Style;

public class AutoStyleSelector {
    // ordered = most likely -> least likely (rough priors; tweak anytime)
    private static final Map<Niche, List<Style>> PRIORS = new EnumMap<>(Niche.class);
    static {
        PRIORS.put(Niche.AI,       Arrays.asList(Style.ANALYST, Style.HYPE,    Style.TEACHER));
        PRIORS.put(Niche.FITNESS,  Arrays.asList(Style.HYPE,    Style.TEACHER, Style.ANALYST));
        PRIORS.put(Niche.FINANCE,  Arrays.asList(Style.ANALYST, Style.TEACHER, Style.HYPE));
        PRIORS.put(Niche.EDUCATION,Arrays.asList(Style.TEACHER, Style.ANALYST, Style.HYPE));
        PRIORS.put(Niche.GAMING,   Arrays.asList(Style.HYPE,    Style.ANALYST, Style.TEACHER));
        PRIORS.put(Niche.BEAUTY,   Arrays.asList(Style.TEACHER, Style.HYPE,    Style.ANALYST));
        PRIORS.put(Niche.GENERAL,  Arrays.asList(Style.HYPE,    Style.TEACHER, Style.ANALYST));
    }

    // 60/30/10 weighting by order
    public Style pick(Niche niche) {
        List<Style> ordered = PRIORS.getOrDefault(niche, PRIORS.get(Niche.GENERAL));
        int roll = new Random().nextInt(100);
        if (roll < 60) return ordered.get(0);
        if (roll < 90) return ordered.get(1);
        return ordered.get(2);
    }
}
