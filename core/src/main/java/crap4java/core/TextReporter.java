package crap4java.core;

import java.util.List;
import java.util.Locale;

/**
 * Plain-text report meant to be read by people and by AI agents: deterministic, ASCII apart from
 * identifiers, one method per line, fully qualified method identity, single summary line.
 * Columns stay aligned for CRAP below 100,000,000 and CC below 100,000.
 */
public final class TextReporter {

    private static final String ROW = "%9.1f %5d %5.1f   %s  %s%n";

    private TextReporter() {
    }

    public static String render(Analysis analysis, List<String> sources, ReportOptions options) {
        StringBuilder out = new StringBuilder();
        out.append(String.format(Locale.ROOT, "CRAP report  threshold=%.1f  source=%s%n",
                analysis.threshold(), String.join(",", sources)));
        out.append(System.lineSeparator());

        List<MethodCrap> rows = select(analysis, options);
        if (!rows.isEmpty()) {
            out.append(String.format(Locale.ROOT, "%9s %5s %5s   %s%n", "CRAP", "CC", "COV%", "METHOD"));
            for (MethodCrap row : rows) {
                out.append(String.format(Locale.ROOT, ROW,
                        row.crap(), row.complexity(), row.coverage() * 100.0, row.signature(), row.location()));
            }
            out.append(System.lineSeparator());
        }

        out.append(String.format(Locale.ROOT,
                "methods=%d crappy=%d (%.1f%%) max=%.1f threshold=%.1f verdict=%s%n",
                analysis.methods().size(),
                analysis.crappy().size(),
                analysis.crappyPercentage(),
                analysis.maxCrap(),
                analysis.threshold(),
                analysis.thresholdExceeded() ? "FAIL" : "OK"));
        return out.toString();
    }

    private static List<MethodCrap> select(Analysis analysis, ReportOptions options) {
        if (options.showAll()) {
            return analysis.methods();
        }
        if (options.top() > 0) {
            List<MethodCrap> all = analysis.methods();
            return all.subList(0, Math.min(options.top(), all.size()));
        }
        return analysis.crappy();
    }
}
