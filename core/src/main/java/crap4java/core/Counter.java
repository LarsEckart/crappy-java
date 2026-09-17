package crap4java.core;

/** A JaCoCo counter: how many items of a kind were missed and covered. */
public record Counter(int missed, int covered) {

    public static final Counter ZERO = new Counter(0, 0);

    public Counter {
        if (missed < 0 || covered < 0) {
            throw new IllegalArgumentException("counter values must not be negative: " + missed + "/" + covered);
        }
    }

    public int total() {
        return missed + covered;
    }

    /** Fraction covered in 0.0..1.0; 0.0 when the counter is empty. */
    public double coveredRatio() {
        int total = total();
        return total == 0 ? 0.0 : (double) covered / total;
    }

    public Counter plus(Counter other) {
        return new Counter(missed + other.missed, covered + other.covered);
    }
}
