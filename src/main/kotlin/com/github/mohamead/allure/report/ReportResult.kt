package com.github.mohamead.allure.report

import java.nio.file.Path

/** Outcome of a successful report generation. */
data class ReportResult(
    /** Directory the report was written to. */
    val outputDirectory: Path,
    /** Results directories that were actually read. */
    val resultsDirectories: List<Path>,
    /** Whether the report is a single self-contained HTML file. */
    val singleFile: Boolean,
) {
    /** The report entry point (`index.html`). */
    val indexHtml: Path get() = outputDirectory.resolve("index.html")
}
