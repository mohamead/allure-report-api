package no.spk.allure.report

import io.qameta.allure.Extension
import io.qameta.allure.core.Configuration
import io.qameta.allure.core.Plugin
import io.qameta.allure.ga.GaPlugin
import java.nio.file.Files
import java.nio.file.Path

/*
 * Allure phones home in two ways, both disabled only by the ALLURE_NO_ANALYTICS environment variable,
 * which a library cannot set for its own JVM. This API never sends analytics, so it removes both.
 */

/** [delegate] without Allure's [GaPlugin], which POSTs a `report_generated` event to google-analytics.com. */
internal class NoAnalyticsConfiguration(private val delegate: Configuration) : Configuration {
    private val extensions: List<Extension> = delegate.extensions.filterNot { it is GaPlugin }

    override fun getUuid(): String? = delegate.uuid
    override fun getVersion(): String? = delegate.version
    override fun getReportLanguage(): String? = delegate.reportLanguage
    override fun getReportName(): String? = delegate.reportName
    override fun getPlugins(): List<Plugin> = delegate.plugins
    override fun getExtensions(): List<Extension> = extensions
}

private val GTM_SCRIPT = Regex("""<script[^>]*googletagmanager\.com[^>]*>\s*</script>\s*""")
private val GTAG_INLINE = Regex("""<script>\s*window\.dataLayer\s*=.*?</script>\s*""", RegexOption.DOT_MATCHES_ALL)

/**
 * Removes the `gtag.js` snippet Allure writes into the report's `index.html`, which would make every
 * browser opening the report load Google Tag Manager.
 */
internal fun stripBrowserAnalytics(indexHtml: Path) {
    if (!Files.isRegularFile(indexHtml)) return
    val html = Files.readString(indexHtml)
    val stripped = html.replace(GTM_SCRIPT, "").replace(GTAG_INLINE, "")
    if (stripped != html) Files.writeString(indexHtml, stripped)
}
