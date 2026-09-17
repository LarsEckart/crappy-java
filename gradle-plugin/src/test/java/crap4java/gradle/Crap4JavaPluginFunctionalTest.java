package crap4java.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Crap4JavaPluginFunctionalTest {

    /** Set by the Gradle test task; falls back to the path relative to the module for IDE runs. */
    private static final Path FIXTURE = Path.of(System.getProperty("crap4java.fixture", "../core/src/test/resources/sample-jacoco.xml"));

    @TempDir
    Path projectDir;

    @BeforeEach
    void settings() throws IOException {
        write("settings.gradle", "rootProject.name = 'consumer'\n");
    }

    @Test
    void fails_the_build_when_a_method_exceeds_the_threshold() throws IOException {
        Files.copy(FIXTURE, projectDir.resolve("sample-jacoco.xml"));
        write("build.gradle", """
                plugins { id 'crap4java' }
                tasks.named('crap') { reports.from('sample-jacoco.xml') }
                """);

        BuildResult result = runner("crap").buildAndFail();

        assertThat(result.task(":crap").getOutcome()).isEqualTo(TaskOutcome.FAILED);
        assertThat(result.getOutput())
                .contains("CRAP report  threshold=30.0  source=sample-jacoco.xml")
                .contains("182.0    13   0.0   sample.Uncovered.classify(IILjava/lang/String;)Ljava/lang/String;  Uncovered.java:8")
                .contains("methods=30 crappy=2 (6.7%) max=182.0 threshold=30.0 verdict=FAIL")
                .contains("CRAP threshold exceeded: max=182.0 > threshold=30.0 (2 crappy method(s)). Report: build/reports/crap4java/crap.txt");
        assertThat(projectDir.resolve("build/reports/crap4java/crap.txt")).content(StandardCharsets.UTF_8)
                .endsWith("verdict=FAIL" + System.lineSeparator());
    }

    @Test
    void extension_configures_threshold_and_fail_on_violation() throws IOException {
        Files.copy(FIXTURE, projectDir.resolve("sample-jacoco.xml"));
        write("build.gradle", """
                plugins { id 'crap4java' }
                crap4java {
                    threshold = 10
                    failOnViolation = false
                    top = 1
                }
                tasks.named('crap') { reports.from('sample-jacoco.xml') }
                """);

        BuildResult result = runner("crap").build();

        assertThat(result.task(":crap").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(result.getOutput())
                .contains("sample.Uncovered.classify")
                .doesNotContain("sample.Pipelines.upper")
                .contains("methods=30 crappy=6 (20.0%) max=182.0 threshold=10.0 verdict=FAIL");
    }

    @Test
    void passes_and_is_configuration_cache_compatible() throws IOException {
        Files.copy(FIXTURE, projectDir.resolve("sample-jacoco.xml"));
        write("build.gradle", """
                plugins { id 'crap4java' }
                crap4java {
                    threshold = 1000
                    showAll = true
                }
                tasks.named('crap') { reports.from('sample-jacoco.xml') }
                """);

        BuildResult first = runner("crap", "--configuration-cache").build();
        BuildResult second = runner("crap", "--configuration-cache").build();

        assertThat(first.getOutput()).contains("verdict=OK").contains("sample.Calculator.<init>()V");
        assertThat(second.getOutput()).contains("Reusing configuration cache");
        assertThat(second.task(":crap").getOutcome()).isEqualTo(TaskOutcome.UP_TO_DATE);
    }

    @Test
    void without_configured_reports_the_task_is_skipped() throws IOException {
        write("build.gradle", "plugins { id 'crap4java' }\n");

        BuildResult result = runner("crap").build();

        assertThat(result.task(":crap").getOutcome()).isEqualTo(TaskOutcome.SKIPPED);
    }

    @Test
    void configured_but_missing_report_fails_instead_of_passing_the_gate() throws IOException {
        write("build.gradle", """
                plugins { id 'crap4java' }
                tasks.named('crap') { reports.from('typo.xml') }
                """);

        BuildResult result = runner("crap").buildAndFail();

        assertThat(result.task(":crap").getOutcome()).isEqualTo(TaskOutcome.FAILED);
        assertThat(result.getOutput()).contains("JaCoCo XML report not found").contains("typo.xml");
    }

    @Test
    void jacoco_project_without_tests_fails_instead_of_passing_the_gate() throws IOException {
        write("build.gradle", """
                plugins {
                    id 'java'
                    id 'jacoco'
                    id 'crap4java'
                }
                """);
        write("src/main/java/demo/Untested.java", """
                package demo;
                public class Untested { public int f(int x) { return x > 0 ? x : -x; } }
                """);

        BuildResult result = runner("crap").buildAndFail();

        assertThat(result.task(":jacocoTestReport").getOutcome()).isEqualTo(TaskOutcome.SKIPPED);
        assertThat(result.task(":crap").getOutcome()).isEqualTo(TaskOutcome.FAILED);
        assertThat(result.getOutput()).contains("JaCoCo XML report not found");
    }

    @Test
    void kotlin_dsl_and_applying_crap4java_before_jacoco() throws IOException {
        Files.delete(projectDir.resolve("settings.gradle"));
        write("settings.gradle.kts", "rootProject.name = \"consumer\"\n");
        Files.copy(FIXTURE, projectDir.resolve("sample-jacoco.xml"));
        write("build.gradle.kts", """
                plugins {
                    id("crap4java")
                    `java-library`
                    jacoco
                }
                crap4java {
                    threshold = 7.5
                    failOnViolation = false
                }
                tasks.named<crap4java.gradle.CrapTask>("crap") {
                    reports.setFrom("sample-jacoco.xml")
                }
                """);

        BuildResult result = runner("crap").build();

        assertThat(result.task(":crap").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(result.getOutput()).contains("threshold=7.5 verdict=FAIL");
    }

    @Test
    void wires_into_jacoco_test_report_end_to_end() throws IOException {
        write("build.gradle", """
                plugins {
                    id 'java'
                    id 'jacoco'
                    id 'crap4java'
                }
                repositories { mavenCentral() }
                dependencies {
                    testImplementation platform('org.junit:junit-bom:6.1.3')
                    testImplementation 'org.junit.jupiter:junit-jupiter'
                    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
                }
                test { useJUnitPlatform() }
                crap4java { threshold = 7.5 }
                """);
        write("src/main/java/demo/Grader.java", """
                package demo;
                public class Grader {
                    public String grade(int score) {
                        if (score >= 90) return "A";
                        if (score >= 80) return "B";
                        if (score >= 70) return "C";
                        if (score >= 60) return "D";
                        return "F";
                    }
                    public int tested(int x) { return x > 0 ? x : -x; }
                }
                """);
        write("src/test/java/demo/GraderTest.java", """
                package demo;
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.assertEquals;
                class GraderTest {
                    @Test void abs() { assertEquals(3, new Grader().tested(-3)); assertEquals(3, new Grader().tested(3)); }
                }
                """);

        BuildResult result = runner("crap").buildAndFail();

        assertThat(result.task(":test").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(result.task(":jacocoTestReport").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(result.task(":crap").getOutcome()).isEqualTo(TaskOutcome.FAILED);
        assertThat(result.getOutput())
                .contains("source=build/reports/jacoco/test/jacocoTestReport.xml")
                .contains("30.0     5   0.0   demo.Grader.grade(I)Ljava/lang/String;  Grader.java:4")
                .contains("threshold=7.5 verdict=FAIL")
                .doesNotContain("demo.Grader.tested")
                .doesNotContain("Grader.tested");
    }

    private GradleRunner runner(String... arguments) {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments(arguments)
                .forwardOutput();
    }

    private void write(String relativePath, String content) throws IOException {
        Path file = projectDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
