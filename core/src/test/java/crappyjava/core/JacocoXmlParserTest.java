package crappyjava.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JacocoXmlParserTest {

    private final JacocoReport report = Fixture.report();

    @Test
    void reads_report_name_and_every_method() {
        assertThat(report.name()).isEqualTo("sample");
        assertThat(report.methods()).hasSize(35);
    }

    @Test
    void reads_method_identity_and_counters() {
        MethodCoverage classify = Fixture.method(report, "sample/Uncovered", "classify", "(IILjava/lang/String;)Ljava/lang/String;");

        assertThat(classify.sourceFile()).isEqualTo("Uncovered.java");
        assertThat(classify.line()).isEqualTo(8);
        assertThat(classify.complexity()).isEqualTo(new Counter(13, 0));
        assertThat(classify.counter(CounterType.BRANCH)).isEqualTo(new Counter(22, 0));
        assertThat(classify.counter(CounterType.INSTRUCTION)).isEqualTo(new Counter(71, 0));
        assertThat(classify.qualifiedClassName()).isEqualTo("sample.Uncovered");
    }

    @Test
    void keeps_overloads_apart_by_descriptor() {
        MethodCoverage byInt = Fixture.method(report, "sample/Calculator", "overloaded", "(I)I");
        MethodCoverage byString = Fixture.method(report, "sample/Calculator", "overloaded", "(Ljava/lang/String;)I");

        assertThat(byInt.complexity()).isEqualTo(new Counter(0, 2));
        assertThat(byString.complexity()).isEqualTo(new Counter(2, 0));
    }

    @Test
    void reads_nested_inner_and_anonymous_classes() {
        assertThat(report.methods()).extracting(MethodCoverage::className)
                .contains("sample/Outer$Inner", "sample/Outer$Nested", "sample/Outer$1");
    }

    @Test
    void reads_lambda_constructor_and_static_initializer_methods() {
        assertThat(report.methods())
                .filteredOn(m -> m.className().equals("sample/Pipelines"))
                .extracting(MethodCoverage::name)
                .containsExactlyInAnyOrder("<init>", "<clinit>", "countLong", "upper",
                        "lambda$new$0", "lambda$static$0", "lambda$countLong$0", "lambda$upper$0", "lambda$upper$1");
    }

    @Test
    void method_without_branch_counter_reports_zero_counter() {
        MethodCoverage trivial = Fixture.method(report, "sample/Uncovered", "trivial", "()I");

        assertThat(trivial.counter(CounterType.BRANCH)).isEqualTo(Counter.ZERO);
        assertThat(trivial.complexity()).isEqualTo(new Counter(1, 0));
    }

    @Test
    void ignores_class_and_package_level_counters() {
        // Package/class counters follow the methods inside <class>; they must not leak into the last method.
        MethodCoverage last = report.methods().get(report.methods().size() - 1);
        assertThat(last.counter(CounterType.CLASS)).isEqualTo(Counter.ZERO);
    }

    @Test
    void parses_minimal_report_with_doctype_without_resolving_it() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <!DOCTYPE report PUBLIC "-//JACOCO//DTD Report 1.1//EN" "report.dtd">
                <report name="mini">
                  <package name="p">
                    <class name="p/A" sourcefilename="A.java">
                      <method name="m" desc="()V" line="3">
                        <counter type="COMPLEXITY" missed="1" covered="2"/>
                      </method>
                      <counter type="COMPLEXITY" missed="1" covered="2"/>
                    </class>
                  </package>
                </report>
                """;

        JacocoReport parsed = parse(xml);

        assertThat(parsed.name()).isEqualTo("mini");
        assertThat(parsed.methods()).containsExactly(
                new MethodCoverage("p/A", "A.java", "m", "()V", 3, Map.of(CounterType.COMPLEXITY, new Counter(1, 2))));
    }

    @Test
    void rejects_xml_that_is_not_a_jacoco_report() {
        assertThatThrownBy(() -> parse("<foo/>"))
                .isInstanceOf(JacocoParseException.class)
                .hasMessageContaining("no <report> element");
    }

    @Test
    void rejects_malformed_xml() {
        assertThatThrownBy(() -> parse("<report name='x'><class name='p/A'>"))
                .isInstanceOf(JacocoParseException.class)
                .hasMessageContaining("Unable to parse");
    }

    @Test
    void merges_reports_with_disjoint_classes() {
        JacocoReport other = parse("""
                <report name="other"><package name="q"><class name="q/B" sourcefilename="B.java">
                <method name="m" desc="()V" line="1"><counter type="COMPLEXITY" missed="1" covered="0"/></method>
                </class></package></report>
                """);

        JacocoReport merged = JacocoReport.merge(List.of(report, other));

        assertThat(merged.name()).isEqualTo("sample,other");
        assertThat(merged.methods()).hasSize(36);
    }

    @Test
    void refuses_to_merge_reports_sharing_a_class() {
        JacocoReport again = new JacocoReport("sample-2", report.methods());

        assertThatThrownBy(() -> JacocoReport.merge(List.of(report, again)))
                .isInstanceOf(JacocoParseException.class)
                .hasMessageContaining("class sample/Calculator appears in both sample and sample-2")
                .hasMessageContaining("merge the JaCoCo exec files");
    }

    @Test
    void rejects_negative_and_non_numeric_counter_values() {
        assertThatThrownBy(() -> parse(minimal("missed=\"-1\" covered=\"0\"")))
                .isInstanceOf(JacocoParseException.class)
                .hasMessageContaining("negative missed=\"-1\" at line 3");
        assertThatThrownBy(() -> parse(minimal("missed=\"abc\" covered=\"0\"")))
                .isInstanceOf(JacocoParseException.class)
                .hasMessageContaining("non-numeric missed=\"abc\" at line 3");
        assertThatThrownBy(() -> parse(minimal("missed=\"1\"")))
                .isInstanceOf(JacocoParseException.class)
                .hasMessageContaining("<counter> without covered attribute at line 3");
    }

    @Test
    void method_without_line_or_sourcefilename_is_tolerated() {
        JacocoReport parsed = parse("""
                <report name="r"><package name="p"><class name="p/A">
                <method name="m" desc="()V"><counter type="COMPLEXITY" missed="1" covered="0"/></method>
                </class></package></report>
                """);

        MethodCoverage method = parsed.methods().get(0);
        assertThat(method.line()).isZero();
        assertThat(method.sourceFile()).isNull();
        assertThat(MethodCrap.of(method).location()).isEqualTo("?");
    }

    @Test
    void parses_aggregated_reports_with_groups() {
        JacocoReport parsed = parse("""
                <report name="aggregate">
                  <group name="module-a"><package name="p"><class name="p/A" sourcefilename="A.java">
                    <method name="m" desc="()V" line="1"><counter type="COMPLEXITY" missed="1" covered="0"/></method>
                  </class></package></group>
                  <group name="module-b"><package name="q"><class name="q/B" sourcefilename="B.java">
                    <method name="m" desc="()V" line="1"><counter type="COMPLEXITY" missed="0" covered="1"/></method>
                  </class></package></group>
                </report>
                """);

        assertThat(parsed.methods()).extracting(MethodCoverage::className).containsExactly("p/A", "q/B");
    }

    @Test
    void rejects_directory_and_missing_file() {
        assertThatThrownBy(() -> JacocoXmlParser.parse(java.nio.file.Path.of(".")))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("not a regular file");
        assertThatThrownBy(() -> JacocoXmlParser.parse(java.nio.file.Path.of("missing.xml")))
                .isInstanceOf(java.nio.file.NoSuchFileException.class);
    }

    private static String minimal(String counterAttributes) {
        return "<report name=\"r\"><package name=\"p\"><class name=\"p/A\">\n"
                + "<method name=\"m\" desc=\"()V\">\n"
                + "<counter type=\"COMPLEXITY\" " + counterAttributes + "/>\n"
                + "</method></class></package></report>";
    }

    private static JacocoReport parse(String xml) {
        return JacocoXmlParser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "inline");
    }
}
