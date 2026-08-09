package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    // --- Medications ---
    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedicationById(id: Long): Medication?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    @Update
    suspend fun updateMedication(medication: Medication)

    @Delete
    suspend fun deleteMedication(medication: Medication)

    @Query("UPDATE medications SET isTakenToday = :taken, remainingTablets = CASE WHEN :taken = 1 AND remainingTablets > 0 THEN remainingTablets - 1 ELSE remainingTablets END, lastTakenTimestamp = :timestamp WHERE id = :id")
    suspend fun markMedicationTaken(id: Long, taken: Boolean, timestamp: Long)

    @Query("UPDATE medications SET remainingTablets = remainingTablets + :addedCount WHERE id = :id")
    suspend fun refillStock(id: Long, addedCount: Int)

    // --- Logs ---
    @Query("SELECT * FROM medication_logs ORDER BY id DESC")
    fun getAllLogs(): Flow<List<MedicationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MedicationLog)

    // --- Family Members ---
    @Query("SELECT * FROM family_members ORDER BY name ASC")
    fun getAllFamilyMembers(): Flow<List<FamilyMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMember(member: FamilyMember)

    @Delete
    suspend fun deleteFamilyMember(member: FamilyMember)

    // --- Drug Interactions ---
    @Query("SELECT * FROM drug_interactions ORDER BY id DESC")
    fun getAllInteractions(): Flow<List<DrugInteraction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: DrugInteraction)
    // --- Clear All User Data ---
    @Query("DELETE FROM medications")
    suspend fun deleteAllMedications()

    @Query("DELETE FROM medication_logs")
    suspend fun deleteAllLogs()

    @Query("DELETE FROM family_members")
    suspend fun deleteAllFamilyMembers()

    @Query("DELETE FROM drug_interactions")
    suspend fun deleteAllInteractions()
}
