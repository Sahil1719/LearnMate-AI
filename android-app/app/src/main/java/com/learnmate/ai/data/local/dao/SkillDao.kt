package com.learnmate.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.learnmate.ai.data.local.entities.SkillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillDao {
    @Query("SELECT * FROM learner_skills WHERE profileId = :profileId ORDER BY calculatedGap DESC, skillName ASC")
    fun getSkillsForProfileFlow(profileId: String): Flow<List<SkillEntity>>

    @Query("SELECT * FROM learner_skills WHERE profileId = :profileId ORDER BY calculatedGap DESC, skillName ASC")
    suspend fun getSkillsForProfile(profileId: String): List<SkillEntity>

    @Query("SELECT * FROM learner_skills ORDER BY calculatedGap DESC, skillName ASC")
    fun getAllSkillsFlow(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM learner_skills ORDER BY calculatedGap DESC, skillName ASC")
    suspend fun getAllSkills(): List<SkillEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSkills(skills: List<SkillEntity>)

    @Query("UPDATE learner_skills SET currentCompetency = :newScore, calculatedGap = max(0, targetCompetency - :newScore), isAssessed = 1, lastUpdatedTimestamp = :timestamp WHERE profileId = :profileId AND skillName = :skillName")
    suspend fun updateSkillCompetency(profileId: String, skillName: String, newScore: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE learner_skills SET currentCompetency = :newScore, calculatedGap = max(0, targetCompetency - :newScore), isAssessed = 1, lastUpdatedTimestamp = :timestamp WHERE skillName = :skillName")
    suspend fun updateSkillCompetency(skillName: String, newScore: Int, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM learner_skills WHERE profileId = :profileId")
    suspend fun clearSkillsForProfile(profileId: String)

    @Query("DELETE FROM learner_skills")
    suspend fun clearSkills()
}
