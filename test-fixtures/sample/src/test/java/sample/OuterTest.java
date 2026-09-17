package sample;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OuterTest {

    @Test
    void outer_and_nested() {
        assertThat(new Outer(10).outerMethod(3)).isEqualTo(7);
        assertThat(new Outer.Nested().nested(4)).isEqualTo(2);
    }
}
