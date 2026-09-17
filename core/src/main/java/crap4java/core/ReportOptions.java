package crap4java.core;

/**
 * What the text report lists.
 *
 * @param showAll list every method instead of only those above the threshold
 * @param top     when positive and {@code showAll} is false, list the {@code top} worst methods regardless of threshold
 */
public record ReportOptions(boolean showAll, int top) {

    public static final ReportOptions DEFAULT = new ReportOptions(false, 0);

    public ReportOptions {
        if (top < 0) {
            throw new IllegalArgumentException("top must not be negative: " + top);
        }
    }
}
