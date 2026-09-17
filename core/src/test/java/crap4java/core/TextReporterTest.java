package crap4java.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;

class TextReporterTest {

    private static final List<String> SOURCES = List.of("sample-jacoco.xml");

    @Test
    void lists_only_crappy_methods_by_default() {
        Approvals.verify(TextReporter.render(Analysis.of(Fixture.report(), 30.0), SOURCES, ReportOptions.DEFAULT));
    }

    @Test
    void lists_every_method_with_show_all() {
        Approvals.verify(TextReporter.render(Analysis.of(Fixture.report(), 30.0), SOURCES, new ReportOptions(true, 0)));
    }

    @Test
    void lists_the_n_worst_methods_with_top() {
        Approvals.verify(TextReporter.render(Analysis.of(Fixture.report(), 1000.0), SOURCES, new ReportOptions(false, 3)));
    }

    @Test
    void prints_only_summary_when_nothing_is_crappy() {
        Approvals.verify(TextReporter.render(Analysis.of(Fixture.report(), 1000.0), SOURCES, ReportOptions.DEFAULT));
    }

    @Test
    void show_all_wins_over_top() {
        String all = TextReporter.render(Analysis.of(Fixture.report(), 30.0), SOURCES, new ReportOptions(true, 0));
        String both = TextReporter.render(Analysis.of(Fixture.report(), 30.0), SOURCES, new ReportOptions(true, 2));

        assertThat(both).isEqualTo(all);
    }

    @Test
    void empty_report_renders_summary_only() {
        String text = TextReporter.render(Analysis.of(new JacocoReport("empty", List.of()), 30.0), List.of("a.xml", "b.xml"), ReportOptions.DEFAULT);

        assertThat(text.lines()).containsExactly(
                "CRAP report  threshold=30.0  source=a.xml,b.xml",
                "",
                "methods=0 crappy=0 (0.0%) max=0.0 threshold=30.0 verdict=OK");
    }

    @Test
    void output_is_plain_ascii() {
        String text = TextReporter.render(Analysis.of(Fixture.report(), 30.0), SOURCES, new ReportOptions(true, 0));

        assertThat(text.chars().allMatch(c -> c == '\n' || c == '\r' || (c >= 0x20 && c < 0x7f))).isTrue();
    }
}
