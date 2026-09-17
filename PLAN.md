# crap4java — Plan

A CRAP (Change Risk Anti-Patterns) analyser for JVM projects, computed entirely
from JaCoCo XML reports.

    CRAP(m) = CC(m)^2 * (1 - cov(m))^3 + CC(m)

Origin: Alberto Savoia, https://testing.googleblog.com/2011/02/this-code-is-crap.html
Inspiration: https://github.com/unclebob/crap4java (CLI shape, exit codes, report style)

## Decisions (2026-09-14)

| Topic | Decision |
|---|---|
| Complexity source | JaCoCo `COMPLEXITY` counter per method (`missed + covered`). No source parsing. |
| Coverage `cov(m)` | `covered / (missed + covered)` of the `COMPLEXITY` counter. Closest to the original's basis-path coverage. |
| Delivery | Core library + CLI + Gradle plugin. The tool never runs tests; it consumes an existing `jacoco.xml`. |
| Gate | Fail when the maximum per-method CRAP exceeds the threshold. Default threshold 30. Configurable. |
| Language | Java 17, zero runtime dependencies. StAX for XML. |
| Lambdas | Fold `lambda$foo$N` into `foo` when `foo` is unambiguous in the class, otherwise report the lambda as its own row. |
| Output | Console text only, designed to be read by an AI agent: deterministic, plain ASCII, no colours, compact. |
| Publishing | Local only. Name stays `crap4java`. |
| Not in v1 | JSON/HTML reports, `--changed` git mode, exclusion globs, baseline/ratchet mode, CRAP load, Maven plugin, aggregation across modules. |

## Why JaCoCo-only

JaCoCo already computes cyclomatic complexity per non-abstract method and
records how much of it tests exercised. Using it for both inputs means:

- no source-to-bytecode method matching (overloads, inner classes, records, anonymous classes)
- complexity and coverage measure the same artefact, so they cannot disagree
- JaCoCo's filters already drop synthetic methods, Lombok output, enum `values`/`valueOf`, try-with-resources noise
- Kotlin, Groovy and Scala work for free

Known consequences: CC numbers are bytecode-level. Exceptions are excluded, string `switch` and
enhanced `for` add branches, and every folded lambda adds its own baseline CC of 1 to the enclosing
method. Document this; do not "fix" it.

## Module layout

Gradle multi-project, Java 17 toolchain, JUnit 5.

    settings.gradle.kts        include("core", "cli", "gradle-plugin")
    core/                      library, no deps
    cli/                       thin main() over core, builds a runnable jar
    gradle-plugin/             java-gradle-plugin, depends on core
    test-fixtures/sample/      standalone Gradle build derived from github.com/LarsEckart/bootstrap
                               (Java 25, JUnit 6, AssertJ, JaCoCo 0.8.15). Regenerate the fixture XML with
                                 ./gradlew -p test-fixtures/sample copyFixture
                               which scrubs <sessioninfo> (hostname, timestamps).

### core

- `JacocoReport` model: `Package -> Class(name, sourceFile) -> Method(name, desc, line, counters)`.
  Counters kept as `missed`/`covered` per type (`INSTRUCTION`, `BRANCH`, `LINE`, `COMPLEXITY`, `METHOD`).
- `JacocoXmlParser`: StAX, DTD loading disabled, external entities disabled. Accepts one or more files.
- `LambdaFolder`: merges `lambda$foo$N` into `foo` by summing `COMPLEXITY` missed/covered.
  Rules: strip `lambda$` prefix and trailing `$N`; if exactly one non-lambda method with that
  name exists in the same class, fold; otherwise keep the lambda as a separate row.
  `lambda$new$N` folds into the constructor `<init>` only if there is exactly one constructor;
  `lambda$static$N` folds into `<clinit>`.
- `CrapCalculator`: `crap(cc, cov)`; `cov = covered / (missed + covered)`. Methods always have
  `cc >= 1` in JaCoCo, so no division by zero.
