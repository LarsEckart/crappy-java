package crappyjava.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/** The checked-in JaCoCo report generated from test-fixtures/sample. */
final class Fixture {

    static final String NAME = "sample-jacoco.xml";

    private Fixture() {
    }

    static JacocoReport report() {
        try (InputStream in = Fixture.class.getResourceAsStream("/" + NAME)) {
            if (in == null) {
                throw new IllegalStateException("missing test resource " + NAME);
            }
            return JacocoXmlParser.parse(in, NAME);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static MethodCoverage method(JacocoReport report, String className, String name, String desc) {
        return report.methods().stream()
                .filter(m -> m.className().equals(className) && m.name().equals(name) && m.desc().equals(desc))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no method " + className + "." + name + desc));
    }
}
