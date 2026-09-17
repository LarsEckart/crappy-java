package crap4java.core;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * One {@code <method>} element from a JaCoCo report.
 *
 * @param className  JaCoCo internal class name, e.g. {@code sample/Outer$Inner}
 * @param sourceFile file name from the enclosing class element, may be null
 * @param name       method name; {@code <init>}, {@code <clinit>} and {@code lambda$foo$0} appear verbatim
 * @param desc       JVM method descriptor
 * @param line       first line of the method, 0 when unknown
 * @param counters   counters by type
 */
public record MethodCoverage(
        String className,
        String sourceFile,
        String name,
        String desc,
        int line,
        Map<CounterType, Counter> counters) {

    private static final String LAMBDA_PREFIX = "lambda$";

    public MethodCoverage {
        Objects.requireNonNull(className, "className");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(desc, "desc");
        Objects.requireNonNull(counters, "counters");
        counters = Map.copyOf(counters);
    }

    public Counter counter(CounterType type) {
        return counters.getOrDefault(type, Counter.ZERO);
    }

    public Counter complexity() {
        return counter(CounterType.COMPLEXITY);
    }

    /** Class name with dots, e.g. {@code sample.Outer$Inner}. */
    public String qualifiedClassName() {
        return className.replace('/', '.');
    }

    public boolean isLambda() {
        return name.startsWith(LAMBDA_PREFIX);
    }

    /** This method with {@code other}'s counters added. Identity fields stay as in {@code this}. */
    public MethodCoverage plus(MethodCoverage other) {
        Map<CounterType, Counter> sum = new EnumMap<>(CounterType.class);
        sum.putAll(counters);
        other.counters.forEach((type, counter) -> sum.merge(type, counter, Counter::plus));
        return new MethodCoverage(className, sourceFile, name, desc, line, sum);
    }
}
