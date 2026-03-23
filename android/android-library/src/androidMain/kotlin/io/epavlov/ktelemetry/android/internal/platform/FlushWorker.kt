package io.epavlov.ktelemetry.android.internal.platform

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.epavlov.ktelemetry.android.KTelemetry

internal class FlushWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        KTelemetry.awaitFlush()
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "ktelemetry_flush"
    }
}
