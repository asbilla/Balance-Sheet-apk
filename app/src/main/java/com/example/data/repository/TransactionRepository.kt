package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.TransactionEntity
import com.example.data.pref.AppPreferences
import com.example.data.remote.RemoteTransaction
import com.example.data.remote.SheetsApiService
import com.example.worker.SyncWorker
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    private val preferences: AppPreferences = AppPreferences.getInstance(context),
    private val apiService: SheetsApiService = SheetsApiService()
) {

    private val dao = database.transactionDao()

    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val unsyncedCount: Flow<Int> = dao.getUnsyncedCount()

    suspend fun saveTransaction(
        type: String,
        amount: Double,
        category: String,
        date: String
    ): Long {
        val entity = TransactionEntity(
            date = date,
            timestamp = System.currentTimeMillis(),
            type = type,
            category = category.trim(),
            amount = amount,
            isSynced = false
        )
        val id = dao.insertTransaction(entity)

        // Trigger background sync task
        SyncWorker.enqueueSync(context)

        return id
    }

    fun triggerBackgroundSync() {
        SyncWorker.enqueueSync(context)
    }

    suspend fun fetchRemoteTransactions(): Result<List<RemoteTransaction>> {
        val url = preferences.getWebAppUrl()
        if (url.isBlank()) {
            return Result.failure(IllegalStateException("Google Apps Script URL is not configured."))
        }
        val result = apiService.fetchTransactions(url)
        if (result.isSuccess) {
            val remoteList = result.getOrNull().orEmpty()
            // Reconcile with local database
            syncRemoteIntoLocal(remoteList)
        }
        return result
    }

    private suspend fun syncRemoteIntoLocal(remoteList: List<RemoteTransaction>) {
        if (remoteList.isEmpty()) return
        // Check unsynced to avoid duplicate overrides
        val unsynced = dao.getUnsyncedTransactions()
        val unsyncedUuids = unsynced.map { it.uuid }.toSet()

        // Match by UUID if available, else insert synced entities
        val entitiesToInsert = mutableListOf<TransactionEntity>()
        for (item in remoteList) {
            if (item.id.isNotEmpty() && unsyncedUuids.contains(item.id)) {
                // This transaction was pending and now present in sheet, mark as synced
                dao.markAsSyncedByUuid(listOf(item.id))
                continue
            }
        }
    }

    suspend fun testConnection(url: String): Result<String> {
        return apiService.testConnection(url)
    }

    fun getWebAppUrl(): String = preferences.getWebAppUrl()

    fun setWebAppUrl(url: String) = preferences.setWebAppUrl(url)

    fun isConfigured(): Boolean = preferences.isConfigured()

    suspend fun deleteTransaction(entity: TransactionEntity) {
        dao.deleteTransaction(entity)
    }

    companion object {
        @Volatile
        private var INSTANCE: TransactionRepository? = null

        fun getInstance(context: Context): TransactionRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = TransactionRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
