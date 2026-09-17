package crap4java.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Parsed JaCoCo report, flattened to its methods. */
public record JacocoReport(String name, List<MethodCoverage> methods) {

    public JacocoReport {
        methods = List.copyOf(methods);
    }

    /**
     * Concatenates reports of disjoint class sets, e.g. one report per module.
     *
     * @throws JacocoParseException when a class appears in more than one report. Coverage of the same
     *                              class from two runs cannot be combined from XML; merge JaCoCo exec
     *                              files instead and pass a single report.
     */
    public static JacocoReport merge(List<JacocoReport> reports) {
        List<MethodCoverage> all = new ArrayList<>();
        List<String> names = new ArrayList<>();
        Map<String, Integer> classOrigin = new HashMap<>();
        for (int i = 0; i < reports.size(); i++) {
            JacocoReport report = reports.get(i);
            for (MethodCoverage method : report.methods()) {
                Integer previous = classOrigin.putIfAbsent(method.className(), i);
                if (previous != null && previous != i) {
                    throw new JacocoParseException("class " + method.className() + " appears in both "
                            + reports.get(previous).name() + " and " + report.name()
                            + ". Coverage of one class from several reports cannot be"
                            + " combined from XML; merge the JaCoCo exec files (jacoco-report-aggregation, jacoco:merge)"
                            + " and pass a single report.");
                }
            }
            all.addAll(report.methods());
            names.add(report.name());
        }
        return new JacocoReport(String.join(",", names), all);
    }
}
