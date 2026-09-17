package crap4java.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LambdaFolderTest {

    private final List<MethodCoverage> folded = LambdaFolder.fold(Fixture.report().methods());

    @Test
    void maps_lambda_names_to_enclosing_methods() {
        assertThat(LambdaFolder.enclosingMethodName("lambda$countLong$0")).isEqualTo("countLong");
        assertThat(LambdaFolder.enclosingMethodName("lambda$new$3")).isEqualTo("<init>");
        assertThat(LambdaFolder.enclosingMethodName("lambda$static$0")).isEqualTo("<clinit>");
        assertThat(LambdaFolder.enclosingMethodName("lambda$with$dollar$1")).isEqualTo("with$dollar");
        assertThat(LambdaFolder.enclosingMethodName("countLong")).isNull();
        assertThat(LambdaFolder.enclosingMethodName("lambda$broken")).isNull();
    }

    @Test
    void folds_unambiguous_lambdas_into_their_method() {
        MethodCoverage upper = find("sample/Pipelines", "upper");
        // own (2 missed) + lambda$upper$0 (2 missed) + lambda$upper$1 (2 missed)
        assertThat(upper.complexity()).isEqualTo(new Counter(6, 0));

        MethodCoverage countLong = find("sample/Pipelines", "countLong");
        assertThat(countLong.complexity()).isEqualTo(new Counter(0, 3));
    }

    @Test
    void folds_constructor_and_static_initializer_lambdas() {
        MethodCoverage constructor = find("sample/Pipelines", "<init>");
        // own (1 missed, 1 covered) + lambda$new$0 (2 missed, 1 covered)
        assertThat(constructor.complexity()).isEqualTo(new Counter(3, 2));

        MethodCoverage clinit = find("sample/Pipelines", "<clinit>");
        assertThat(clinit.complexity()).isEqualTo(new Counter(0, 3));
    }

    @Test
    void no_lambda_entries_remain_where_folding_was_possible() {
        assertThat(folded)
                .filteredOn(m -> m.className().equals("sample/Pipelines"))
                .extracting(MethodCoverage::name)
                .containsExactlyInAnyOrder("<init>", "<clinit>", "countLong", "upper");
    }

    @Test
    void keeps_lambdas_of_overloaded_methods_separate() {
        assertThat(folded)
                .filteredOn(m -> m.className().equals("sample/Overloads"))
                .extracting(MethodCoverage::name)
                .containsExactlyInAnyOrder("<init>", "map", "map", "lambda$map$0", "lambda$map$1");
        assertThat(find("sample/Overloads", "lambda$map$1").complexity()).isEqualTo(new Counter(2, 0));
    }

    @Test
    void keeps_lambda_without_enclosing_method() {
        MethodCoverage orphan = method("p/A", "lambda$gone$0", 1, 0);

        assertThat(LambdaFolder.fold(List.of(orphan))).containsExactly(orphan);
    }

    @Test
    void folding_preserves_other_counters_and_identity_of_the_enclosing_method() {
        MethodCoverage host = new MethodCoverage("p/A", "A.java", "run", "()V", 10,
                Map.of(CounterType.COMPLEXITY, new Counter(1, 1), CounterType.INSTRUCTION, new Counter(0, 5)));
        MethodCoverage lambda = new MethodCoverage("p/A", "A.java", "lambda$run$0", "(I)Z", 12,
                Map.of(CounterType.COMPLEXITY, new Counter(2, 0), CounterType.BRANCH, new Counter(2, 0)));

        List<MethodCoverage> result = LambdaFolder.fold(List.of(host, lambda));

        assertThat(result).containsExactly(new MethodCoverage("p/A", "A.java", "run", "()V", 10, Map.of(
                CounterType.COMPLEXITY, new Counter(3, 1),
                CounterType.INSTRUCTION, new Counter(0, 5),
                CounterType.BRANCH, new Counter(2, 0))));
    }

    @Test
    void does_not_fold_across_classes() {
        MethodCoverage host = method("p/A", "run", 1, 1);
        MethodCoverage lambdaElsewhere = method("p/B", "lambda$run$0", 2, 0);

        assertThat(LambdaFolder.fold(List.of(host, lambdaElsewhere))).containsExactly(host, lambdaElsewhere);
    }

    private MethodCoverage find(String className, String name) {
        return folded.stream()
                .filter(m -> m.className().equals(className) && m.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no " + className + "." + name));
    }

    private static MethodCoverage method(String className, String name, int missed, int covered) {
        return new MethodCoverage(className, "X.java", name, "()V", 1,
                Map.of(CounterType.COMPLEXITY, new Counter(missed, covered)));
    }
}
