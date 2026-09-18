package crappyjava.core;

public class JacocoParseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public JacocoParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public JacocoParseException(String message) {
        super(message);
    }
}
