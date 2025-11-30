package io.ktelemetry.android.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.ktelemetry.android.TelemetryClient

class TelemetryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            TelemetryClient.getInstance().flush()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

