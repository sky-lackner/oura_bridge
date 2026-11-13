package net.ourabridge

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = try {
        SleepMirror(applicationContext).sync()
        Result.success()
    } catch (e: Exception) {
        Prefs.setLastLog(applicationContext, "Background sync failed: ${e.message}")
        Result.retry()
    }

    companion object {
        /** Mirrors new Oura sleep every hour in the background. */
        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(ctx)
                .enqueueUniquePeriodicWork("oura-mirror", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
