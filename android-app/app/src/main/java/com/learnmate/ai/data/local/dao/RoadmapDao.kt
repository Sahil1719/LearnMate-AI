package com.learnmate.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.learnmate.ai.data.local.entities.RoadmapPhaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoadmapDao {
    @Query("SELECT * FROM roadmap_phases WHERE profileId = :profileId ORDER BY phaseNumber ASC")
    fun getPhasesForProfileFlow(profileId: String): Flow<List<RoadmapPhaseEntity>>

    @Query("SELECT * FROM roadmap_phases ORDER BY phaseNumber ASC")
    fun getAllPhasesFlow(): Flow<List<RoadmapPhaseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhases(phases: List<RoadmapPhaseEntity>)

    @Query("UPDATE roadmap_phases SET isCompleted = :completed WHERE profileId = :profileId AND id = :phaseId")
    suspend fun setPhaseCompleted(profileId: String, phaseId: String, completed: Boolean)

    @Query("UPDATE roadmap_phases SET isCompleted = :completed WHERE id = :phaseId")
    suspend fun setPhaseCompleted(phaseId: String, completed: Boolean)

    @Query("UPDATE roadmap_phases SET isCurrentActive = :active WHERE profileId = :profileId AND id = :phaseId")
    suspend fun setPhaseActive(profileId: String, phaseId: String, active: Boolean)

    @Query("UPDATE roadmap_phases SET isCurrentActive = :active WHERE id = :phaseId")
    suspend fun setPhaseActive(phaseId: String, active: Boolean)

    @Query("DELETE FROM roadmap_phases WHERE profileId = :profileId")
    suspend fun clearPhasesForProfile(profileId: String)

    @Query("DELETE FROM roadmap_phases")
    suspend fun clearPhases()
}
