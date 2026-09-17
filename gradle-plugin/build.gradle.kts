plugins {
    `java-gradle-plugin`
    jacoco
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    implementation(project(":core"))

    testImplementation(gradleTestKit())
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testRuntimeOnly(libs.junit.platform.launcher)
}

gradlePlugin {
    plugins {
        create("crap4java") {
            id = "crap4java"
            implementationClass = "crap4java.gradle.Crap4JavaPlugin"
            displayName = "crap4java"
            description = "CRAP (Change Risk Anti-Patterns) report and quality gate computed from JaCoCo XML reports"
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:all,-processing")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
    // functional tests read the fixture report from the core module
    inputs.file(layout.projectDirectory.file("../core/src/test/resources/sample-jacoco.xml"))
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = false
    }
}
