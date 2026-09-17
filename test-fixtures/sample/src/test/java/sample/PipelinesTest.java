package sample;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PipelinesTest {

    @Test
    void count_long_names() {
        Pipelines pipelines = new Pipelines(List.of("ab", "abcd", "x"));
        assertThat(pipelines.countLong()).isEqualTo(2);
    }
}
