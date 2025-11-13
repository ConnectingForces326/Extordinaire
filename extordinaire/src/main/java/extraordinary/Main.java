package extraordinary;

import java.util.Scanner;

import extraordinary.content.DynamicNicheChannel;
import extraordinary.helpers.HelperModules;
import extraordinary.ideas.CreativeNotes;
import extraordinary.logic.AlphaBlueprint;
import extraordinary.logic.AutoStyleSelector;
import extraordinary.logic.ConceptVision;
import extraordinary.logic.ScriptGenerator;
import extraordinary.models.ContentRequest;
import extraordinary.models.Niche;
import extraordinary.models.Style;
import extraordinary.models.VideoPlan;

public class Main {
    public static void main(String[] args) {
        System.out.println("The Extordinaire");

        ConceptVision vision = new ConceptVision();
        AlphaBlueprint blueprint = new AlphaBlueprint();
        ScriptGenerator scripter = new ScriptGenerator();
        DynamicNicheChannel channel = new DynamicNicheChannel();
        HelperModules helper = new HelperModules();
        CreativeNotes notes = new CreativeNotes();
        AutoStyleSelector auto = new AutoStyleSelector();

        // --- Input ---
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter a TOPIC: ");
        String topic = sc.nextLine().trim();
        if (topic.isEmpty()) topic = "AI video editing";

        System.out.print("Niche (GENERAL/AI/FITNESS/FINANCE/EDUCATION/GAMING/BEAUTY): ");
        String nicheStr = sc.nextLine().trim().toUpperCase();
        Niche niche;
        try { niche = Niche.valueOf(nicheStr); } catch (Exception e) { niche = Niche.GENERAL; }

        // Style: AUTO by default; allow manual override
        System.out.print("Style (press Enter or type 'AUTO' for auto; or HYPE/TEACHER/ANALYST): ");
        String styleStr = sc.nextLine().trim().toUpperCase();
        Style style;
        if (styleStr.isEmpty() || styleStr.equals("AUTO")) {
            style = auto.pick(niche);
            System.out.println("[Auto-selected style] " + style);
        } else {
            try { style = Style.valueOf(styleStr); }
            catch (Exception e) { style = auto.pick(niche); System.out.println("[Invalid style, auto-selected] " + style); }
        }
        sc.close();

        // --- Pipeline ---
        ContentRequest req = new ContentRequest(topic, "Universal", niche, style);
        String hook = vision.ideateHook(req);
        String script = scripter.generate(req, hook);
        VideoPlan plan = channel.assembleVideo(hook, script);

        // --- Output ---
        System.out.println("\n" + plan);

        String saved = notes.saveDraftToFile(plan.toString());
        if (saved != null) System.out.println("[Saved to] " + saved);
    }
}
