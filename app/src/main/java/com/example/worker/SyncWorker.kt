package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.pref.AppPreferences
import com.example.data.remote.SheetsApiService

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val database = AppDatabase.getDatabase(appContext)
    private val preferences = AppPreferences.getInstance(appContext)
    private val apiService = SheetsApiService()

    override suspend fun doWork(): Result {
        val webAppUrl = preferences.getWebAppUrl()
        if (webAppUrl.isBlank()) {
            Log.w(TAG, "SyncWorker skipped: Google Apps Script Web App URL is not configured.")
            return Result.failure()
        }

        val unsyncedList = database.transactionDao().getUnsyncedTransactions()
        if (unsyncedList.isEmpty()) {
            Log.d(TAG, "SyncWorker: No unsynced transactions found.")
            return Result.success()
        }

        Log.d(TAG, "SyncWorker starting: Syncing ${unsyncedList.size} transactions to Sheets...")

        var anyFailures = false
        val successfullySyncedIds = mutableListOf<Long>()

        for (transaction in unsyncedList) {
            val result = apiService.postTransaction(webAppUrl, transaction)
            if (result.isSuccess) {
                successfullySyncedIds.add(transaction.id)
            } else {
                Log.e(TAG, "Failed syncing transaction #${transaction.id}: ${result.exceptionOrNull()?.message}")
                anyFailures = true
            }
        }

        if (successfullySyncedIds.isNotEmpty()) {
            database.transactionDao().markAsSynced(successfullySyncedIds)
            Log.d(TAG, "SyncWorker: Marked ${successfullySyncedIds.size} transactions as synced.")
        }

        return if (anyFailures) {
            Log.w(TAG, "SyncWorker completed with some errors, requesting retry.")
            Result.retry()
        } else {
            Log.d(TAG, "SyncWorker successfully synced all transactions.")
            Result.success()
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "google_sheets_sync_work"

        fun enqueueSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                syncRequest
            )
            Log.d(TAG, "SyncWorker request enqueued.")
        }
    }
}
