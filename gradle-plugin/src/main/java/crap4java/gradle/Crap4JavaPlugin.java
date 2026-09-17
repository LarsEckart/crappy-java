package crap4java.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.testing.jacoco.tasks.JacocoReport;

/**
 * Registers the {@code crap} task and the {@code crap4java} extension. When the {@code jacoco} plugin
 * is applied, {@code jacocoTestReport} is told to emit XML and {@code crap} consumes it.
 */
public class Crap4JavaPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "crap4java";
    public static final String TASK_NAME = "crap";
    static final String JACOCO_REPORT_TASK = "jacocoTestReport";

    @Override
    public void apply(Project project) {
        Crap4JavaExtension extension = project.getExtensions().create(EXTENSION_NAME, Crap4JavaExtension.class);

        TaskProvider<CrapTask> crap = project.getTasks().register(TASK_NAME, CrapTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Reports CRAP scores computed from JaCoCo XML and fails above the threshold.");
            task.getThreshold().convention(extension.getThreshold());
            task.getFailOnViolation().convention(extension.getFailOnViolation());
            task.getShowAll().convention(extension.getShowAll());
            task.getTop().convention(extension.getTop());
            task.getOutputFile().convention(project.getLayout().getBuildDirectory().file("reports/crap4java/crap.txt"));
            // Skip only when nothing was configured at all. A configured-but-missing file must fail loudly.
            task.onlyIf("no JaCoCo XML reports configured", t -> !((CrapTask) t).getReports().getFrom().isEmpty());
        });

        project.getPluginManager().withPlugin("java", java ->
                project.getPluginManager().withPlugin("jacoco", jacoco -> wireJacoco(project, crap)));
    }

    private static void wireJacoco(Project project, TaskProvider<CrapTask> crap) {
        TaskProvider<JacocoReport> jacocoReport = project.getTasks().named(JACOCO_REPORT_TASK, JacocoReport.class);
        jacocoReport.configure(report -> report.getReports().getXml().getRequired().set(true));
        crap.configure(task -> {
            // Report.outputLocation has no owning task in Gradle's model, so map from the task provider
            // (which carries the dependency) rather than flatMap-ing the property itself.
            task.getReports().from(jacocoReport.map(report -> report.getReports().getXml().getOutputLocation().get().getAsFile()));
            task.dependsOn(jacocoReport);
            // jacocoTestReport only runs after test, it does not trigger it
            task.dependsOn(project.getTasks().named("test"));
        });
    }
}