- `Analysis`: list of `MethodCrap(className, methodName, desc, line, cc, coverage, crap)` sorted
  by crap desc, then class, then line. Summary: method count, crappy count and percentage, max crap,
  threshold, verdict.
- `TextReporter`: agent-friendly text (see below).
- `Gate`: `exceeded = maxCrap > threshold`.

### cli

    crap4java [--threshold <n>] [--all] [--top <n>] <jacoco.xml>...
    crap4java --help

- default: print methods with `crap > threshold`, then summary
- `--all`: print every method
- `--top n`: print the n worst methods regardless of threshold
- exit codes as in Uncle Bob's tool: `0` ok, `1` usage error, `2` threshold exceeded
- missing or unreadable XML: exit `1` with a one-line error on stderr

### gradle-plugin

- plugin id `crap4java`, applies nothing itself but reacts to the `jacoco` plugin
- sets `jacocoTestReport.reports.xml.required = true`
- registers task `crap` (type `CrapTask`, `dependsOn(jacocoTestReport)`):
  inputs: XML report file(s), threshold, `failOnViolation`, `showAll`, `top`;
  output: the text report to console and to `build/reports/crap4java/crap.txt`
- extension `crap4java { threshold.set(30.0); failOnViolation.set(true) }`
- not wired into `check` by default; users add `tasks.check { dependsOn("crap") }`
- multi-module: point `reports` at several XML files, or at the aggregation plugin's output. Aggregation itself is out of scope.

## Text report format (for agents)

Goals: stable column order, one method per line, greppable, no wrapping, no
Unicode, no ANSI. Method identity is fully qualified so an agent can jump to it.

    CRAP report  threshold=30.0  source=build/reports/jacoco/test/jacocoTestReport.xml

      CRAP    CC  COV%   METHOD
    420.0    20   0.0   com.acme.billing.InvoiceCalculator.compute(Lcom/acme/Order;)D  InvoiceCalculator.java:42
     58.3    12  33.3   com.acme.billing.TaxRules.rateFor(Ljava/lang/String;)D  TaxRules.java:17

    methods=184 crappy=2 (1.1%) max=420.0 threshold=30.0 verdict=FAIL

- `desc` is the JVM descriptor from JaCoCo, printed verbatim. It is ugly but unambiguous for overloads.
- columns: `%9.1f %5d %5.1f`, aligned for CRAP < 10^8 and CC < 10^5; parse by whitespace anyway.
- several input reports must cover disjoint classes; the same class twice is refused (coverage cannot be
  unioned from XML). An opt-in "max covered" approximation is a possible later addition.
- when nothing exceeds the threshold, the table is omitted and only the summary line prints, with `verdict=OK`
- `--all` / `showAll` prints every method in the same table

## Testing strategy

- `core`: unit tests on the calculator (known values, e.g. cc=20 cov=0 -> 420.0, cc=20 cov=1 -> 20.0, cc=12 cov=1/3 -> 54.67) and on the lambda folder.
  Parser tests run against a checked-in `jacoco.xml` generated once from `test-fixtures/sample`.
  Reporter tests compare exact output strings.
- `test-fixtures/sample`: classes chosen to exercise edge cases:
  uncovered complex method, fully covered complex method, partially covered branches,
  stream pipeline with lambdas, inner class, overloaded methods, constructor with logic, enum, record.
- `cli`: tests call `main` with the fixture XML and assert output and exit code.
- `gradle-plugin`: Gradle TestKit functional test that runs `./gradlew crap` on the fixture and
  asserts on console output and build success/failure for two thresholds.
- Dogfood: apply `jacoco` and `crap4java` to this repo once the plugin exists.

## Implementation order

1. DONE 2026-09-14. Repo skeleton: multi-project settings, Java 17 toolchain, version catalog, `core` and `cli` modules, JUnit 6 + AssertJ + ApprovalTests for tests.
2. DONE 2026-09-14. Fixture project and checked-in `core/src/test/resources/sample-jacoco.xml`.
   Confirmed in the XML: `lambda$new$0`, `lambda$static$0`, `lambda$countLong$0`, `lambda$upper$0/1`,
   `lambda$map$0/1` under two `map` overloads, `Outer$Inner`, `Outer$Nested`, `Outer$1`, record compact
   `<init>` with CC 3, enum `sides` with CC 3 and the `$SwitchMap` synthetic filtered out,
   `Uncovered.classify` with CC 13 and zero coverage (expected CRAP 182.0),
   `Calculator.partial` CC 4 with 1 of 4 covered (expected CRAP 10.75).
