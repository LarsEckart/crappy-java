// Root build. Modules: core (analysis library), cli (command line), gradle-plugin.
// The fixture under test-fixtures/sample is a standalone build; see PLAN.md.

plugins {
    base
}

val releaseVersion = providers.gradleProperty("releaseVersion").orElse("0.1.0-SNAPSHOT")

allprojects {
    group = "com.larseckart"
    version = releaseVersion.get()
}

// Dogfooding: run our own CLI over the JaCoCo reports of our own modules.
// Module names and report paths are spelled out so the root never touches subproject models
// (isolated-projects friendly); string task paths are allowed.
val crapCliDependencies = configurations.dependencyScope("crapCliDependencies")
val crapCli = configurations.resolvable("crapCli") {
    extendsFrom(crapCliDependencies.get())
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

dependencies {
    add(crapCliDependencies.name, project(":cli"))
}

val modules = listOf("core", "cli", "gradle-plugin")

tasks.register<JavaExec>("crap") {
    group = "verification"
    description = "CRAP report for crappy-java's own modules (exit 2 fails the build)."
    classpath = crapCli.get()
    mainClass = "crappyjava.cli.Main"
    workingDir = layout.projectDirectory.asFile
    val reports = modules.map { "$it/build/reports/jacoco/test/jacocoTestReport.xml" }
    modules.forEach { dependsOn(":$it:jacocoTestReport") }
    inputs.files(reports.map { layout.projectDirectory.file(it) })
    args(reports)
}

tasks.check {
    dependsOn("crap")
}
