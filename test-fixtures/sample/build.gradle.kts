// Fixture project for crap4java, derived from https://github.com/LarsEckart/bootstrap.
// Its only job is to produce a JaCoCo XML report with known edge cases:
// uncovered/partially/fully covered methods, lambdas (incl. in constructor and static init),
// inner/nested/anonymous classes, overloads, enum switch, record compact constructor.
plugins {
    java
    jacoco
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

jacoco {
    toolVersion = "0.8.15"
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = false
        csv.required = false
    }
}

// Regenerates the checked-in fixture used by crap4java's tests, with machine-specific session info scrubbed.
tasks.register("copyFixture") {
    group = "build"
    description = "Copies the JaCoCo XML report to core/src/test/resources/sample-jacoco.xml"
    dependsOn(tasks.jacocoTestReport)
    val source = layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml")
    val target = layout.projectDirectory.file("../../core/src/test/resources/sample-jacoco.xml")
    inputs.file(source)
    outputs.file(target)
    doLast {
        val scrubbed = source.get().asFile.readText()
            .replace(Regex("<sessioninfo [^>]*/>"), "<sessioninfo id=\"sample\" start=\"0\" dump=\"0\"/>")
        target.asFile.writeText(scrubbed)
    }
}
