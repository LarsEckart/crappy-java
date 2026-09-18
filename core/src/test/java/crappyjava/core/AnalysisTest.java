package crappyjava.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnalysisTest {

    private final Analysis analysis = Analysis.of(Fixture.report(), 30.0);

    @Test
    void lambdas_are_folded_before_scoring() {
        // 35 raw methods, 5 lambdas folded (static, new, countLong, upper x2), 2 kept (ambiguous map overloads)
        assertThat(analysis.methods()).hasSize(30);
    }

    @Test
    void worst_method_comes_first() {
        MethodCrap worst = analysis.methods().get(0);

        assertThat(worst.signature()).isEqualTo("sample.Uncovered.classify(IILjava/lang/String;)Ljava/lang/String;");
        assertThat(worst.location()).isEqualTo("Uncovered.java:8");
        assertThat(worst.complexity()).isEqualTo(13);
        assertThat(worst.coverage()).isEqualTo(0.0);
        assertThat(worst.crap()).isEqualTo(182.0);
    }

    @Test
    void summary_values() {
        assertThat(analysis.maxCrap()).isEqualTo(182.0);
        assertThat(analysis.thresholdExceeded()).isTrue();
        assertThat(analysis.crappy()).extracting(MethodCrap::signature)
                .containsExactly(
                        "sample.Uncovered.classify(IILjava/lang/String;)Ljava/lang/String;",
                        "sample.Pipelines.upper(Z)Ljava/util/List;");
        assertThat(analysis.crappyPercentage()).isCloseTo(100.0 * 2 / 30, within(0.001));
    }

    @Test
    void threshold_is_exclusive() {
        Analysis exact = Analysis.of(Fixture.report(), 182.0);

        assertThat(exact.thresholdExceeded()).isFalse();
        assertThat(exact.crappy()).isEmpty();
    }

    @Test
    void ties_are_ordered_by_class_then_line() {
        MethodCoverage b = new MethodCoverage("p/B", "B.java", "m", "()V", 5, Map.of(CounterType.COMPLEXITY, new Counter(1, 0)));
        MethodCoverage a2 = new MethodCoverage("p/A", "A.java", "m", "()V", 9, Map.of(CounterType.COMPLEXITY, new Counter(1, 0)));
        MethodCoverage a1 = new MethodCoverage("p/A", "A.java", "m", "()V", 3, Map.of(CounterType.COMPLEXITY, new Counter(1, 0)));

        Analysis sorted = Analysis.of(new JacocoReport("t", List.of(b, a2, a1)), 30.0);

        assertThat(sorted.methods()).extracting(MethodCrap::location).containsExactly("A.java:3", "A.java:9", "B.java:5");
    }

    @Test
    void empty_report() {
        Analysis empty = Analysis.of(new JacocoReport("empty", List.of()), 30.0);

        assertThat(empty.maxCrap()).isEqualTo(0.0);
        assertThat(empty.thresholdExceeded()).isFalse();
        assertThat(empty.crappyPercentage()).isEqualTo(0.0);
    }

    @Test
    void location_without_source_information() {
        MethodCoverage noSource = new MethodCoverage("p/A", null, "m", "()V", 0, Map.of(CounterType.COMPLEXITY, new Counter(1, 0)));

        assertThat(MethodCrap.of(noSource).location()).isEqualTo("?");
    }
}
