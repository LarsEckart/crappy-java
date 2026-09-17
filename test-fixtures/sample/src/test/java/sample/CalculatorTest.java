package sample;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CalculatorTest {

    private final Calculator calculator = new Calculator();

    @Test
    void covered_all_branches() {
        assertThat(calculator.covered(-5)).isEqualTo(-1);
        assertThat(calculator.covered(0)).isEqualTo(0);
        assertThat(calculator.covered(500)).isEqualTo(100);
        assertThat(calculator.covered(7)).isEqualTo(7);
    }

    @Test
    void partial_only_happy_path() {
        assertThat(calculator.partial(7)).isEqualTo(7);
    }

    @Test
    void overloaded_int_only() {
        assertThat(calculator.overloaded(3)).isEqualTo(3);
        assertThat(calculator.overloaded(-3)).isEqualTo(0);
    }
}
