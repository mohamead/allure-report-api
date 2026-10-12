package com.github.mohamead.allure.report

import io.qameta.allure.Extension
import io.qameta.allure.core.Plugin
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/** DSL builder behind [AllureReport.generate]. */
class ReportRequestBuilder {
    private val results = mutableListOf<Path>()
    private var output: Path? = null
    private val extensions = mutableListOf<Extension>()
    private val plugins = mutableListOf<Plugin>()

    var reportName: String? = null
    var language: String? = null

    /** Delete the output directory before generating. */
    var clean: Boolean = false

    /** Produce a single self-contained `index.html`. */
    var singleFile: Boolean = false

    /** Register Allure's standard extensions (default). */
    var useBundledExtensions: Boolean = true

    /** Include Allure's screen-diff plugin (default). */
    var screenDiff: Boolean = true

    fun results(vararg directories: String) = apply { directories.mapTo(results) { Paths.get(it) } }
    fun results(vararg directories: Path) = apply { results.addAll(directories) }
    fun results(vararg directories: File) = apply { directories.mapTo(results, File::toPath) }

    fun output(directory: String) = apply { output = Paths.get(directory) }
    fun output(directory: Path) = apply { output = directory }
    fun output(directory: File) = apply { output = directory.toPath() }

    fun extension(extension: Extension) = apply { extensions += extension }
    fun plugin(plugin: Plugin) = apply { plugins += plugin }

    internal fun resultsDirectories(): List<Path> =
        results.ifEmpty { throw AllureReportException("No results directory configured; call results(...)") }

    internal fun outputDirectory(): Path =
        output ?: throw AllureReportException("No output directory configured; call output(...)")

    internal fun toOptions() = ReportOptions(
        reportName = reportName,
        language = language,
        cleanOutputDirectory = clean,
        singleFile = singleFile,
        useBundledExtensions = useBundledExtensions,
        screenDiff = screenDiff,
        extensions = extensions.toList(),
        plugins = plugins.toList(),
    )
}
