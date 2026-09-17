package sample;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class OverloadsTest {

    @Test
    void integer_overload_only() {
        assertThat(new Overloads().map(List.of(-1, 2))).containsExactly(1, 2);
    }
}
