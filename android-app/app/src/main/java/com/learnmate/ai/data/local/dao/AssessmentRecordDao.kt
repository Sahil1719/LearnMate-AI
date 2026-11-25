package com.learnmate.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.learnmate.ai.data.local.entities.AssessmentRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentRecordDao {

    @Query("SELECT * FROM assessment_records WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getRecordsForProfileFlow(profileId: String): Flow<List<AssessmentRecordEntity>>

    @Query("SELECT * FROM assessment_records ORDER BY timestamp DESC")
    fun getAllRecordsFlow(): Flow<List<AssessmentRecordEntity>>

    @Query("SELECT * FROM assessment_records WHERE relatedSkill = :skillName ORDER BY timestamp DESC")
    fun getRecordsForSkillFlow(skillName: String): Flow<List<AssessmentRecordEntity>>

    @Query("SELECT * FROM assessment_records ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentRecords(limit: Int = 10): List<AssessmentRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AssessmentRecordEntity): Long

    @Query("DELETE FROM assessment_records WHERE profileId = :profileId")
    suspend fun clearRecordsForProfile(profileId: String)

    @Query("DELETE FROM assessment_records")
    suspend fun clearRecords()
}
