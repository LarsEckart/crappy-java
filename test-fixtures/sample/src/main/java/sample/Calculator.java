package sample;

public class Calculator {

    /** CC 4, every branch exercised by tests. */
    public int covered(int x) {
        if (x < 0) {
            return -1;
        }
        if (x == 0) {
            return 0;
        }
        if (x > 100) {
            return 100;
        }
        return x;
    }

    /** CC 4, tests only exercise the happy path. */
    public int partial(int x) {
        if (x < 0) {
            return -1;
        }
        if (x == 0) {
            return 0;
        }
        if (x > 100) {
            return 100;
        }
        return x;
    }

    /** Overload 1: tested. */
    public int overloaded(int x) {
        return x > 0 ? x : 0;
    }

    /** Overload 2: not tested. Same name, different descriptor. */
    public int overloaded(String s) {
        return s == null ? 0 : s.length();
    }
}
