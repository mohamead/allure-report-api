package com.github.mohamead.allure.report

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AllureReportTest {

    @TempDir
    lateinit var tmp: Path

    private fun writeResults(): Path {
        val dir = Files.createDirectories(tmp.resolve("allure-results"))
        dir.resolve("1-result.json").writeText(
            """
            {
              "uuid": "1", "historyId": "h1", "name": "adds numbers", "fullName": "demo.CalcTest.adds",
              "status": "passed", "stage": "finished", "start": 1000, "stop": 2000,
              "labels": [{"name": "suite", "value": "CalcTest"}]
            }
            """.trimIndent(),
        )
        return dir
    }

    @Test
    fun `generates multi-file report with dsl`() {
        val out = tmp.resolve("report")
        val result = AllureReport.generate {
            results(writeResults())
            output(out)
            reportName = "Demo report"
        }

        assertTrue(result.indexHtml.exists())
        assertTrue(out.resolve("data/test-cases").exists())
        assertContains(out.resolve("widgets/summary.json").readText(), "Demo report")
    }

    @Test
    fun `generates single file report`() {
        val out = tmp.resolve("single")
        val result = AllureReport(ReportOptions(singleFile = true)).generate(writeResults(), out)

        assertTrue(result.singleFile)
        assertTrue(result.indexHtml.exists())
        assertFalse(out.resolve("data").exists())
    }

    @Test
    fun `generates when context class loader cannot see allure`() {
        val thread = Thread.currentThread()
        val previous = thread.contextClassLoader
        thread.contextClassLoader = java.net.URLClassLoader(emptyArray(), null)
        try {
            val result = AllureReport().generate(writeResults(), tmp.resolve("tccl"))
            assertTrue(result.indexHtml.exists())
        } finally {
            thread.contextClassLoader = previous
        }
        assertTrue(thread.contextClassLoader === previous)
    }

    @Test
    fun `reads surefire junit xml reports`() {
        val dir = Files.createDirectories(tmp.resolve("surefire-reports"))
        dir.resolve("TEST-demo.CalcTest.xml").writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuite name="demo.CalcTest" tests="1" errors="0" skipped="0" failures="0" time="0.01">
              <testcase name="adds" classname="demo.CalcTest" time="0.01"/>
            </testsuite>
            """.trimIndent(),
        )
        val out = tmp.resolve("junit-report")

        AllureReport().generate(dir, out)

        assertTrue(Regex("\"total\"\\s*:\\s*1").containsMatchIn(out.resolve("widgets/summary.json").readText()))
    }

    @Test
    fun `never sends analytics`() {
        BundledPlugins().use { bundled ->
            assertTrue(AllureReport().buildConfiguration(bundled).extensions.none { it is io.qameta.allure.ga.GaPlugin })
        }

        for (singleFile in listOf(false, true)) {
            val out = tmp.resolve("no-analytics-$singleFile")
            AllureReport(ReportOptions(singleFile = singleFile)).generate(writeResults(), out)
            val html = out.resolve("index.html").readText()
            assertFalse(html.contains("googletagmanager"), "singleFile=$singleFile")
            assertFalse(html.contains("gtag("), "singleFile=$singleFile")
        }
    }

    @Test
    fun `includes screen-diff plugin unless disabled`() {
        val with = tmp.resolve("with")
        val without = tmp.resolve("without")
        AllureReport().generate(writeResults(), with)
        AllureReport(ReportOptions(screenDiff = false)).generate(writeResults(), without)

        assertTrue(with.resolve("plugin/screen-diff/index.js").exists())
        assertFalse(without.resolve("plugin/screen-diff").exists())
    }

    @Test
    fun `clean removes stale output`() {
        val out = Files.createDirectories(tmp.resolve("report"))
        val stale = out.resolve("stale.txt").also { it.writeText("old") }

        AllureReport(ReportOptions(cleanOutputDirectory = true)).generate(writeResults(), out)

        assertFalse(stale.exists())
    }

    @Test
    fun `fails when results directory is missing`() {
        assertFailsWith<AllureReportException> {
            AllureReport().generate(tmp.resolve("nope"), tmp.resolve("report"))
        }
    }

    @Test
    fun `serves generated report over http`() {
        AllureReport().serve(listOf(writeResults()), openBrowser = false).use { server ->
            val client = java.net.http.HttpClient.newHttpClient()
            fun status(path: String) = client.send(
                java.net.http.HttpRequest.newBuilder(server.url.resolve(path)).build(),
                java.net.http.HttpResponse.BodyHandlers.ofString(),
            ).statusCode()

            assertTrue(status("/") == 200)
            assertTrue(status("/widgets/summary.json") == 200)
            assertTrue(status("/nope.json") == 404)
        }
    }

    @Test
    fun `open fails for missing report directory`() {
        assertFailsWith<AllureReportException> { AllureReport.open(tmp.resolve("nope"), openBrowser = false) }
    }

    @Test
    fun `dsl requires results and output`() {
        assertFailsWith<AllureReportException> { AllureReport.generate { output(tmp) } }
        assertFailsWith<AllureReportException> { AllureReport.generate { results(tmp) } }
    }
}
