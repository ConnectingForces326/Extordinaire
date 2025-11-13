package extraordinary.content.hooks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * HookTemplates
 * --------------
 * Fundamental repository + API for intro/hook formats.
 * Placeholders: {X}, {Y}, {Z} (AI fills later).
 *
 * Usage examples:
 *   var t = HookTemplates.randomTemplate();
 *   String rendered = HookTemplates.render(t, Map.of("X","intermittent fasting","Y","your metabolism"));
 *
 * Notes:
 * - Keep this PURE (no network/AI). The AI layer chooses a template and supplies values.
 * - All strings are short-form optimized (Shorts/Reels/TikTok).
 */
public final class HookTemplates {

    /** Psychological trigger buckets for analytics & A/B routing. */
    public enum Category {
        SHOCK_CURIOSITY,
        CHALLENGE_REVERSAL,
        EMOTIONAL_RELATABLE,
        VALUE_EDU,
        NARRATIVE_MYSTERY,
        VISUAL_DEMO,
        LIST_COUNTDOWN,
        CONVERSATIONAL_DIRECT
    }

    /** Immutable template model. */
    public record HookTemplate(
            String id,                // stable id for telemetry, e.g. "SC-01"
            Category category,
            String template,          // text with {X}/{Y}/{Z}
            String note               // quick guidance for the AI/director
    ) {}

    private HookTemplates() {}

    // ===== Repository =====
    private static final List<HookTemplate> ALL = List.of(
        // ---- 1) Shock / Curiosity ----
        t("SC-01", Category.SHOCK_CURIOSITY, "You wouldn’t believe what happens when {X}.", "Lead with surprise; immediate visual if possible."),
        t("SC-02", Category.SHOCK_CURIOSITY, "Nobody told you {X} was this powerful.", "Great for under-rated tools/habits."),
        t("SC-03", Category.SHOCK_CURIOSITY, "I tried {X} for 24 hours — here’s what broke.", "Show failure or unexpected side-effect quickly."),
        t("SC-04", Category.SHOCK_CURIOSITY, "This one thing about {X} makes no sense…", "Cut to the contradiction in ≤2s."),
        t("SC-05", Category.SHOCK_CURIOSITY, "The truth about {X} nobody talks about.", "Pairs with receipts/data overlay."),

        // ---- 2) Challenge / Reverse Expectation ----
        t("CR-01", Category.CHALLENGE_REVERSAL, "Everyone says {X} doesn’t work — until you do it like this.", "Tease a technique; reveal step 1 fast."),
        t("CR-02", Category.CHALLENGE_REVERSAL, "You’ve been doing {X} wrong your whole life.", "Show the wrong way on screen."),
        t("CR-03", Category.CHALLENGE_REVERSAL, "I bet you can’t {Y} without {X}.", "Set up a challenge the viewer imagines doing."),
        t("CR-04", Category.CHALLENGE_REVERSAL, "Watch me prove {X} completely wrong.", "On-screen timer or progress bar helps."),
        t("CR-05", Category.CHALLENGE_REVERSAL, "If you think {X} is easy, try this.", "Great for fitness, dev, finance tasks."),

        // ---- 3) Emotional / Relatable ----
        t("ER-01", Category.EMOTIONAL_RELATABLE, "You ever feel like {X} no matter what you do?", "Hook by naming the pain."),
        t("ER-02", Category.EMOTIONAL_RELATABLE, "That moment when {X} hits out of nowhere…", "Cut to POV/first-person shot."),
        t("ER-03", Category.EMOTIONAL_RELATABLE, "I didn’t realize {Y} was ruining my {X}.", "Confession → quick fix."),
        t("ER-04", Category.EMOTIONAL_RELATABLE, "If {X} keeps happening to you, this is for you.", "Promise relief in <5s."),
        t("ER-05", Category.EMOTIONAL_RELATABLE, "What nobody tells you about {X} until it’s too late.", "High-stakes framing."),

        // ---- 4) Value-Driven / Educational ----
        t("VE-01", Category.VALUE_EDU, "Here’s the {X} hack nobody’s showing you.", "Start with the result on screen."),
        t("VE-02", Category.VALUE_EDU, "3 reasons your {X} never works — and how to fix it.", "Numbers keep pacing."),
        t("VE-03", Category.VALUE_EDU, "You’re missing this simple {X} rule.", "Zoom on the rule; then demo."),
        t("VE-04", Category.VALUE_EDU, "Why {X} outperforms {Y} — the quick math.", "Show a tiny table/graph."),
        t("VE-05", Category.VALUE_EDU, "The science behind why {X} actually works.", "One-line mechanism → example."),

        // ---- 5) Narrative / Mystery ----
        t("NM-01", Category.NARRATIVE_MYSTERY, "It all started when {X} happened…", "Cut with ambient SFX."),
        t("NM-02", Category.NARRATIVE_MYSTERY, "I didn’t expect {X} to change everything.", "Show the ‘before’ frame 1."),
        t("NM-03", Category.NARRATIVE_MYSTERY, "There was only one rule: never {X}.", "Break the rule at :02."),
        t("NM-04", Category.NARRATIVE_MYSTERY, "By the time I realized {Y}, it was already too late.", "Use timestamp overlays."),
        t("NM-05", Category.NARRATIVE_MYSTERY, "This story about {X} still messes with my head.", "Good for cautionary tales."),

        // ---- 6) Visual / Demonstrative ----
        t("VD-01", Category.VISUAL_DEMO, "Watch what happens when I put {X} into {Y}.", "Immediate cut to action."),
        t("VD-02", Category.VISUAL_DEMO, "24 hours later, {X} looks like this.", "Use jump-cut or timelapse."),
        t("VD-03", Category.VISUAL_DEMO, "I poured {X} on {Y} — the reaction is wild.", "Safety disclaimer if needed."),
        t("VD-04", Category.VISUAL_DEMO, "Don’t blink — you’ll miss {X}.", "Micro-movement or reveal."),
        t("VD-05", Category.VISUAL_DEMO, "Here’s {X} before and after {Y}.", "Split-screen transform."),

        // ---- 7) List / Countdown ----
        t("LC-01", Category.LIST_COUNTDOWN, "Top 3 ways to fix {X} — number 1 will surprise you.", "Keep #s on screen."),
        t("LC-02", Category.LIST_COUNTDOWN, "5 things you’re doing that destroy {X}.", "Rapid cuts; 1 tip/beat."),
        t("LC-03", Category.LIST_COUNTDOWN, "The 3 tools that changed my {X} forever.", "Pin a free option."),
        t("LC-04", Category.LIST_COUNTDOWN, "Before you {Y}, learn these {X} rules.", "Swap X/Y as needed."),
        t("LC-05", Category.LIST_COUNTDOWN, "2 mistakes that cost me {Y} in {X}.", "Quantify the pain."),

        // ---- 8) Conversational / Direct ----
        t("CD-01", Category.CONVERSATIONAL_DIRECT, "Listen — if you’re doing {X}, stop.", "Firm, short, confident."),
        t("CD-02", Category.CONVERSATIONAL_DIRECT, "You and I both know {X} isn’t working anymore.", "Empathy → fix."),
        t("CD-03", Category.CONVERSATIONAL_DIRECT, "I’m gonna be real: {X} changed everything for me.", "Quick proof shot."),
        t("CD-04", Category.CONVERSATIONAL_DIRECT, "Here’s why your {X} strategy keeps failing.", "Name the root cause."),
        t("CD-05", Category.CONVERSATIONAL_DIRECT, "You’re not crazy — {X} actually is harder now.", "Offer a workaround.")
    );

