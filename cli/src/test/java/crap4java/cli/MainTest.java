package crap4java.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {

    /** Set by the Gradle test task; falls back to the path relative to the cli module for IDE runs. */
    private static final String FIXTURE = System.getProperty("crap4java.fixture", "../core/src/test/resources/sample-jacoco.xml");

    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    @Test
    void reports_crappy_methods_and_exits_with_2() {
        int exit = run(FIXTURE);

        assertThat(exit).isEqualTo(Main.EXIT_THRESHOLD);
        assertThat(err()).isEmpty();
        Approvals.verify(out().replace(FIXTURE, "sample-jacoco.xml"));
    }

    @Test
    void exits_with_0_when_nothing_exceeds_threshold() {
        int exit = run("--threshold", "1000", FIXTURE);

        assertThat(exit).isEqualTo(Main.EXIT_OK);
        assertThat(out()).contains("verdict=OK").doesNotContain("Uncovered.classify");
    }

    @Test
    void top_lists_worst_methods_regardless_of_threshold() {
        int exit = run("--threshold", "1000", "--top", "1", FIXTURE);

        assertThat(exit).isEqualTo(Main.EXIT_OK);
        assertThat(out()).contains("Uncovered.classify").doesNotContain("Pipelines.upper");
    }

    @Test
    void all_lists_every_method() {
        run("--all", FIXTURE);

        assertThat(out()).contains("sample.Calculator.<init>()V");
    }

    @Test
    void merges_reports_of_different_modules(@TempDir Path dir) throws IOException {
        Path other = dir.resolve("other.xml");
        Files.writeString(other, """
                <report name="other"><package name="q"><class name="q/B" sourcefilename="B.java">
                <method name="m" desc="()V" line="1"><counter type="COMPLEXITY" missed="4" covered="0"/></method>
                </class></package></report>
                """);

        int exit = run("--threshold", "1000", FIXTURE, other.toString());

        assertThat(exit).isEqualTo(Main.EXIT_OK);
        assertThat(out()).contains("methods=31").contains("source=" + FIXTURE + "," + other);
    }

    @Test
    void refuses_the_same_class_from_two_reports() {
        int exit = run(FIXTURE, FIXTURE);

        assertThat(exit).isEqualTo(Main.EXIT_USAGE);
        assertThat(err()).startsWith("crap4java: class sample/").contains("merge the JaCoCo exec files");
    }

    @Test
    void warns_when_the_report_has_no_methods(@TempDir Path dir) throws IOException {
        Path empty = dir.resolve("empty.xml");
        Files.writeString(empty, "<report name=\"empty\"/>");

        int exit = run(empty.toString());

        assertThat(exit).isEqualTo(Main.EXIT_OK);
        assertThat(err()).isEqualTo("crap4java: warning: no methods found in " + empty + "; is this the right report?\n");
        assertThat(out()).contains("methods=0").contains("verdict=OK");
    }

    @Test
    void all_and_top_are_mutually_exclusive() {
        assertThat(run("--all", "--top", "2", FIXTURE)).isEqualTo(Main.EXIT_USAGE);
        assertThat(err()).contains("--all and --top are mutually exclusive");
    }

    @Test
    void help_wins_over_other_arguments() {
        assertThat(run("--bogus", "--help")).isEqualTo(Main.EXIT_OK);
        assertThat(out()).startsWith("usage:");
    }

    @Test
    void directory_as_report_exits_with_1() {
        int exit = run(".");

        assertThat(exit).isEqualTo(Main.EXIT_USAGE);
        assertThat(err()).isEqualTo("crap4java: cannot read report: not a regular file: .\n");
    }

    @Test
    void help_prints_usage_and_exits_with_0() {
        int exit = run("--help");

        assertThat(exit).isEqualTo(Main.EXIT_OK);
        assertThat(out()).startsWith("usage: crap4java");
        assertThat(err()).isEmpty();
    }

    @Test
    void no_arguments_is_a_usage_error() {
        int exit = run();

        assertThat(exit).isEqualTo(Main.EXIT_USAGE);
        assertThat(err()).startsWith("crap4java: no JaCoCo XML report given").contains("usage:");
        assertThat(out()).isEmpty();
    }

    @Test
    void invalid_options_are_usage_errors() {
        assertThat(run("--bogus", FIXTURE)).isEqualTo(Main.EXIT_USAGE);
        assertThat(run("--threshold", "abc", FIXTURE)).isEqualTo(Main.EXIT_USAGE);
        assertThat(run("--threshold", "-1", FIXTURE)).isEqualTo(Main.EXIT_USAGE);
        assertThat(run("--threshold", "Infinity", FIXTURE)).isEqualTo(Main.EXIT_USAGE);
        assertThat(run("--threshold")).isEqualTo(Main.EXIT_USAGE);
        assertThat(run("--top", "0", FIXTURE)).isEqualTo(Main.EXIT_USAGE);
        assertThat(err()).contains("unknown option: --bogus")
                .contains("--threshold must be a number, got abc")
                .contains("--threshold must be a non-negative number, got -1")
                .contains("--threshold requires a value")
                .contains("--top must be a positive integer, got 0");
    }

    @Test
    void missing_report_file_exits_with_1() {
        int exit = run("does-not-exist.xml");

        assertThat(exit).isEqualTo(Main.EXIT_USAGE);
        assertThat(err()).isEqualTo("crap4java: cannot read report: no such file: does-not-exist.xml\n");
    }

    @Test
    void non_jacoco_xml_exits_with_1() {
        int exit = run("build.gradle.kts");

        assertThat(exit).isEqualTo(Main.EXIT_USAGE);
        assertThat(err()).startsWith("crap4java: Unable to parse JaCoCo XML");
    }

    private int run(String... args) {
        return Main.run(args, new PrintStream(stdout, true, StandardCharsets.UTF_8), new PrintStream(stderr, true, StandardCharsets.UTF_8));
    }

    private String out() {
        return stdout.toString(StandardCharsets.UTF_8);
    }

    private String err() {
        return stderr.toString(StandardCharsets.UTF_8);
    }
}
