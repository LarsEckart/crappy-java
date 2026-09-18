import org.gradle.plugin.compatibility.compatibility

plugins {
    `java-gradle-plugin`
    jacoco
    id("com.gradle.plugin-publish") version "2.2.1"
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
    website.set("https://github.com/LarsEckart/crappy-java")
    vcsUrl.set("https://github.com/LarsEckart/crappy-java.git")

    plugins {
        create("crappyJava") {
            id = "com.larseckart.crappy-java"
            implementationClass = "crappyjava.gradle.CrappyJavaPlugin"
            displayName = "crappy-java"
            description = "CRAP (Change Risk Anti-Patterns) report and quality gate computed from JaCoCo XML reports"
            tags.set(listOf("crap", "jacoco", "coverage", "quality", "metrics"))

            compatibility {
                features {
                    configurationCache = true
                }
            }
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
