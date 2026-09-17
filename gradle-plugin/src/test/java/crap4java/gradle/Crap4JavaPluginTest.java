package crap4java.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testing.jacoco.tasks.JacocoReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** In-process tests of the plugin's wiring. Complements the TestKit tests, which run in a separate daemon. */
class Crap4JavaPluginTest {

    @TempDir
    Path projectDir;

    @Test
    void registers_extension_with_conventions_and_task() {
        Project project = project();
        project.getPluginManager().apply(Crap4JavaPlugin.class);

        Crap4JavaExtension extension = project.getExtensions().getByType(Crap4JavaExtension.class);
        CrapTask task = crapTask(project);

        assertThat(extension.getThreshold().get()).isEqualTo(30.0);
        assertThat(extension.getFailOnViolation().get()).isTrue();
        assertThat(extension.getShowAll().get()).isFalse();
        assertThat(extension.getTop().get()).isZero();
        assertThat(task.getGroup()).isEqualTo("verification");
        assertThat(task.getThreshold().get()).isEqualTo(30.0);
        assertThat(task.getOutputFile().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory().file("reports/crap4java/crap.txt").get().getAsFile());
        assertThat(task.getReports().getFrom()).isEmpty();
    }

    @Test
    void extension_values_flow_into_the_task_and_accept_integers() {
        Project project = project();
        project.getPluginManager().apply(Crap4JavaPlugin.class);
        Crap4JavaExtension extension = project.getExtensions().getByType(Crap4JavaExtension.class);

        extension.setThreshold(10);
        extension.getFailOnViolation().set(false);
        extension.getShowAll().set(true);
        extension.getTop().set(3);

        CrapTask task = crapTask(project);
        assertThat(task.getThreshold().get()).isEqualTo(10.0);
        assertThat(task.getFailOnViolation().get()).isFalse();
        assertThat(task.getShowAll().get()).isTrue();
        assertThat(task.getTop().get()).isEqualTo(3);
    }

    @Test
    void wires_jacoco_test_report_when_java_and_jacoco_are_applied() {
        Project project = project();
        project.getPluginManager().apply("java");
        project.getPluginManager().apply("jacoco");
        project.getPluginManager().apply(Crap4JavaPlugin.class);

        assertJacocoWired(project);
    }

    @Test
    void wires_jacoco_test_report_when_applied_before_java_and_jacoco() {
        Project project = project();
        project.getPluginManager().apply(Crap4JavaPlugin.class);
        project.getPluginManager().apply("jacoco");
        project.getPluginManager().apply("java-library");

        assertJacocoWired(project);
    }

    @Test
    void without_jacoco_nothing_is_wired() {
        Project project = project();
        project.getPluginManager().apply("java");
        project.getPluginManager().apply(Crap4JavaPlugin.class);

        assertThat(crapTask(project).getReports().getFrom()).isEmpty();
    }

    private void assertJacocoWired(Project project) {
        CrapTask task = crapTask(project);
        JacocoReport jacocoReport = project.getTasks().named("jacocoTestReport", JacocoReport.class).get();
        File xml = jacocoReport.getReports().getXml().getOutputLocation().get().getAsFile();

        assertThat(jacocoReport.getReports().getXml().getRequired().get()).isTrue();
        assertThat(task.getReports().getFiles()).containsExactly(xml);
        assertThat(task.getTaskDependencies().getDependencies(task)).extracting(Task::getName)
                .contains("jacocoTestReport", "test");
    }

    private Project project() {
        return ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build();
    }

    private static CrapTask crapTask(Project project) {
        return project.getTasks().named(Crap4JavaPlugin.TASK_NAME, CrapTask.class).get();
    }
}
