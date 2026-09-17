package crap4java.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Streaming parser for JaCoCo XML reports. Reads {@code <class>}, {@code <method>} and the
 * method-level {@code <counter>} elements; everything else is skipped. The DOCTYPE is not resolved.
 */
public final class JacocoXmlParser {

    private JacocoXmlParser() {
    }

    public static JacocoReport parse(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new NoSuchFileException(path.toString());
        }
        if (!Files.isRegularFile(path)) {
            throw new IOException("not a regular file: " + path);
        }
        try (InputStream in = Files.newInputStream(path)) {
            return parse(in, path.toString());
        }
    }

    public static JacocoReport parseAll(List<Path> paths) throws IOException {
        List<JacocoReport> reports = new ArrayList<>();
        for (Path path : paths) {
            reports.add(parse(path));
        }
        return JacocoReport.merge(reports);
    }

    public static JacocoReport parse(InputStream in, String sourceName) {
        XMLStreamReader reader = null;
        try {
            reader = newFactory().createXMLStreamReader(in);
            return read(reader, sourceName);
        } catch (XMLStreamException e) {
            throw new JacocoParseException("Unable to parse JaCoCo XML: " + sourceName, e);
        } finally {
            closeQuietly(reader);
        }
    }

    private static XMLInputFactory newFactory() {
        // newDefaultFactory pins the JDK implementation; newInstance could pick up Woodstox/Aalto from a
        // Gradle daemon classpath, whose DTD handling differs from what these settings were tested with.
        XMLInputFactory factory = XMLInputFactory.newDefaultFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return factory;
    }

    private static JacocoReport read(XMLStreamReader reader, String sourceName) throws XMLStreamException {
        List<MethodCoverage> methods = new ArrayList<>();
        String reportName = sourceName;
        String className = null;
        String sourceFile = null;
        String methodName = null;
        String methodDesc = null;
        int methodLine = 0;
        Map<CounterType, Counter> counters = null;
        boolean sawReport = false;

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                switch (reader.getLocalName()) {
                    case "report" -> {
                        sawReport = true;
                        String name = reader.getAttributeValue(null, "name");
                        if (name != null && !name.isBlank()) {
                            reportName = name;
                        }
                    }
                    case "class" -> {
                        className = required(reader, "name", sourceName);
                        sourceFile = reader.getAttributeValue(null, "sourcefilename");
                    }
                    case "method" -> {
                        methodName = required(reader, "name", sourceName);
                        methodDesc = required(reader, "desc", sourceName);
                        methodLine = optionalInt(reader.getAttributeValue(null, "line"));
                        counters = new EnumMap<>(CounterType.class);
                    }
                    case "counter" -> {
                        if (counters != null) {
                            CounterType type = counterType(reader.getAttributeValue(null, "type"));
                            if (type != null) {
                                counters.put(type, new Counter(
                                        requiredInt(reader, "missed", sourceName),
                                        requiredInt(reader, "covered", sourceName)));
                            }
                        }
                    }
                    default -> {
                        // ignore
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                switch (reader.getLocalName()) {
                    case "method" -> {
                        if (className == null) {
                            throw new JacocoParseException("<method> outside <class> in " + sourceName);
                        }
                        methods.add(new MethodCoverage(className, sourceFile, methodName, methodDesc, methodLine, counters));
                        counters = null;
                    }
                    case "class" -> {
                        className = null;
                        sourceFile = null;
                    }
                    default -> {
                        // ignore
                    }
                }
            }
        }
        if (!sawReport) {
            throw new JacocoParseException("Not a JaCoCo report (no <report> element): " + sourceName);
        }
        return new JacocoReport(reportName, methods);
    }

    private static String required(XMLStreamReader reader, String attribute, String sourceName) {
        String value = reader.getAttributeValue(null, attribute);
        if (value == null) {
            throw new JacocoParseException("<" + reader.getLocalName() + "> without " + attribute
                    + " attribute at line " + reader.getLocation().getLineNumber() + " in " + sourceName);
        }
        return value;
    }

    private static CounterType counterType(String value) {
        if (value == null) {
            return null;
        }
        try {
            return CounterType.valueOf(value);
        } catch (IllegalArgumentException unknownType) {
            return null;
        }
    }

    /** For attributes that may legitimately be absent, such as {@code line}. */
    private static int optionalInt(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** For counter values: absent, non-numeric or negative values are report corruption, not data. */
    private static int requiredInt(XMLStreamReader reader, String attribute, String sourceName) {
        String value = required(reader, attribute, sourceName);
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                throw new JacocoParseException("negative " + attribute + "=\"" + value + "\" at line "
                        + reader.getLocation().getLineNumber() + " in " + sourceName);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new JacocoParseException("non-numeric " + attribute + "=\"" + value + "\" at line "
                    + reader.getLocation().getLineNumber() + " in " + sourceName);
        }
    }

    private static void closeQuietly(XMLStreamReader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (XMLStreamException ignored) {
                // nothing useful to do
            }
        }
    }
}
