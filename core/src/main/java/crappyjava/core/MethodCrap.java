package crappyjava.core;

import java.util.Comparator;

/** CRAP result for one method. */
public record MethodCrap(
        String className,
        String methodName,
        String desc,
        String sourceFile,
        int line,
        int complexity,
        double coverage,
        double crap) {

    /** Worst first, then a stable location-based order. */
    public static final Comparator<MethodCrap> WORST_FIRST = Comparator
            .comparingDouble(MethodCrap::crap).reversed()
            .thenComparing(MethodCrap::className)
            .thenComparingInt(MethodCrap::line)
            .thenComparing(MethodCrap::methodName)
            .thenComparing(MethodCrap::desc);

    public static MethodCrap of(MethodCoverage method) {
        Counter complexity = method.complexity();
        int cc = complexity.total();
        double coverage = complexity.coveredRatio();
        return new MethodCrap(
                method.qualifiedClassName(),
                method.name(),
                method.desc(),
                method.sourceFile(),
                method.line(),
                cc,
                coverage,
                Crap.score(cc, coverage));
    }

    /** {@code sample.Outer$Inner.inner(I)I} */
    public String signature() {
        return className + "." + methodName + desc;
    }

    /** {@code Outer.java:25}, or {@code ?} when the report has no source information. */
    public String location() {
        if (sourceFile == null) {
            return "?";
        }
        return line > 0 ? sourceFile + ":" + line : sourceFile;
    }
}
