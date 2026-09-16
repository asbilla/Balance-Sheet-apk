package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.AppointmentEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.AppointmentSettings
import com.example.data.pref.AppPreferences
import com.example.data.remote.RemoteAppointment
import com.example.data.remote.RemoteTransaction
import com.example.data.remote.SheetsApiService
import com.example.worker.SyncWorker
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class SyncResult(
    val transactionsPushed: Int = 0,
    val transactionsPulled: Int = 0,
    val appointmentsPushed: Int = 0,
    val appointmentsPulled: Int = 0,
    val message: String = ""
)

class TransactionRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    private val preferences: AppPreferences = AppPreferences.getInstance(context),
    private val apiService: SheetsApiService = SheetsApiService()
) {

    private val dao = database.transactionDao()
    private val appointmentDao = database.appointmentDao()

    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val unsyncedCount: Flow<Int> = dao.getUnsyncedCount()
    val unsyncedAppointmentCount: Flow<Int> = appointmentDao.getUnsyncedAppointmentCount()
    val businessProfile = preferences.businessProfileFlow
    val themeMode = preferences.themeModeFlow
    val appointmentSettings = preferences.appointmentSettingsFlow

    val allAppointments: Flow<List<AppointmentEntity>> = appointmentDao.getAllAppointments()

    fun getAppointmentsForDate(date: String): Flow<List<AppointmentEntity>> =
        appointmentDao.getAppointmentsForDate(date)

    fun getUpcomingAppointments(fromDate: String): Flow<List<AppointmentEntity>> =
        appointmentDao.getUpcomingAppointments(fromDate)

    fun getActiveAppointmentCountForDate(date: String): Flow<Int> =
        appointmentDao.getActiveAppointmentCountForDate(date)

    suspend fun saveAppointment(appointment: AppointmentEntity, syncToSheets: Boolean = true): Long {
        val id = appointmentDao.insertAppointment(appointment.copy(isSynced = false))
        if (syncToSheets) {
            val url = getWebAppUrl()
            if (url.isNotBlank() && !url.contains("offline-demo")) {
                val insertedEntity = appointment.copy(id = id)
                val res = apiService.postAppointment(url, insertedEntity)
                if (res.isSuccess) {
                    appointmentDao.markAsSyncedByIds(listOf(id))
                }
            }
        }
        return id
    }

    suspend fun updateAppointment(appointment: AppointmentEntity, syncToSheets: Boolean = true) {
        appointmentDao.updateAppointment(appointment.copy(isSynced = false))
        if (syncToSheets) {
            val url = getWebAppUrl()
            if (url.isNotBlank() && !url.contains("offline-demo")) {
                val res = apiService.postAppointment(url, appointment)
                if (res.isSuccess) {
                    appointmentDao.markAsSyncedByIds(listOf(appointment.id))
                }
            }
        }
    }

    suspend fun deleteAppointment(appointment: AppointmentEntity): Result<String> {
        appointmentDao.deleteAppointment(appointment)
        val url = getWebAppUrl()
        if (url.isNotBlank() && !url.contains("offline-demo")) {
            return apiService.deleteAppointmentOnSheets(url, appointment.uuid)
        }
        return Result.success("Deleted locally")
    }

    suspend fun deleteAppointmentById(id: Long): Result<String> {
        appointmentDao.deleteAppointmentById(id)
        return Result.success("Deleted locally")
    }

    suspend fun updateAppointmentStatus(id: Long, newStatus: String) {
        appointmentDao.updateStatus(id, newStatus)
        // Trigger background sync or direct sync
        val url = getWebAppUrl()
        if (url.isNotBlank() && !url.contains("offline-demo")) {
            // Find uuid and post update
            // Background sync will pick up or push on next sync
        }
    }

    fun getAppointmentSettings(): AppointmentSettings = preferences.getAppointmentSettings()

    fun setAppointmentSettings(settings: AppointmentSettings) = preferences.setAppointmentSettings(settings)

    fun generateTimeSlots(settings: AppointmentSettings = getAppointmentSettings()): List<String> =
        preferences.generateTimeSlots(settings)

    fun getThemeMode(): String = preferences.getThemeMode()
    fun setThemeMode(mode: String) = preferences.setThemeMode(mode)

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

    suspend fun syncBothWays(): Result<SyncResult> {
        val url = getWebAppUrl()
        if (url.isBlank() || url.contains("offline-demo")) {
            return Result.failure(IllegalStateException("Google Apps Script URL is not configured."))
        }

        return try {
            var txPushed = 0
            var txPulled = 0
            var apptPushed = 0
            var apptPulled = 0

            // 1. Push unsynced transactions using fast batch POST
            val unsyncedTx = dao.getUnsyncedTransactions()
            if (unsyncedTx.isNotEmpty()) {
                val pushRes = apiService.postTransactionsBatch(url, unsyncedTx)
                if (pushRes.isSuccess) {
                    dao.markAsSyncedByUuid(unsyncedTx.map { it.uuid })
                    txPushed = unsyncedTx.size
                } else {
                    // Fallback to individual
                    for (tx in unsyncedTx) {
                        val singleRes = apiService.postTransaction(url, tx)
                        if (singleRes.isSuccess) {
                            dao.markAsSyncedByUuid(listOf(tx.uuid))
                            txPushed++
                        }
                    }
                }
            }

            // 2. Push unsynced appointments using fast batch POST
            val unsyncedAppts = appointmentDao.getUnsyncedAppointments()
            if (unsyncedAppts.isNotEmpty()) {
                val pushApptRes = apiService.postAppointmentsBatch(url, unsyncedAppts)
                if (pushApptRes.isSuccess) {
                    appointmentDao.markAsSyncedByIds(unsyncedAppts.map { it.id })
                    apptPushed = unsyncedAppts.size
                } else {
                    for (appt in unsyncedAppts) {
                        val singleRes = apiService.postAppointment(url, appt)
                        if (singleRes.isSuccess) {
                            appointmentDao.markAsSyncedByIds(listOf(appt.id))
                            apptPushed++
                        }
                    }
                }
            }

            // 3. Pull transactions from spreadsheet & save any edits/new rows into local Room DB
            val fetchTxResult = apiService.fetchTransactions(url)
            if (fetchTxResult.isSuccess) {
                val remoteList = fetchTxResult.getOrNull().orEmpty()
                txPulled = syncRemoteTransactionsIntoLocal(remoteList)
            } else {
                return Result.failure(fetchTxResult.exceptionOrNull() ?: Exception("Failed fetching transactions"))
            }

            // 4. Pull appointments from spreadsheet & save any edits/new rows into local Room DB
            val fetchApptResult = apiService.fetchAppointments(url)
            if (fetchApptResult.isSuccess) {
                val remoteAppts = fetchApptResult.getOrNull().orEmpty()
                apptPulled = syncRemoteAppointmentsIntoLocal(remoteAppts)
            }

            val summaryMsg = "Two-way sync complete: $txPushed uploaded, local database up to date."
            Result.success(
                SyncResult(
                    transactionsPushed = txPushed,
                    transactionsPulled = txPulled,
                    appointmentsPushed = apptPushed,
                    appointmentsPulled = apptPulled,
                    message = summaryMsg
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchRemoteTransactions(): Result<List<RemoteTransaction>> {
        val syncRes = syncBothWays()
        val url = preferences.getWebAppUrl()
        return if (syncRes.isSuccess) {
            apiService.fetchTransactions(url)
        } else {
            Result.failure(syncRes.exceptionOrNull() ?: Exception("Sync failed"))
        }
    }

    private suspend fun syncRemoteTransactionsIntoLocal(remoteList: List<RemoteTransaction>): Int {
        if (remoteList.isEmpty()) return 0
        var updatedCount = 0

        val localList = dao.getAllTransactionsSync()
        val localMap = localList.filter { it.uuid.isNotEmpty() }.associateBy { it.uuid }

        for (remote in remoteList) {
            val existing = if (remote.id.isNotEmpty()) localMap[remote.id] else null
            if (existing != null) {
                // If the user edited data directly on the spreadsheet, reflect those edits in local Room DB!
                val amountDiff = Math.abs(existing.amount - remote.amount) > 0.001
                val notesDiff = existing.category != remote.notes
                val typeDiff = existing.type != remote.type
                val dateDiff = existing.date != remote.date

                if (amountDiff || notesDiff || typeDiff || dateDiff || !existing.isSynced) {
                    val updated = existing.copy(
                        date = remote.date,
                        type = remote.type,
                        category = remote.notes,
                        amount = remote.amount,
                        isSynced = true
                    )
                    dao.updateTransaction(updated)
                    updatedCount++
                }
            } else {
                // Brand new transaction entered directly into Google Sheets!
                val newEntity = TransactionEntity(
                    uuid = if (remote.id.isNotEmpty()) remote.id else UUID.randomUUID().toString(),
                    date = remote.date,
                    type = remote.type,
                    category = remote.notes,
                    amount = remote.amount,
                    timestamp = System.currentTimeMillis(),
                    isSynced = true
                )
                dao.insertTransaction(newEntity)
                updatedCount++
            }
        }
        return updatedCount
    }

    private suspend fun syncRemoteAppointmentsIntoLocal(remoteAppts: List<RemoteAppointment>): Int {
        if (remoteAppts.isEmpty()) return 0
        var updatedCount = 0

        for (remote in remoteAppts) {
            var local: AppointmentEntity? = if (remote.id.isNotEmpty()) {
                appointmentDao.getAppointmentByUuid(remote.id)
            } else null

            if (local == null && remote.phone.isNotEmpty() && remote.date.isNotEmpty()) {
                local = appointmentDao.findAppointment(remote.phone, remote.date, remote.time)
            }

            if (local != null) {
                val statusDiff = local.status != remote.status
                val nameDiff = local.customerName != remote.customerName
                val serviceDiff = local.serviceName != remote.service
                val notesDiff = local.notes != remote.notes
                val priceDiff = Math.abs(local.price - remote.price) > 0.001

                if (statusDiff || nameDiff || serviceDiff || notesDiff || priceDiff || !local.isSynced) {
                    val updated = local.copy(
                        customerName = if (remote.customerName.isNotBlank()) remote.customerName else local.customerName,
                        serviceName = if (remote.service.isNotBlank()) remote.service else local.serviceName,
                        status = remote.status,
                        notes = remote.notes,
                        price = if (remote.price > 0) remote.price else local.price,
                        isSynced = true
                    )
                    appointmentDao.updateAppointment(updated)
                    updatedCount++
                }
            } else {
                val newAppt = AppointmentEntity(
                    uuid = if (remote.id.isNotEmpty()) remote.id else UUID.randomUUID().toString(),
                    customerName = remote.customerName,
                    customerPhone = remote.phone,
                    serviceName = remote.service,
                    appointmentDate = remote.date,
                    appointmentTime = remote.time,
                    durationMinutes = remote.duration,
                    price = remote.price,
                    status = remote.status,
                    notes = remote.notes,
                    isSynced = true
                )
                appointmentDao.insertAppointment(newAppt)
                updatedCount++
            }
        }
        return updatedCount
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
