package sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PointTest {

    @Test
    void quadrants() {
        assertThat(new Point(1, 1).quadrant()).isEqualTo(1);
        assertThat(new Point(0, 5).quadrant()).isEqualTo(0);
    }

    @Test
    void rejects_negative() {
        assertThatThrownBy(() -> new Point(-1, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
