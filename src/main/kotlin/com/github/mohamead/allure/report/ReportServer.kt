package com.github.mohamead.allure.report

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.qameta.allure.DefaultResultsVisitor
import java.awt.Desktop
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.io.path.isDirectory

/**
 * A running local web server hosting a generated Allure report.
 *
 * Start one with [AllureReport.open] or [AllureReport.serve]. Servers do not block the caller:
 * call [close] to stop, or [await] to block until it is stopped.
 */
class ReportServer private constructor(
    private val server: HttpServer,
    private val onClose: () -> Unit,
) : AutoCloseable {

    private val closed = CountDownLatch(1)

    /** Address the report is served on. */
    val url: URI = URI.create("http://${server.address.hostString}:${server.address.port}")

    /** Tries to open [url] in the system browser. Returns `false` if the platform can't do that. */
    fun openInBrowser(): Boolean =
        try {
            val supported = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
            if (supported) Desktop.getDesktop().browse(url)
            supported
        } catch (_: IOException) {
            false
        } catch (_: UnsupportedOperationException) {
            false
        } catch (_: java.awt.AWTError) {
            false
        }

    /** Blocks until [close] is called. */
    fun await() = closed.await()

    override fun close() {
        if (closed.count == 0L) return
        server.stop(0)
        onClose()
        closed.countDown()
    }

    internal companion object {
        fun start(reportDirectory: Path, host: String, port: Int, onClose: () -> Unit = {}): ReportServer {
            if (!reportDirectory.isDirectory()) {
                throw AllureReportException("Report directory does not exist: $reportDirectory")
            }
            val root = reportDirectory.toAbsolutePath().normalize()
            val server = try {
                HttpServer.create(InetSocketAddress(host, port), 0)
            } catch (e: IOException) {
                throw AllureReportException("Could not start web server on $host:$port: ${e.message}", e)
            }
            server.createContext("/") { exchange -> handle(exchange, root) }
            server.executor = Executors.newCachedThreadPool { r -> Thread(r, "allure-report-server").apply { isDaemon = true } }
            server.start()
            return ReportServer(server, onClose)
        }

        private fun handle(exchange: HttpExchange, root: Path) {
            exchange.use {
                // Resolve against the report root and refuse anything that escapes it (e.g. "/../").
                val requested = root.resolve("." + exchange.requestURI.path).normalize()
                val file = if (requested.isDirectory()) requested.resolve("index.html") else requested
                if (requested.startsWith(root) && Files.isRegularFile(file)) {
                    exchange.responseHeaders.add("Content-Type", DefaultResultsVisitor.probeContentType(file))
                    exchange.sendResponseHeaders(200, Files.size(file))
                    Files.copy(file, exchange.responseBody)
                } else {
                    val body = "404 Not Found".toByteArray()
                    exchange.sendResponseHeaders(404, body.size.toLong())
                    exchange.responseBody.write(body)
                }
            }
        }
    }
}
