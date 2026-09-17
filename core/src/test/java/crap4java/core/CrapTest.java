package crap4java.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class CrapTest {

    @Test
    void uncovered_method_scores_cc_squared_plus_cc() {
        assertThat(Crap.score(20, 0.0)).isEqualTo(420.0);
        assertThat(Crap.score(13, 0.0)).isEqualTo(182.0);
        assertThat(Crap.score(1, 0.0)).isEqualTo(2.0);
    }

    @Test
    void fully_covered_method_scores_its_complexity() {
        assertThat(Crap.score(20, 1.0)).isEqualTo(20.0);
        assertThat(Crap.score(1, 1.0)).isEqualTo(1.0);
    }

    @Test
    void partial_coverage_is_cubed() {
        assertThat(Crap.score(12, 1.0 / 3.0)).isCloseTo(54.6667, within(0.0001));
        assertThat(Crap.score(4, 0.25)).isEqualTo(10.75);
    }

    @Test
    void rejects_coverage_outside_unit_interval() {
        assertThatThrownBy(() -> Crap.score(1, 1.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Crap.score(1, -0.1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void counter_ratio_handles_empty_counter() {
        assertThat(Counter.ZERO.coveredRatio()).isEqualTo(0.0);
        assertThat(new Counter(1, 3).coveredRatio()).isEqualTo(0.75);
        assertThat(new Counter(1, 2).plus(new Counter(3, 4))).isEqualTo(new Counter(4, 6));
    }
}
