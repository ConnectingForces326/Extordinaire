package extraordinary.content.hooks;

import java.util.List;

import extraordinary.logic.TimelineAllocator;
import extraordinary.logic.TimelineAllocator.Mode;
import extraordinary.logic.TimelineAllocator.Spec;
import extraordinary.models.VideoPlan;
import extraordinary.models.VideoPlan.SegmentType;

public class PromptStructureHook {
    public static VideoPlan build(int totalSeconds) {
        List<Spec> specs = List.of(
            new Spec("hook", SegmentType.HOOK,
                "Steal this yurr prompt structure (insane results).", Mode.FIXED, 3),
            new Spec("step-1", SegmentType.STEP,
                "Step 1 — Foundation: set up the basics for yurr.", Mode.PERCENT, 15),
            new Spec("step-2", SegmentType.STEP,
                "Step 2 — Apply: Plan → Execute → Review loop", Mode.AUTO, 2),
            new Spec("step-3", SegmentType.STEP,
                "Step 3 — Practice: small variation to learn faster.", Mode.AUTO, 2),
            new Spec("step-4", SegmentType.STEP,
                "Step 4 — Review: check one metric weekly.", Mode.AUTO, 2)
        );

        Spec cta = new Spec("cta", SegmentType.CTA,
                "Follow for step-by-step guides", Mode.FIXED, 5);

        return TimelineAllocator.allocate(
            totalSeconds,  // 30, 45, 60… any length
            specs,
            cta,           // or null if no CTA
            1              // snap to whole seconds
        );
    }
}