    private static HookTemplate t(String id, Category c, String s, String note) {
        return new HookTemplate(id, c, s, note);
    }

    // ===== Public API =====

    /** All templates (immutable). */
    public static List<HookTemplate> all() { return ALL; }

    /** Templates of a given category. */
    public static List<HookTemplate> byCategory(Category category) {
        return ALL.stream().filter(t -> t.category == category).collect(Collectors.toUnmodifiableList());
    }

    /** Look up by id (e.g., "SC-01"). Returns Optional.empty() if not found. */
    public static Optional<HookTemplate> byId(String id) {
        return ALL.stream().filter(t -> t.id.equalsIgnoreCase(id)).findFirst();
    }

    /** Uniform random template. */
    public static HookTemplate randomTemplate() {
        return ALL.get(ThreadLocalRandom.current().nextInt(ALL.size()));
    }

    /** Random template from a category. */
    public static HookTemplate randomTemplate(Category category) {
        var list = byCategory(category);
        if (list.isEmpty()) throw new IllegalArgumentException("No templates for category: " + category);
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    /**
     * Render the template by replacing {keys} with values.
     * Unknown placeholders are left as-is so the AI can fill them later if desired.
     */
    public static String render(HookTemplate template, Map<String, String> values) {
        String out = template.template;
        if (values != null) {
            for (var e : values.entrySet()) {
                out = out.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
            }
        }
        return out;
    }

    /** Convenience varargs renderer: render(t, "X","creatine", "Y","your recovery"). */
    public static String render(HookTemplate template, String... kv) {
        if (kv == null || kv.length == 0) return template.template;
        if (kv.length % 2 != 0) throw new IllegalArgumentException("Key/value args must be even length.");
        Map<String,String> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i+1]);
        return render(template, m);
    }
}
