package crap4java.core;

/** The CRAP formula: {@code CC^2 * (1 - cov)^3 + CC}. */
public final class Crap {

    private Crap() {
    }

    /**
     * @param complexity cyclomatic complexity, 0 or more (JaCoCo reports at least 1 for real methods)
     * @param coverage   covered fraction in 0.0..1.0
     */
    public static double score(int complexity, double coverage) {
        if (complexity < 0) {
            throw new IllegalArgumentException("complexity must not be negative: " + complexity);
        }
        if (coverage < 0.0 || coverage > 1.0) {
            throw new IllegalArgumentException("coverage must be within 0..1: " + coverage);
        }
        double uncovered = 1.0 - coverage;
        return (double) complexity * complexity * uncovered * uncovered * uncovered + complexity;
    }
}
