package sample;

public enum Shape {
    CIRCLE, SQUARE, TRIANGLE;

    /** Switch on enum; only CIRCLE is exercised by tests. */
    public int sides() {
        return switch (this) {
            case CIRCLE -> 0;
            case SQUARE -> 4;
            case TRIANGLE -> 3;
        };
    }
}
