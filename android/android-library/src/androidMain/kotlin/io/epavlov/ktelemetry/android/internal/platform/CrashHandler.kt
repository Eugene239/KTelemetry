package io.epavlov.ktelemetry.android.internal.platform

import io.epavlov.ktelemetry.android.KTelemetry
import io.epavlov.ktelemetry.core.ErrorInfo
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class CrashHandler(
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(
        thread: Thread,
        exception: Throwable,
    ) {
        try {
            val sw = StringWriter()
            exception.printStackTrace(PrintWriter(sw))

            val allThreads =
                Thread.getAllStackTraces().entries.joinToString("\n\n") { (t, frames) ->
                    "\"${t.name}\" (${t.state})\n" + frames.joinToString("\n") { "    at $it" }
                }

            val done = CountDownLatch(1)
            KTelemetry.enqueueFatalCrashAndFlush(
                ErrorInfo(
                    type = exception.javaClass.name,
                    message = exception.message,
                    stacktrace = sw.toString(),
                    isFatal = true,
                    threadName = thread.name,
                    threads = allThreads,
                ),
            ) {
                done.countDown()
            }
            done.await(CRASH_FLUSH_WAIT_MS, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
        } finally {
            defaultHandler?.uncaughtException(thread, exception)
        }
    }

    companion object {
        private const val CRASH_FLUSH_WAIT_MS = 2000L

        fun install() {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(defaultHandler))
        }
    }
}
