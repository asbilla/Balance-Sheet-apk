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
    val businessProfile = preferences.businessProfileFlow

    fun getBusinessProfile(): com.example.data.pref.BusinessProfile = preferences.getBusinessProfile()

    suspend fun saveBusinessProfile(
        profile: com.example.data.pref.BusinessProfile,
        syncToSheets: Boolean = true
    ): Result<String> {
        preferences.setBusinessProfile(profile)
        val url = getWebAppUrl()
        if (syncToSheets && url.isNotBlank() && !url.contains("offline-demo")) {
            return apiService.postBusinessProfile(url, profile)
        }
        return Result.success("Saved locally")
    }

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

    suspend fun fetchPdfExportUrl(): Result<String> {
        val url = getWebAppUrl()
        return apiService.fetchPdfExportUrl(url)
    }

    fun getWebAppUrl(): String = preferences.getWebAppUrl()

    fun setWebAppUrl(url: String) = preferences.setWebAppUrl(url)

    fun isConfigured(): Boolean = preferences.isConfigured()

    suspend fun deleteTransaction(entity: TransactionEntity) {
        dao.deleteTransaction(entity)
    }

    suspend fun updateTransactionAmount(
        id: String,
        date: String,
        type: String,
        category: String,
        newAmount: Double
    ): Result<String> {
        // 1. Update in local Room database
        val existing = dao.getTransactionByUuid(id)
        val targetUuid = if (existing != null) {
            val updated = existing.copy(
                amount = newAmount,
                category = category.ifBlank { existing.category },
                isSynced = false
            )
            dao.updateTransaction(updated)
            existing.uuid
        } else {
            val byDateAndType = dao.getTransactionsByDateAndType(date, type).firstOrNull()
            if (byDateAndType != null) {
                val updated = byDateAndType.copy(
                    amount = newAmount,
                    category = category.ifBlank { byDateAndType.category },
                    isSynced = false
                )
                dao.updateTransaction(updated)
                byDateAndType.uuid
            } else {
                val assignedUuid = if (id.isNotBlank()) id else java.util.UUID.randomUUID().toString()
                val newEntity = TransactionEntity(
                    uuid = assignedUuid,
                    date = date,
                    timestamp = System.currentTimeMillis(),
                    type = type,
                    category = category,
                    amount = newAmount,
                    isSynced = false
                )
                dao.insertTransaction(newEntity)
                assignedUuid
            }
        }

        // 2. Update to Google Sheets spreadsheet immediately if connected
        val url = getWebAppUrl()
        if (url.isNotBlank() && !url.contains("offline-demo")) {
            val remoteResult = apiService.updateTransactionOnSheets(
                webAppUrl = url,
                id = targetUuid,
                date = date,
                type = type,
                amount = newAmount,
                notes = category
            )
            if (remoteResult.isSuccess) {
                dao.markAsSyncedByUuid(listOf(targetUuid))
                return Result.success("Amount updated and synced to spreadsheet!")
            } else {
                SyncWorker.enqueueSync(context)
                return Result.success("Amount updated locally; queued for Sheets sync.")
            }
        } else {
            return Result.success("Amount updated locally.")
        }
    }

    suspend fun deleteTransactionPermanently(
        id: String,
        date: String,
        type: String
    ): Result<String> {
        // 1. Delete from local Room database
        if (id.isNotBlank()) {
            dao.deleteByUuid(id)
            val parsedId = id.toLongOrNull()
            if (parsedId != null) {
                dao.deleteById(parsedId)
            }
        } else if (date.isNotBlank() && type.isNotBlank()) {
            // Only fallback to date & type if id is not available
            dao.deleteByDateAndType(date, type)
        }

        // 2. Delete from Google Sheets if configured
        val url = getWebAppUrl()
        if (url.isNotBlank() && !url.contains("offline-demo")) {
            val remoteResult = apiService.deleteTransactionOnSheets(
                webAppUrl = url,
                id = id,
                date = date,
                type = type
            )
            if (remoteResult.isSuccess) {
                return Result.success("Transaction deleted from local database & Google Sheet!")
            } else {
                return Result.success("Deleted locally. Note: ${remoteResult.exceptionOrNull()?.message}")
            }
        } else {
            return Result.success("Transaction deleted locally.")
        }
    }

    fun getCachedProducts(): List<com.example.data.model.ProductItem> = preferences.getCachedProducts()

    suspend fun getProducts(forceRefresh: Boolean = false): List<com.example.data.model.ProductItem> {
        val cached = preferences.getCachedProducts()
        val url = getWebAppUrl()
        if (url.isBlank() || url.contains("offline-demo")) {
            return cached
        }

        if (forceRefresh || cached.isEmpty()) {
            val result = apiService.fetchProducts(url)
            if (result.isSuccess) {
                val remoteProducts = result.getOrDefault(emptyList())
                if (remoteProducts.isNotEmpty()) {
                    preferences.setCachedProducts(remoteProducts)
                    return remoteProducts
                }
            }
        }
        return cached
    }

    suspend fun refreshProductsFromSheets(): Result<List<com.example.data.model.ProductItem>> {
        val url = getWebAppUrl()
        if (url.isBlank()) {
            return Result.failure(IllegalStateException("Google Apps Script URL is not configured"))
        }
        val result = apiService.fetchProducts(url)
        if (result.isSuccess) {
            val remoteProducts = result.getOrDefault(emptyList())
            if (remoteProducts.isNotEmpty()) {
                preferences.setCachedProducts(remoteProducts)
            } else {
                // If remote is empty, attempt to push defaults
                val defaults = preferences.getCachedProducts()
                syncProductsToSheets(defaults)
            }
        }
        return result
    }

    suspend fun syncProductsToSheets(products: List<com.example.data.model.ProductItem>) {
        val url = getWebAppUrl()
        if (url.isBlank() || url.contains("offline-demo")) return
        
        // Push each product to sheets
        products.forEach { product ->
            apiService.postProduct(url, product)
        }
    }

    suspend fun saveProduct(product: com.example.data.model.ProductItem): Result<String> {
        val current = preferences.getCachedProducts().toMutableList()
        val existingIndex = current.indexOfFirst { it.name.equals(product.name, ignoreCase = true) }
        if (existingIndex >= 0) {
            current[existingIndex] = product
        } else {
            current.add(product)
        }
        preferences.setCachedProducts(current)

        val url = getWebAppUrl()
        if (url.isNotBlank() && !url.contains("offline-demo")) {
            return apiService.postProduct(url, product)
        }
        return Result.success("Saved to local products cache")
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
