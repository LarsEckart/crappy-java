package crap4java.gradle;

import crap4java.core.Analysis;
import crap4java.core.JacocoParseException;
import crap4java.core.JacocoReport;
import crap4java.core.JacocoXmlParser;
import crap4java.core.ReportOptions;
import crap4java.core.TextReporter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;
import org.gradle.work.DisableCachingByDefault;

/**
 * Computes CRAP scores from JaCoCo XML reports, prints the report and optionally fails the build.
 * A configured report file that does not exist fails the task rather than silently passing the gate.
 */
@DisableCachingByDefault(because = "cheap to compute; output embeds project-relative report paths")
public abstract class CrapTask extends DefaultTask {

    /**
     * JaCoCo XML report files. Wired to {@code jacocoTestReport} when the jacoco plugin is applied.
     * Absolute path sensitivity is intended: the report text names these files relative to the project.
     */
    @InputFiles
    public abstract ConfigurableFileCollection getReports();

    @Input
    public abstract Property<Double> getThreshold();

    @Input
    public abstract Property<Boolean> getFailOnViolation();

    @Input
    public abstract Property<Boolean> getShowAll();

    @Input
    public abstract Property<Integer> getTop();

    /** Where the text report is written, in addition to the console. */
    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @Inject
    protected abstract ProjectLayout getLayout();

    @TaskAction
    public void run() {
        List<Path> paths = new ArrayList<>();
        List<String> names = new ArrayList<>();
        Path projectDir = getLayout().getProjectDirectory().getAsFile().toPath();
        for (File file : getReports().getFiles()) {
            if (!file.isFile()) {
                throw new GradleException("JaCoCo XML report not found: " + file
                        + ". Run the jacocoTestReport task with XML output enabled, or point crap.reports at an existing file.");
            }
            paths.add(file.toPath());
            names.add(relativize(projectDir, file.toPath()));
        }

        JacocoReport report;
        try {
            report = JacocoXmlParser.parseAll(paths);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read JaCoCo XML report", e);
        } catch (JacocoParseException e) {
            throw new GradleException(e.getMessage(), e);
        }

        if (report.methods().isEmpty()) {
            getLogger().warn("crap4java: no methods found in {}; is this the right report?", String.join(",", names));
        }
        double threshold = getThreshold().get();
        Analysis analysis = Analysis.of(report, threshold);
        String text = TextReporter.render(analysis, names, new ReportOptions(getShowAll().get(), getTop().get()));

        Path output = getOutputFile().get().getAsFile().toPath();
        try {
            Files.createDirectories(output.getParent());
            Files.writeString(output, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write " + output, e);
        }

        getLogger().lifecycle(text.stripTrailing());

        if (analysis.thresholdExceeded() && getFailOnViolation().get()) {
            throw new VerificationException(String.format(Locale.ROOT,
                    "CRAP threshold exceeded: max=%.1f > threshold=%.1f (%d crappy method(s)). Report: %s",
                    analysis.maxCrap(), threshold, analysis.crappy().size(), relativize(projectDir, output)));
        }
    }

    private static String relativize(Path base, Path path) {
        try {
            return base.relativize(path).toString().replace(File.separatorChar, '/');
        } catch (IllegalArgumentException differentRoots) {
            return path.toString();
        }
    }
}
