package sample;

public class Outer {

    private final int base;

    public Outer(int base) {
        this.base = base;
    }

    public int outerMethod(int x) {
        return x > base ? x - base : base - x;
    }

    /** Reported by JaCoCo as sample/Outer$Nested. */
    public static class Nested {
        public int nested(int x) {
            return x % 2 == 0 ? x / 2 : 3 * x + 1;
        }
    }

    /** Reported by JaCoCo as sample/Outer$Inner. */
    public class Inner {
        public int inner(int x) {
            return x > 0 && base > 0 ? x * base : 0;
        }
    }

    /** Anonymous class, reported as sample/Outer$1. */
    public Runnable anonymous(int x) {
        return new Runnable() {
            @Override
            public void run() {
                if (x > base) {
                    System.out.println("above");
                } else {
                    System.out.println("below");
                }
            }
        };
    }
}
