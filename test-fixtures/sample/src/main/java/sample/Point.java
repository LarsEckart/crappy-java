package sample;

public record Point(int x, int y) {

    /** Compact constructor with logic. */
    public Point {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("negative coordinate");
        }
    }

    public int quadrant() {
        if (x == 0 || y == 0) {
            return 0;
        }
        return x > 0 ? (y > 0 ? 1 : 4) : (y > 0 ? 2 : 3);
    }
}
