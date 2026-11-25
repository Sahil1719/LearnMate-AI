package com.learnmate.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.learnmate.ai.data.local.entities.StudyTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyTaskDao {
    @Query("SELECT * FROM study_tasks WHERE profileId = :profileId ORDER BY orderIndex ASC, scheduledDate ASC")
    fun getTasksForProfileFlow(profileId: String): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks ORDER BY orderIndex ASC, scheduledDate ASC")
    fun getAllTasksFlow(): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks WHERE scheduledDate = :date ORDER BY orderIndex ASC")
    fun getTasksForDateFlow(date: String): Flow<List<StudyTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<StudyTaskEntity>)

    @Query("UPDATE study_tasks SET isCompleted = :completed WHERE profileId = :profileId AND id = :taskId")
    suspend fun updateTaskCompletion(profileId: String, taskId: String, completed: Boolean)

    @Query("UPDATE study_tasks SET isCompleted = :completed WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: String, completed: Boolean)

    @Query("UPDATE study_tasks SET scheduledDate = :newDate WHERE profileId = :profileId AND id = :taskId")
    suspend fun rescheduleTask(profileId: String, taskId: String, newDate: String)

    @Query("UPDATE study_tasks SET scheduledDate = :newDate WHERE id = :taskId")
    suspend fun rescheduleTask(taskId: String, newDate: String)

    @Query("DELETE FROM study_tasks WHERE profileId = :profileId")
    suspend fun clearTasksForProfile(profileId: String)

    @Query("DELETE FROM study_tasks")
    suspend fun clearTasks()
}
