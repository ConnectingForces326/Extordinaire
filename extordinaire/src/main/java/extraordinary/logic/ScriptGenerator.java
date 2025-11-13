package extraordinary.logic;

import java.util.Arrays;
import java.util.List;

import extraordinary.helpers.RandomUtil;
import extraordinary.models.ContentRequest;

public class ScriptGenerator {
    private final RandomUtil R = new RandomUtil();

    public String generate(ContentRequest req, String hook) {
        return switch (req.style()) {
            case HYPE -> hypeScript(req, hook);
            case TEACHER -> teacherScript(req, hook);
            case ANALYST -> analystScript(req, hook);
        };
    }

    // ---------------- HYPE: fast, punchy, emoji-friendly ----------------
    private String hypeScript(ContentRequest req, String hook) {
        String t = req.topic();
        List<String> beats = Arrays.asList(
            span(3,8, blast("Quick win:", oneLiner(t))),
            span(8,16, blast("Do this:", actionLine(t))),
            span(16,26, blast("Proof:", proofLine(t))),
            span(26,40, blast("Try now:", demoLine(t))),
            span(40,55, blast("Level up:", tipLineHype()))
        );
        StringBuilder sb = new StringBuilder();
        sb.append(span(0,3, hook)).append("\n");
        for (String line : R.shuffled(beats)) sb.append(line).append("\n");
        sb.append(span(55,60, "CTA ( " + ctaHype() + " )"));
        return sb.toString();
    }

    // ---------------- TEACHER: clear steps, calm tone ----------------
    private String teacherScript(ContentRequest req, String hook) {
        String t = req.topic();
        List<String> beats = Arrays.asList(
            span(3,12, "Step 1 — Foundation: set up the basics for " + t + "."),
            span(12,24, "Step 2 — Apply: use a simple method: " + teacherMethod(t)),
            span(24,38, "Step 3 — Practice: repeat with a small variation to learn faster."),
            span(38,52, "Step 4 — Review: check one metric and adjust weekly.")
        );
        StringBuilder sb = new StringBuilder();
        sb.append(span(0,3, hook)).append("\n");
        for (String line : beats) sb.append(line).append("\n"); // teacher = ordered, not shuffled
        sb.append(span(55,60, "CTA ( " + ctaTeacher() + " )"));
        return sb.toString();
    }

    // ---------------- ANALYST: evidence, outcomes, concise ----------------
    private String analystScript(ContentRequest req, String hook) {
        String t = req.topic();
        List<String> beats = Arrays.asList(
            span(3,12, "Baseline: define 1 measurable outcome for " + t + "."),
            span(12,22, "Intervention: apply " + analystIntervention(t) + "."),
            span(22,34, "Result: expect " + analystOutcome() + " in early trials."),
            span(34,50, "Example: small test on 5 samples; compare against control."),
            span(50,55, "Decision: keep if delta ≥ " + R.pick("10%", "15%", "20%") + ".")
        );
        StringBuilder sb = new StringBuilder();
        sb.append(span(0,3, hook)).append("\n");
        for (String line : beats) sb.append(line).append("\n"); // analyst = ordered, crisp
        sb.append(span(55,60, "CTA ( " + ctaAnalyst() + " )"));
        return sb.toString();
    }

    // ---------------- Phrase helpers ----------------
    private String oneLiner(String t) {
        return R.pick(
            "Use a 3-beat hook for " + t,
            "Cut fluff — keep only gains in " + t,
            "Swap 1 setting to boost " + t,
            "Borrow a template; ship faster in " + t
        );
    }

    private String actionLine(String t) {
        return R.pick(
            "Apply the best 20% that moves " + t,
            "Copy this layout; adjust once",
            "Timebox edits; prioritize the first 10s in " + t
        );
    }

    private String proofLine(String t) {
        return R.pick(
            "Before/after difference is obvious in " + t,
            "1 change improves speed, quality, or reach",
            "Repeatable results in 3 tries"
        );
    }

    private String demoLine(String t) {
        return R.pick(
            "1 slider change — do it on " + t,
            "Mini-demo: duplicate → tweak → compare",
            "Template load → plug inputs → export"
        );
    }

    private String tipLineHype() {
        return R.pick(
            "Batch 5 hooks; post the winner",
            "Kill dead seconds; cut to payoff",
            "End on a benefit, not a feature"
        );
    }

    private String teacherMethod(String t) {
        return R.pick(
            "Hook → Proof → Payoff for " + t,
            "Plan → Execute → Review loop",
            "Copy → Modify → Compare (CMC)"
        );
    }

    private String analystIntervention(String t) {
        return R.pick(
            "one variable change in " + t,
            "a template baseline for " + t,
            "a noise-reduction step in " + t
        );
    }

    private String analystOutcome() {
        return R.pick(
            "faster completion time",
            "higher first-10s retention",
            "lower rework rate",
            "clearer output consistency"
        );
    }

    private String ctaHype() {
        return R.pick(
            "Follow for 🔥 quick wins",
            "Save this — try it today",
            "Subscribe for fast breakdowns"
        );
    }

    private String ctaTeacher() {
        return R.pick(
            "Follow for step-by-step guides",
            "Save this lesson",
            "Subscribe for weekly walkthroughs"
        );
    }

    private String ctaAnalyst() {
        return R.pick(
            "Subscribe for data-backed methods",
            "Save if you value proven playbooks",
            "Follow for tested frameworks"
        );
    }

    // ---------------- Formatting helpers ----------------
    private static String span(int start, int end, String text) {
        return String.format("%d–%ds: %s", start, end, text);
    }

    private String blast(String label, String text) {
        // Add a little energy marker sometimes
        String marker = R.pick("", " ⚡", " 🔥", " ✅");
        return label + " " + text + marker;
    }
}
