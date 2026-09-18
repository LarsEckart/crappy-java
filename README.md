# crappy-java

CRAP (Change Risk Anti-Patterns) analyser for JVM projects, computed entirely from JaCoCo XML reports.

    CRAP(m) = CC(m)^2 * (1 - cov(m))^3 + CC(m)

A method with high cyclomatic complexity and low test coverage is risky to change. CRAP puts a
number on that: fully covered code scores its complexity, uncovered code scores roughly complexity
squared. The metric comes from Alberto Savoia and Bob Evans, see
[This Code is CRAP](https://testing.googleblog.com/2011/02/this-code-is-crap.html). The original
threshold of 30 is the default here.

## Installation

You need Java 17 or newer. Apply the published Gradle plugin with normal plugin resolution:

```kotlin
plugins {
    java
    jacoco
    id("com.larseckart.crappy-java") version "<version>"
}
```

Use the reusable core library from Maven Central:

```kotlin
dependencies {
    implementation("com.larseckart:crappy-java-core:<version>")
}
```

For development, clone this repository and run `./gradlew build`. The build also produces the runnable
CLI at `cli/build/install/crappy-java/bin/crappy-java`.

## How it works

JaCoCo already computes cyclomatic complexity for every non-abstract method and records how much of
it tests exercised. crappy-java reads the `COMPLEXITY` counter from a JaCoCo XML report and uses

- `CC = missed + covered`
- `cov = covered / (missed + covered)`, JaCoCo's complexity coverage, the closest available
  measure to the basis-path coverage the original crap4j used.

No source code is parsed. That means no matching of source methods to bytecode, Kotlin and Groovy
work too, and JaCoCo's filters already remove synthetic methods, Lombok output, enum helpers and
try-with-resources noise. It also means complexity is bytecode-level: exceptions do not count,
string `switch` and enhanced `for` add branches.

javac compiles lambda bodies to separate methods named `lambda$foo$0`. crappy-java folds them into
`foo` by summing counters, so each lambda adds its own baseline complexity of 1 to `foo`. When `foo`
is overloaded the lambda cannot be attributed and is reported as its own row.

Several reports can be analysed together as long as they cover different classes, for example one
report per module. The same class in two reports is refused, because coverage from two runs cannot
be combined from XML. Merge the exec files instead, with `jacoco-report-aggregation` or `jacoco:merge`.

## Gradle plugin

During development, use the plugin from a checkout with an included build:

```kotlin
// settings.gradle.kts of your project
pluginManagement {
    includeBuild("../crappy-java")
}
```

```kotlin
// build.gradle.kts
plugins {
    java
    jacoco
    id("com.larseckart.crappy-java")
}

crappyJava {
    threshold = 30.0        // default 30; Groovy DSL accepts `threshold = 30`
    failOnViolation = true  // default true
    showAll = false         // default false: list only methods above the threshold
    top = 0                 // default 0: when set, list the n worst methods instead
}
```

`./gradlew crap` runs tests, generates the JaCoCo XML report, prints the CRAP report and writes it
to `build/reports/crappy-java/crap.txt`. The build fails when any method scores above the threshold,
and also when a configured report file is missing, so a typo or a module without tests cannot turn
the gate green. To make it part of `check`, add `tasks.check { dependsOn("crap") }`.

Only the standard `test` task and its `jacocoTestReport` are wired. For other test tasks, or without
the `jacoco` plugin, point the task at report files yourself:

```kotlin
tasks.named<crappyjava.gradle.CrapTask>("crap") {
    reports.setFrom("path/to/jacoco.xml", "another/module/jacoco.xml")
    threshold.set(10.0)   // the task property is a plain Property<Double>; the extension also takes integers
}
```

## Command line

```bash
./gradlew :cli:installDist
cli/build/install/crappy-java/bin/crappy-java [--threshold <n>] [--all | --top <n>] <jacoco.xml>...
```

| Option | Meaning |
|---|---|
| `--threshold <n>` | CRAP above which a method is crappy and the run fails. Default 30. |
| `--all` | List every method, not only crappy ones. |
| `--top <n>` | List the n worst methods regardless of threshold. |
| `--help` | Usage. |

Exit codes: `0` nothing above the threshold, `1` usage error or unreadable report, `2` threshold exceeded.

## Report format

Deterministic, one method per line, ASCII apart from identifiers, meant to be read by people and by AI agents.

```
CRAP report  threshold=30.0  source=build/reports/jacoco/test/jacocoTestReport.xml

     CRAP    CC  COV%   METHOD
    182.0    13   0.0   sample.Uncovered.classify(IILjava/lang/String;)Ljava/lang/String;  Uncovered.java:8
     42.0     6   0.0   sample.Pipelines.upper(Z)Ljava/util/List;  Pipelines.java:35

methods=30 crappy=2 (6.7%) max=182.0 threshold=30.0 verdict=FAIL
```

Methods are identified by class, name and JVM descriptor so overloads stay apart. When nothing is
above the threshold the table is omitted and only the summary line prints.

## Building

Java 17 toolchain, Gradle wrapper included.

```bash
./gradlew build
```

Modules: `core` (analysis library, no dependencies), `cli`, `gradle-plugin`.
`test-fixtures/sample` is a standalone Gradle project whose JaCoCo report is checked in as the
test fixture. Regenerate it with `./gradlew -p test-fixtures/sample copyFixture`. See `PLAN.md` for
the design decisions.

## Releasing

Publish a GitHub release tagged `v<version>`. The release workflow builds the project, validates the
Plugin Portal upload, publishes signed `crappy-java-core` artifacts to Maven Central, waits for them to resolve,
then publishes and smoke-tests the plugin.

Add these GitHub repository secrets before the first release:

- `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD`: a Maven Central **user token**, not your
  Central Portal sign-in.
- `SIGNING_IN_MEMORY_KEY`: an ASCII-armored private PGP key from
  `gpg --export-secret-keys --armor <key-id>`.
- `SIGNING_IN_MEMORY_KEY_PASSWORD`: the private key passphrase.
- `GRADLE_PUBLISH_KEY` and `GRADLE_PUBLISH_SECRET`: Gradle Plugin Portal API credentials.

To repeat the final check after a Portal release, run
`./scripts/smoke-released-plugin.sh <version>`. It creates a temporary Gradle project with no
`pluginManagement` block, so it uses the default Plugin Portal resolution path.
