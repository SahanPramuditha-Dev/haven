package com.example.haven.data.sync

import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class HavenSyncWorker(
    private val appContext: android.content.Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val familyId = inputData.getString(KEY_FAMILY_ID) ?: return Result.success()
        Log.i(TAG, "Executing background outbox sync worker for family: $familyId")

        return try {
            val syncManager = HavenSyncManager.getInstance(appContext)
            syncManager.drainAndSyncDirect(familyId)
            Log.i(TAG, "Background sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during background sync worker execution", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "HavenSyncWorker"
        const val WORK_NAME = "haven_outbox_sync_work"
        const val KEY_FAMILY_ID = "family_id"

        fun enqueue(context: android.content.Context, familyId: String? = null) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = workDataOf(KEY_FAMILY_ID to familyId)

            val workRequest = OneTimeWorkRequestBuilder<HavenSyncWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            Log.i(TAG, "Enqueued HavenSyncWorker successfully")
         }
    }
}