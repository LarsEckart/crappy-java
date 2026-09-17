plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "crap4java"

include("core", "cli", "gradle-plugin")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
