package extraordinary.ideas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class CreativeNotes {
    public void saveDraft(String text) {
        System.out.println("[Saved Draft]\n" + text);
    }

    public String saveDraftToFile(String text) {
        try {
            Path dir = Paths.get("drafts");
            Files.createDirectories(dir);
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path file = dir.resolve("plan_" + timestamp + ".txt");
            String header = "# The Extordinaire Draft (" + timestamp + ")\n\n";
            Files.writeString(file, header + text, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return file.toAbsolutePath().toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
