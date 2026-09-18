import com.vanniktech.maven.publish.DeploymentValidation

plugins {
    `java-library`
    jacoco
    id("com.vanniktech.maven.publish") version "0.37.0"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true, validateDeployment = DeploymentValidation.PUBLISHED)
    signAllPublications()

    coordinates(project.group.toString(), "crappy-java-core", project.version.toString())

    pom {
        name.set("crappy-java core")
        description.set("CRAP (Change Risk Anti-Patterns) analysis from JaCoCo XML reports")
        inceptionYear.set("2026")
        url.set("https://github.com/LarsEckart/crappy-java")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("LarsEckart")
                name.set("Lars Eckart")
                url.set("https://github.com/LarsEckart")
            }
        }

        scm {
            url.set("https://github.com/LarsEckart/crappy-java")
            connection.set("scm:git:git://github.com/LarsEckart/crappy-java.git")
            developerConnection.set("scm:git:ssh://git@github.com/LarsEckart/crappy-java.git")
        }

        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/LarsEckart/crappy-java/issues")
        }
    }
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(libs.approvaltests)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:all")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
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
