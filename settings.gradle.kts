plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "crappy-java"

include("core", "cli", "gradle-plugin")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
