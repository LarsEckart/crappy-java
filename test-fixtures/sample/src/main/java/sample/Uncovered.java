package sample;

/** No test touches this class. Expected to be the crappiest thing in the report. */
public class Uncovered {

    public String classify(int a, int b, String mode) {
        String result;
        switch (mode) {
            case "sum" -> result = a + b > 10 ? "big" : "small";
            case "diff" -> result = a - b < 0 ? "negative" : "non-negative";
            case "product" -> result = a * b == 0 ? "zero" : "non-zero";
            default -> result = "unknown";
        }
        if (a > 100 && b > 100) {
            result = result + "!";
        } else if (a < 0 || b < 0) {
            result = result + "?";
        }
        for (int i = 0; i < a; i++) {
            if (i % 7 == 0) {
                result = result + ".";
            }
        }
        return result;
    }

    public int trivial() {
        return 42;
    }
}
