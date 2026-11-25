package com.learnmate.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.learnmate.ai.data.local.entities.LearnerProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LearnerProfileDao {
    @Query("SELECT * FROM learner_profile ORDER BY createdAtTimestamp ASC")
    fun getAllProfilesFlow(): Flow<List<LearnerProfileEntity>>

    @Query("SELECT * FROM learner_profile ORDER BY createdAtTimestamp ASC")
    suspend fun getAllProfiles(): List<LearnerProfileEntity>

    @Query("SELECT * FROM learner_profile WHERE id = :id LIMIT 1")
    fun getProfileFlow(id: String = "primary_learner"): Flow<LearnerProfileEntity?>

    @Query("SELECT * FROM learner_profile WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: String = "primary_learner"): LearnerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: LearnerProfileEntity)

    @Query("UPDATE learner_profile SET learningStreak = learningStreak + 1 WHERE id = :id")
    suspend fun incrementStreak(id: String = "primary_learner")

    @Query("UPDATE learner_profile SET weeklyGoalProgressPercent = :progress WHERE id = :id")
    suspend fun updateWeeklyProgress(progress: Int, id: String = "primary_learner")

    @Query("DELETE FROM learner_profile WHERE id = :id")
    suspend fun deleteProfile(id: String = "primary_learner")
}
