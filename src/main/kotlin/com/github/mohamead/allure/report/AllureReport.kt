package com.github.mohamead.allure.report

import io.qameta.allure.ConfigurationBuilder
import io.qameta.allure.Extension
import io.qameta.allure.ReportGenerator
import io.qameta.allure.behaviors.BehaviorsPlugin
import io.qameta.allure.core.Configuration
import io.qameta.allure.junitxml.JunitXmlPlugin
import io.qameta.allure.packages.PackagesPlugin
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory

/**
 * Generates Allure 2 reports from Kotlin/Java code.
 *
 * ```
 * val report = AllureReport.generate {
 *     results("build/allure-results")
 *     output("build/allure-report")
 *     reportName = "My service"
 *     clean = true
 * }
 * println(report.indexHtml)
 * ```
 */
class AllureReport(private val options: ReportOptions = ReportOptions()) {

    /**
     * Generates a report from [resultsDirectories] into [outputDirectory].
     *
     * @throws AllureReportException if no results directory exists, or generation fails.
     */
    fun generate(resultsDirectories: List<Path>, outputDirectory: Path): ReportResult {
        val readable = resultsDirectories.filter { it.isDirectory() }
        if (readable.isEmpty()) {
            throw AllureReportException(
                "None of the Allure results directories exist or are directories: $resultsDirectories",
            )
        }

        try {
            if (options.cleanOutputDirectory) {
                outputDirectory.toFile().deleteRecursively()
            }
            Files.createDirectories(outputDirectory)

            // Allure (2.46+) loads web resources through the thread context class loader. In hosts with
            // their own class loaders (IDE plugins, app servers) that is not the loader holding Allure.
            withContextClassLoader(ReportGenerator::class.java.classLoader) {
                BundledPlugins().use { bundled ->
                    val generator = ReportGenerator(buildConfiguration(bundled))
                    if (options.singleFile) {
                        generator.generateSingleFile(outputDirectory, readable)
                    } else {
                        generator.generate(outputDirectory, readable)
                    }
                }
            }
            stripBrowserAnalytics(outputDirectory.resolve("index.html"))
        } catch (e: RuntimeException) {
            throw AllureReportException("Allure report generation failed: ${e.message}", e)
        }

        return ReportResult(outputDirectory, readable, options.singleFile)
    }

    fun generate(resultsDirectory: Path, outputDirectory: Path): ReportResult =
        generate(listOf(resultsDirectory), outputDirectory)

    fun generate(resultsDirectories: List<File>, outputDirectory: File): ReportResult =
        generate(resultsDirectories.map(File::toPath), outputDirectory.toPath())

    /**
     * Generates a report from [resultsDirectories] into a temporary directory and serves it.
     * The temporary report is deleted when the returned server is closed, or at JVM exit.
     *
     * @param port port to listen on; `0` picks a free one.
     * @param openBrowser also open the report in the system browser.
     */
    fun serve(
        resultsDirectories: List<Path>,
        host: String = DEFAULT_HOST,
        port: Int = 0,
        openBrowser: Boolean = true,
    ): ReportServer {
        val tmp = Files.createTempDirectory("allure-serve-")
        // Closing the server removes the temp report; the hook covers callers that never close it.
        val deleteTmp = Thread { tmp.toFile().deleteRecursively() }
        Runtime.getRuntime().addShutdownHook(deleteTmp)
        val cleanup = {
            try {
                Runtime.getRuntime().removeShutdownHook(deleteTmp)
            } catch (_: IllegalStateException) {
                // JVM is already shutting down, so the hook is running (or about to).
            }
            tmp.toFile().deleteRecursively()
            Unit
        }
        try {
            val report = generate(resultsDirectories, tmp.resolve("allure-report"))
            return ReportServer.start(report.outputDirectory, host, port, cleanup)
                .also { if (openBrowser) it.openInBrowser() }
        } catch (e: Exception) {
            cleanup()
            throw e
        }
    }

    private inline fun withContextClassLoader(loader: ClassLoader, block: () -> Unit) {
        val thread = Thread.currentThread()
        val previous = thread.contextClassLoader
        thread.contextClassLoader = loader
        try {
            block()
        } finally {
            thread.contextClassLoader = previous
        }
    }

    internal fun buildConfiguration(bundled: BundledPlugins): Configuration =
        NoAnalyticsConfiguration(
            (if (options.useBundledExtensions) bundledConfiguration() else ConfigurationBuilder.empty())
                .withExtensions(options.extensions)
                .withPlugins(if (options.screenDiff) listOf(bundled.screenDiff()) + options.plugins else options.plugins)
                .withReportName(options.reportName)
                .withReportLanguage(options.language)
                .build(),
        )

    /** `ConfigurationBuilder.bundled()` plus the plugins it leaves out; the CLI loaded those from its plugins dir. */
    private fun bundledConfiguration() =
        ConfigurationBuilder.bundled().withExtensions(
            listOf<Extension>(
                JunitXmlPlugin(),
                BehaviorsPlugin(),
                PackagesPlugin(),
            ),
        )

    companion object {
        const val DEFAULT_HOST = "localhost"

        /**
         * Serves an already generated report directory on a local web server.
         *
         * @param port port to listen on; `0` picks a free one.
         * @param openBrowser also open the report in the system browser.
         */
        fun open(
            reportDirectory: Path,
            host: String = DEFAULT_HOST,
            port: Int = 0,
            openBrowser: Boolean = true,
        ): ReportServer =
            ReportServer.start(reportDirectory, host, port).also { if (openBrowser) it.openInBrowser() }

        /** Generates a report configured through the [ReportRequestBuilder] DSL. */
        fun generate(configure: ReportRequestBuilder.() -> Unit): ReportResult {
            val request = ReportRequestBuilder().apply(configure)
            return AllureReport(request.toOptions()).generate(request.resultsDirectories(), request.outputDirectory())
        }
    }
}
