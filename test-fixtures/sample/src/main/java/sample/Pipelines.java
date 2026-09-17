package sample;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Pipelines {

    /** Lambda in a static initializer: javac emits lambda$static$N. */
    static final Set<String> STOP_WORDS = Stream.of("a", "the", "of")
            .map(s -> s.length() > 1 ? s.toUpperCase() : s)
            .collect(Collectors.toSet());

    private final List<String> names;
    private final int threshold;

    /** Lambda in a constructor: javac emits lambda$new$N. Constructor itself has a branch. */
    public Pipelines(List<String> names) {
        this.names = names.stream()
                .filter(n -> n != null && !n.isBlank())
                .toList();
        this.threshold = names.size() > 3 ? 3 : 1;
    }

    /** Lambda with a branch inside: complexity lives in lambda$countLong$N, not here. */
    public long countLong() {
        return names.stream()
                .filter(n -> n.length() > threshold)
                .count();
    }

    /** Two lambdas, and a branch in the method body itself. */
    public List<String> upper(boolean sorted) {
        Stream<String> stream = names.stream()
                .filter(n -> !STOP_WORDS.contains(n.toUpperCase()))
                .map(n -> n.isEmpty() ? "?" : n.toUpperCase());
        if (sorted) {
            stream = stream.sorted();
        }
        return stream.toList();
    }
}
