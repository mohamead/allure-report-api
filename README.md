# allure-report-api
Kotlin library that wraps Allure 2 (`io.qameta.allure:allure-generator`) so any project can generate
Allure reports from code, without the CLI.

```kotlin
val report = AllureReport.generate {
    results("build/allure-results")   // one or more results directories
    output("build/allure-report")
    reportName = "My service"
    language = "en"
    clean = true                       // wipe output first
    singleFile = false                 // true -> one self-contained index.html
}
println(report.indexHtml)
```

Or without the DSL:

```kotlin
AllureReport(ReportOptions(reportName = "My service")).generate(resultsDir, outputDir)
```

Extra Allure `Extension`s / `Plugin`s can be passed via `ReportOptions` or `extension(...)` / `plugin(...)` in the DSL.

Build: `./gradlew build` (Java 17+). Install locally for other projects with `./gradlew publishToMavenLocal`.

## Using it in another project

The library is built from GitHub by [JitPack](https://jitpack.io/#mohamead/allure-report-api):

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    // a release tag, a commit hash, or <branch>-SNAPSHOT (e.g. main-SNAPSHOT)
    implementation("com.github.mohamead:allure-report-api:main-SNAPSHOT")
}
```

## Serve and open

```kotlin
// generate to a temp dir and serve it (temp dir removed on close)
AllureReport().serve(listOf(resultsDir)).use { server -> server.await() }

// serve an already generated report
AllureReport.open(reportDir, port = 8080)
```

Both start a non-blocking local web server and open the system browser (`openBrowser = false` to skip).
`ReportServer` exposes `url`, `await()` and `close()`.

## Screen diff

Allure's screen-diff plugin is included by default (`screenDiff = false` to turn it off). It adds a diff / overlay
view to tests labelled `testType=screenshotDiff` that attach `expected.png`, `actual.png` and `diff.png`.
The plugin only exists as a plugin zip upstream (the jar on Maven Central is empty), so its files are bundled in
`src/main/resources/allure-plugins/screen-diff`, copied from allure2 (Apache License 2.0, Qameta Software).
