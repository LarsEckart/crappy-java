package crap4java.core;

import java.util.ArrayList;
import java.util.List;

/** All methods of a report scored and sorted worst first, plus the threshold they are judged against. */
public record Analysis(List<MethodCrap> methods, double threshold) {

    public Analysis {
        methods = List.copyOf(methods);
    }

    public static Analysis of(JacocoReport report, double threshold) {
        List<MethodCrap> scored = new ArrayList<>();
        for (MethodCoverage method : LambdaFolder.fold(report.methods())) {
            scored.add(MethodCrap.of(method));
        }
        scored.sort(MethodCrap.WORST_FIRST);
        return new Analysis(scored, threshold);
    }

    /** Methods whose CRAP is strictly above the threshold, worst first. */
    public List<MethodCrap> crappy() {
        List<MethodCrap> result = new ArrayList<>();
        for (MethodCrap method : methods) {
            if (method.crap() > threshold) {
                result.add(method);
            }
        }
        return result;
    }

    public double maxCrap() {
        return methods.isEmpty() ? 0.0 : methods.get(0).crap();
    }

    public boolean thresholdExceeded() {
        return maxCrap() > threshold;
    }

    /** Percentage of crappy methods, 0.0 when there are no methods. */
    public double crappyPercentage() {
        return methods.isEmpty() ? 0.0 : 100.0 * crappy().size() / methods.size();
    }
}
