package com.github.mohamead.allure.report

import io.qameta.allure.Extension
import io.qameta.allure.core.Plugin

/**
 * Settings for a single Allure report generation.
 *
 * @property reportName title shown in the report header; Allure's default when `null`.
 * @property language report UI language (e.g. `"en"`, `"nb"`); Allure's default when `null`.
 * @property cleanOutputDirectory delete the output directory before generating.
 * @property singleFile produce a self-contained `index.html` instead of a multi-file report.
 * @property useBundledExtensions register Allure's standard readers, aggregators and widgets.
 *   Disable only if you supply everything yourself through [extensions] / [plugins].
 * @property screenDiff include Allure's screen-diff plugin, which adds a diff/overlay view to tests labelled
 *   `testType=screenshotDiff` that attach `expected.png`, `actual.png` and `diff.png`.
 * @property extensions additional Allure [Extension]s (readers, aggregators, contexts).
 * @property plugins additional Allure [Plugin]s.
 */
data class ReportOptions(
    val reportName: String? = null,
    val language: String? = null,
    val cleanOutputDirectory: Boolean = false,
    val singleFile: Boolean = false,
    val useBundledExtensions: Boolean = true,
    val screenDiff: Boolean = true,
    val extensions: List<Extension> = emptyList(),
    val plugins: List<Plugin> = emptyList(),
)
