package extraordinary.sandbox.hooksdemo;

import java.util.List;

import extraordinary.content.hooks.HookSelector;
import extraordinary.content.hooks.HookService;
import extraordinary.content.hooks.HookTemplates;
import extraordinary.content.hooks.RandomSelector;
import extraordinary.content.hooks.SimpleFiller;

public final class DemoBody {
    public static void main(String[] args) {
        HookService svc = new HookService(new RandomSelector(), new SimpleFiller());
        String topic = argOr(args,0,"creatine");
        String niche = argOr(args,1,"fitness");
        String style = argOr(args,2,"educational");

        var hook = svc.generateHookLine(topic, niche, style, HookSelector.SelectorContext.none());
        var tpl  = HookTemplates.byId(hook.id()).orElseThrow();

        List<String> beats = switch (tpl.category()) {
            case VALUE_EDU            -> List.of("Problem (1 line).", "Tip #1.", "Tip #2.", "Rule.", "Payoff.", "CTA.");
            case CHALLENGE_REVERSAL   -> List.of("Wrong way.", "Reversal.", "Quick demo.", "Why it works.", "Your turn.");
            default                   -> List.of("Context.", "Proof/Demo.", "Takeaway.");
        };

        System.out.println("Hook: " + hook.text());
        System.out.println("Beats:");
        for (int i = 0; i < beats.size(); i++) System.out.println("  " + (i+1) + ") " + beats.get(i));
    }

    private static String argOr(String[] a, int i, String d) {
        return (a!=null && a.length>i && a[i]!=null && !a[i].isBlank()) ? a[i] : d;
    }
}
