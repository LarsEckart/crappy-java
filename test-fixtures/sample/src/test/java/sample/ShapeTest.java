package sample;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ShapeTest {

    @Test
    void circle_has_no_sides() {
        assertThat(Shape.CIRCLE.sides()).isEqualTo(0);
    }
}
