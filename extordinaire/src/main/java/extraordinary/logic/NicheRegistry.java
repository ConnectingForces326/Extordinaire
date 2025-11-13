package extraordinary.logic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import extraordinary.models.Niche;

public class NicheRegistry {
    private final Map<Niche, List<String>> HOOKS = new HashMap<>();

    public NicheRegistry() {
        // Fallback / general hooks
        HOOKS.put(Niche.GENERAL, Arrays.asList(
            "Nobody talks about %s like this…",
            "I tested %s so you don’t have to.",
            "The fastest way to win at %s (no fluff).",
            "You’re doing %s wrong — fix it in 10s.",
            "3% of people know this %s trick.",
            "Stop scrolling: one %s idea to copy."
        ));

        // Example niche-specific sets (expand later)
        HOOKS.put(Niche.AI, Arrays.asList(
            "This %s workflow cuts render time in half.",
            "The 10-second %s setup I wish I knew sooner.",
            "Stop over-tuning — 1 %s change = cleaner output.",
            "Steal this %s prompt structure (insane results)."
        ));

        HOOKS.put(Niche.FITNESS, Arrays.asList(
            "The 30s %s fix that stops plateaus.",
            "This %s cue instantly cleans your form.",
            "One %s tweak for faster gains (no extra time)."
        ));

        HOOKS.put(Niche.FINANCE, Arrays.asList(
            "The 60s %s play most people miss.",
            "One %s habit that compounds fast.",
            "This %s checklist saves real money."
        ));

        HOOKS.put(Niche.EDUCATION, Arrays.asList(
            "A 3-step %s method students actually use.",
            "The fastest way to retain %s in 10 minutes.",
            "Stop cramming — 1 %s trick that sticks."
        ));

        HOOKS.put(Niche.GAMING, Arrays.asList(
            "One %s setting pros won’t tell you.",
            "The fastest path to rank up in %s.",
            "Stop doing this in %s — do this instead."
        ));

        HOOKS.put(Niche.BEAUTY, Arrays.asList(
            "This 30s %s routine changes everything.",
            "One %s mistake ruining your look.",
            "The pro %s trick you can copy today."
        ));
    }

    public List<String> hooksFor(Niche niche) {
        return HOOKS.getOrDefault(niche, HOOKS.get(Niche.GENERAL));
    }
}