3. DONE 2026-09-14. `JacocoXmlParser` (StAX, DTD not resolved) + model, tests against the fixture XML.
4. DONE 2026-09-14. `Crap`, `LambdaFolder`, `MethodCrap`, `Analysis` (gate = `maxCrap > threshold`), unit tests.
5. DONE 2026-09-14. `TextReporter`, ApprovalTests-approved output for default/all/top/summary-only.
6. DONE 2026-09-14. `cli` module, exit codes 0/1/2, tests.
7. DONE 2026-09-14. `gradle-plugin`: extension `crap4java {}`, task `crap`, wired to `test` + `jacocoTestReport`,
   configuration-cache compatible, eight TestKit tests (the end-to-end one needs Maven Central for JUnit)
   plus eleven in-process ProjectBuilder tests.
   Extension is a concrete class with `setThreshold(Number)` because Groovy literals arrive as Integer/BigDecimal
   and `Property<Double>` rejects both; Gradle forbids a setter next to an abstract managed property.
   Report wiring uses `jacocoReport.map(...)` because `Report.outputLocation` has no owning task and
   `flatMap` on it fails dependency inference.
8. DONE 2026-09-14. README.
9. DONE 2026-09-14. Dogfood: every module applies `jacoco`; root task `crap` runs the CLI over the three
   module reports and is wired into `check`. The plugin cannot be applied to its own build, so the CLI is used.

## Review before first commit (2026-09-14)

Two independent reviews (core+CLI, plugin+build+docs). Fixed: duplicate classes across reports now refused
instead of silently doubling rows; negative/non-numeric counters and directories/missing files give clear
errors; empty reports warn; StAX pinned to the JDK factory; `--all`/`--top` exclusive, `--help` wins,
non-finite thresholds rejected; wider report columns; UTF-8 stdout; task no longer `@SkipWhenEmpty`
(a configured-but-missing report fails), `@DisableCachingByDefault`, absolute path sensitivity, strict
`validatePlugins`; root dogfood task isolated-projects safe and free of deprecated APIs; fixture
regeneration task; fixture paths via system property; gitattributes/gitignore/gradle.properties cleaned.

## Open items for later

- Build-logic convention plugin to dedupe the three module build scripts (toolchain, JUnit, JaCoCo, lint).
- Wire additional `Test`/`JacocoReport` tasks (integrationTest) automatically; today only `test`.
- Opt-in dedupe of the same class across reports using max covered per counter (conservative approximation).
- `--` terminator in the CLI for report paths starting with `-`.

- TestKit runs the plugin inside a separate Gradle daemon, so those tests contribute no JaCoCo coverage.
  Resolved for now by ProjectBuilder tests (`Crap4JavaPluginTest`, `CrapTaskTest`) that run in-process;
  the dogfood gate went from max 42.0 to 20.0. Passing the agent into the TestKit daemon remains an option.

- Lambda folding tiebreaker: in the fixture, each `lambda$map$N` carries the `line` of its enclosing
  overload (9 vs 13). When the name is ambiguous, the lambda `line` falling inside an overload's line
  range could disambiguate. Needs method end lines, which JaCoCo does not report directly; the
  `<line nr>` elements per sourcefile could approximate them. Not in v1.

- CRAP load: define as "additional covered paths needed per crappy method to get under threshold",
  flag methods that cannot get under it even at 100% (CC > threshold) as "needs refactor".
- Baseline/ratchet mode for legacy codebases.
- `--changed` via git and JaCoCo's `sourcefile` element.
- JSON output, HTML output.
- Exclusion globs.
- Publishing to the Gradle Plugin Portal under a real group id.
