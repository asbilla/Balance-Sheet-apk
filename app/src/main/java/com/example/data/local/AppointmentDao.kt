package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {

    @Query("SELECT * FROM appointments ORDER BY appointmentDate ASC, appointmentTime ASC")
    fun getAllAppointments(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE appointmentDate = :date ORDER BY appointmentTime ASC")
    fun getAppointmentsForDate(date: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE appointmentDate >= :fromDate ORDER BY appointmentDate ASC, appointmentTime ASC")
    fun getUpcomingAppointments(fromDate: String): Flow<List<AppointmentEntity>>

    @Query("SELECT COUNT(*) FROM appointments WHERE appointmentDate = :date AND status != 'Cancelled'")
    fun getActiveAppointmentCountForDate(date: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity): Long

    @Update
    suspend fun updateAppointment(appointment: AppointmentEntity)

    @Delete
    suspend fun deleteAppointment(appointment: AppointmentEntity)

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteAppointmentById(id: Long)

    @Query("UPDATE appointments SET status = :newStatus WHERE id = :id")
    suspend fun updateStatus(id: Long, newStatus: String)
}
