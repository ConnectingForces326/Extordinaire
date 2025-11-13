package extraordinary.helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonUtil {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    // ---------- SAVE ----------

    /** Save any object to a JSON file (String path). */
    public static <T> void save(String path, T data) throws IOException {
        save(Paths.get(path), data);
    }

    /** Save any object to a JSON file (Path). */
    public static <T> void save(Path path, T data) throws IOException {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path)) {
            gson.toJson(data, writer);
        }
    }

    // ---------- LOAD (simple Class<T>) ----------

    /** Load a JSON file into an object (String path + Class). */
    public static <T> T load(String path, Class<T> type) throws IOException {
        return load(Paths.get(path), type);
    }

    /** Load a JSON file into an object (Path + Class). */
    public static <T> T load(Path path, Class<T> type) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return gson.fromJson(reader, type);
        }
    }

    // ---------- LOAD (generic Type, e.g. List<Something>) ----------

    /** Load using a Type token (for lists, maps, etc.). */
    public static <T> T load(String path, Type type) throws IOException {
        return load(Paths.get(path), type);
    }

    public static <T> T load(Path path, Type type) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return gson.fromJson(reader, type);
        }
    }

    // ---------- STRING HELPERS ----------

    public static String toJson(Object o) {
        return gson.toJson(o);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return gson.fromJson(json, type);
    }

    public static <T> T fromJson(String json, Type type) {
        return gson.fromJson(json, type);
    }
}
