package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.AppointmentEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.AppointmentSettings
import com.example.data.model.ProductItem
import com.example.data.pref.AppPreferences
import com.example.data.pref.BusinessProfile
import com.example.data.remote.RemoteAppointment
import com.example.data.remote.RemoteTransaction
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
    private val preferences: AppPreferences = AppPreferences.getInstance(context)
) {

    private val dao = database.transactionDao()
    private val appointmentDao = database.appointmentDao()

    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val unsyncedCount: Flow<Int> = dao.getUnsyncedCount()
    val unsyncedAppointmentCount: Flow<Int> = appointmentDao.getUnsyncedAppointmentCount()
    val businessProfile = preferences.businessProfileFlow
    val themeMode = preferences.themeModeFlow
    val appointmentSettings = preferences.appointmentSettingsFlow
    val products = preferences.productsFlow

    val allAppointments: Flow<List<AppointmentEntity>> = appointmentDao.getAllAppointments()

    fun getAppointmentsForDate(date: String): Flow<List<AppointmentEntity>> =
        appointmentDao.getAppointmentsForDate(date)

    fun getUpcomingAppointments(fromDate: String): Flow<List<AppointmentEntity>> =
        appointmentDao.getUpcomingAppointments(fromDate)

    fun getActiveAppointmentCountForDate(date: String): Flow<Int> =
        appointmentDao.getActiveAppointmentCountForDate(date)

    suspend fun saveAppointment(appointment: AppointmentEntity, @Suppress("UNUSED_PARAMETER") syncToSheets: Boolean = false): Long {
        return appointmentDao.insertAppointment(appointment.copy(isSynced = true))
    }

    suspend fun updateAppointment(appointment: AppointmentEntity, @Suppress("UNUSED_PARAMETER") syncToSheets: Boolean = false) {
        appointmentDao.updateAppointment(appointment.copy(isSynced = true))
    }

    suspend fun deleteAppointment(appointment: AppointmentEntity): Result<String> {
        appointmentDao.deleteAppointment(appointment)
        return Result.success("Deleted from local storage")
    }

    suspend fun deleteAppointmentById(id: Long): Result<String> {
        appointmentDao.deleteAppointmentById(id)
        return Result.success("Deleted from local storage")
    }

    suspend fun updateAppointmentStatus(id: Long, newStatus: String) {
        appointmentDao.updateStatus(id, newStatus)
    }

    fun getAppointmentSettings(): AppointmentSettings = preferences.getAppointmentSettings()

    fun setAppointmentSettings(settings: AppointmentSettings) = preferences.setAppointmentSettings(settings)

    fun generateTimeSlots(settings: AppointmentSettings = getAppointmentSettings()): List<String> =
        preferences.generateTimeSlots(settings)

    fun getThemeMode(): String = preferences.getThemeMode()
    fun setThemeMode(mode: String) = preferences.setThemeMode(mode)

    fun getBusinessProfile(): BusinessProfile = preferences.getBusinessProfile()

    suspend fun saveBusinessProfile(
        profile: BusinessProfile,
        @Suppress("UNUSED_PARAMETER") syncToSheets: Boolean = false
    ): Result<String> {
        preferences.setBusinessProfile(profile)
        return Result.success("Business details saved locally")
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
            isSynced = true
        )
        return dao.insertTransaction(entity)
    }

    fun triggerBackgroundSync() {
        // No-op in local storage mode
    }

    suspend fun syncBothWays(): Result<SyncResult> {
        return Result.success(SyncResult(message = "Local storage is active"))
    }

    suspend fun fetchRemoteTransactions(): Result<List<RemoteTransaction>> {
        return Result.success(emptyList())
    }

    suspend fun fetchAppointments(): Result<List<RemoteAppointment>> {
        return Result.success(emptyList())
    }

    suspend fun testConnection(@Suppress("UNUSED_PARAMETER") url: String): Result<String> {
        return Result.success("Local storage active")
    }

    suspend fun fetchPdfExportUrl(): Result<String> {
        return Result.failure(Exception("PDF export generated locally on device"))
    }

    fun getWebAppUrl(): String = ""

    fun setWebAppUrl(@Suppress("UNUSED_PARAMETER") url: String) {
        // No-op
    }

    fun isConfigured(): Boolean = true

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
        val existing = dao.getTransactionByUuid(id)
        if (existing != null) {
            val updated = existing.copy(
                amount = newAmount,
                category = category.ifBlank { existing.category },
                isSynced = true
            )
            dao.updateTransaction(updated)
        } else {
            val byDateAndType = dao.getTransactionsByDateAndType(date, type).firstOrNull()
            if (byDateAndType != null) {
                val updated = byDateAndType.copy(
                    amount = newAmount,
                    category = category.ifBlank { byDateAndType.category },
                    isSynced = true
                )
                dao.updateTransaction(updated)
            } else {
                val assignedUuid = if (id.isNotBlank()) id else UUID.randomUUID().toString()
                val newEntity = TransactionEntity(
                    uuid = assignedUuid,
                    date = date,
                    timestamp = System.currentTimeMillis(),
                    type = type,
                    category = category,
                    amount = newAmount,
                    isSynced = true
                )
                dao.insertTransaction(newEntity)
            }
        }
        return Result.success("Amount updated in local storage")
    }

    suspend fun deleteTransactionPermanently(
        id: String,
        date: String,
        type: String
    ): Result<String> {
        if (id.isNotBlank()) {
            dao.deleteByUuid(id)
            val parsedId = id.toLongOrNull()
            if (parsedId != null) {
                dao.deleteById(parsedId)
            }
        } else if (date.isNotBlank() && type.isNotBlank()) {
            dao.deleteByDateAndType(date, type)
        }
        return Result.success("Transaction deleted from local storage.")
    }

    fun getCachedProducts(): List<ProductItem> = preferences.getCachedProducts()

    suspend fun getProducts(@Suppress("UNUSED_PARAMETER") forceRefresh: Boolean = false): List<ProductItem> {
        return preferences.getCachedProducts()
    }

    suspend fun refreshProductsFromSheets(): Result<List<ProductItem>> {
        return Result.success(preferences.getCachedProducts())
    }

    suspend fun syncProductsToSheets(@Suppress("UNUSED_PARAMETER") products: List<ProductItem>) {
        // No-op in local storage mode
    }

    suspend fun saveProduct(product: ProductItem): Result<String> {
        preferences.saveProduct(product)
        return Result.success("Saved to local menu items")
    }

    fun deleteProduct(productName: String): Result<String> {
        preferences.deleteProduct(productName)
        return Result.success("Product deleted from local storage")
    }

    fun resetDefaultProducts(): Result<String> {
        preferences.resetDefaultProducts()
        return Result.success("Reset to default menu items")
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
