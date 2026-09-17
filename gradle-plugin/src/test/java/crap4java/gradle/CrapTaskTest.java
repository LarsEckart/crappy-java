package crap4java.gradle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.VerificationException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Runs the task action in-process so its branches are covered and its errors can be asserted precisely. */
class CrapTaskTest {

    private static final Path FIXTURE = Path.of(System.getProperty("crap4java.fixture", "../core/src/test/resources/sample-jacoco.xml"));

    @TempDir
    Path projectDir;

    private CrapTask task;
    private Path outputFile;

    @BeforeEach
    void createTask() throws IOException {
        Files.copy(FIXTURE, projectDir.resolve("sample-jacoco.xml"));
        Project project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build();
        project.getPluginManager().apply(Crap4JavaPlugin.class);
        task = project.getTasks().named(Crap4JavaPlugin.TASK_NAME, CrapTask.class).get();
        task.getReports().from("sample-jacoco.xml");
        outputFile = task.getOutputFile().get().getAsFile().toPath();
    }

    @Test
    void fails_with_verification_exception_above_threshold_and_still_writes_the_report() throws IOException {
        assertThatThrownBy(task::run)
                .isInstanceOf(VerificationException.class)
                .hasMessage("CRAP threshold exceeded: max=182.0 > threshold=30.0 (2 crappy method(s)). Report: build/reports/crap4java/crap.txt");

        String report = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertThat(report).startsWith("CRAP report  threshold=30.0  source=sample-jacoco.xml")
                .contains("sample.Uncovered.classify")
                .endsWith("methods=30 crappy=2 (6.7%) max=182.0 threshold=30.0 verdict=FAIL" + System.lineSeparator());
    }

    @Test
    void does_not_fail_when_fail_on_violation_is_off() throws IOException {
        task.getFailOnViolation().set(false);

        assertThatCode(task::run).doesNotThrowAnyException();
        assertThat(Files.readString(outputFile, StandardCharsets.UTF_8)).contains("verdict=FAIL");
    }

    @Test
    void passes_below_threshold_and_honours_show_all_and_top() throws IOException {
        task.getThreshold().set(1000.0);
        task.getTop().set(1);

        task.run();

        String report = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertThat(report).contains("sample.Uncovered.classify").doesNotContain("sample.Pipelines.upper").contains("verdict=OK");
    }

    @Test
    void missing_report_file_is_an_error() {
        task.getReports().setFrom("typo.xml");

        assertThatThrownBy(task::run)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("JaCoCo XML report not found")
                .hasMessageContaining("typo.xml");
    }

    @Test
    void invalid_report_is_an_error_with_the_parser_message() throws IOException {
        Files.writeString(projectDir.resolve("broken.xml"), "<report name='x'><class name='p/A'>");
        task.getReports().setFrom("broken.xml");

        assertThatThrownBy(task::run)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("Unable to parse JaCoCo XML");
    }

    @Test
    void empty_report_passes_and_is_reported_as_empty() throws IOException {
        Files.writeString(projectDir.resolve("empty.xml"), "<report name='empty'/>");
        task.getReports().setFrom("empty.xml");

        task.run();

        assertThat(Files.readString(outputFile, StandardCharsets.UTF_8)).contains("methods=0 crappy=0 (0.0%) max=0.0 threshold=30.0 verdict=OK");
    }
}
