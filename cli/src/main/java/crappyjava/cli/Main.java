package crappyjava.cli;

import crappyjava.core.Analysis;
import crappyjava.core.JacocoParseException;
import crappyjava.core.JacocoReport;
import crappyjava.core.JacocoXmlParser;
import crappyjava.core.ReportOptions;
import crappyjava.core.TextReporter;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

/** Command line entry point: {@code crappy-java [options] <jacoco.xml>...}. */
public final class Main {

    static final int EXIT_OK = 0;
    static final int EXIT_USAGE = 1;
    static final int EXIT_THRESHOLD = 2;

    static final String USAGE = """
            usage: crappy-java [--threshold <n>] [--all | --top <n>] <jacoco.xml>...
                   crappy-java --help

            Computes the CRAP score  CC^2 * (1 - coverage)^3 + CC  for every method in the given
            JaCoCo XML report(s). Complexity and coverage both come from JaCoCo's COMPLEXITY counter.

            options:
              --threshold <n>   CRAP above which a method is crappy and the run fails (default 30)
              --all             list every method, not only crappy ones
              --top <n>         list the n worst methods regardless of threshold
              -h, --help        print this help

            exit codes:
              0  no method above the threshold
              1  usage error, unreadable or invalid report
              2  at least one method above the threshold
            """;

    private Main() {
    }

    public static void main(String[] args) {
        // Identifiers may be non-ASCII; do not depend on the platform default encoding.
        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8);
        System.exit(run(args, out, err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        CliArguments arguments;
        try {
            arguments = CliArguments.parse(args);
        } catch (UsageException e) {
            err.println("crappy-java: " + e.getMessage());
            err.print(USAGE);
            return EXIT_USAGE;
        }
        if (arguments.help()) {
            out.print(USAGE);
            return EXIT_OK;
        }

        JacocoReport report;
        try {
            report = JacocoXmlParser.parseAll(arguments.reports());
        } catch (NoSuchFileException e) {
            err.println("crappy-java: cannot read report: no such file: " + e.getMessage());
            return EXIT_USAGE;
        } catch (IOException e) {
            err.println("crappy-java: cannot read report: " + e.getMessage());
            return EXIT_USAGE;
        } catch (JacocoParseException e) {
            err.println("crappy-java: " + e.getMessage());
            return EXIT_USAGE;
        }

        List<String> sources = arguments.reports().stream().map(Path::toString).toList();
        if (report.methods().isEmpty()) {
            err.println("crappy-java: warning: no methods found in " + String.join(",", sources)
                    + "; is this the right report?");
        }
        Analysis analysis = Analysis.of(report, arguments.threshold());
        out.print(TextReporter.render(analysis, sources, new ReportOptions(arguments.showAll(), arguments.top())));
        return analysis.thresholdExceeded() ? EXIT_THRESHOLD : EXIT_OK;
    }
}
