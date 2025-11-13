package extraordinary.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomUtil {
    private final Random rng;

    public RandomUtil(long seed) { this.rng = new Random(seed); }
    public RandomUtil() { this(System.nanoTime()); }

    public <T> T pick(List<T> items) { return items.get(rng.nextInt(items.size())); }
    public String pick(String... items) { return items[rng.nextInt(items.length)]; }

    public <T> List<T> shuffled(List<T> list) {
        List<T> copy = new ArrayList<>(list);
        Collections.shuffle(copy, rng);
        return copy;
    }
}
