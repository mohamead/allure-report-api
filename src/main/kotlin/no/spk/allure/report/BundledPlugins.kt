package no.spk.allure.report

import io.qameta.allure.core.Plugin
import io.qameta.allure.plugin.DefaultPluginLoader
import java.nio.file.Files
import java.nio.file.Path

/**
 * Allure plugins that are only distributed as plugin zips (static assets + `allure-plugin.yml`), not as jars on
 * Maven Central, so their files are bundled in this library's resources. Allure's [DefaultPluginLoader] needs a
 * directory, so they are extracted to a temp directory that [close] deletes.
 */
internal class BundledPlugins : AutoCloseable {
    private var root: Path? = null

    /** Allure's screen-diff plugin (Apache 2.0, Qameta Software): a diff/overlay view for `screenshotDiff` tests. */
    fun screenDiff(): Plugin = extract("screen-diff", SCREEN_DIFF_FILES)

    private fun extract(name: String, files: List<String>): Plugin {
        val dir = (root ?: Files.createTempDirectory("allure-plugins-").also { root = it }).resolve(name)
        val loader = BundledPlugins::class.java.classLoader
        for (file in files) {
            val target = dir.resolve(file).also { Files.createDirectories(it.parent) }
            val resource = "allure-plugins/$name/$file"
            val stream = loader.getResourceAsStream(resource)
                ?: throw AllureReportException("Bundled Allure plugin file missing: $resource")
            stream.use { Files.copy(it, target) }
        }
        return DefaultPluginLoader().loadPlugin(loader, dir)
            .orElseThrow { AllureReportException("Could not load bundled Allure plugin '$name'") }
    }

    override fun close() {
        root?.toFile()?.deleteRecursively()
    }

    private companion object {
        val SCREEN_DIFF_FILES = listOf("allure-plugin.yml", "static/index.js", "static/styles.css")
    }
}
