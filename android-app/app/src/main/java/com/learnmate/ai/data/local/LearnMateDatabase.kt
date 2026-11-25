package com.learnmate.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.learnmate.ai.data.local.dao.AssessmentRecordDao
import com.learnmate.ai.data.local.dao.ConversationDao
import com.learnmate.ai.data.local.dao.LearnerProfileDao
import com.learnmate.ai.data.local.dao.RoadmapDao
import com.learnmate.ai.data.local.dao.SkillDao
import com.learnmate.ai.data.local.dao.StudyTaskDao
import com.learnmate.ai.data.local.entities.AssessmentRecordEntity
import com.learnmate.ai.data.local.entities.ChatMessageEntity
import com.learnmate.ai.data.local.entities.ConversationEntity
import com.learnmate.ai.data.local.entities.LearnerProfileEntity
import com.learnmate.ai.data.local.entities.RoadmapPhaseEntity
import com.learnmate.ai.data.local.entities.SkillEntity
import com.learnmate.ai.data.local.entities.StudyTaskEntity

@Database(
    entities = [
        LearnerProfileEntity::class,
        SkillEntity::class,
        RoadmapPhaseEntity::class,
        StudyTaskEntity::class,
        AssessmentRecordEntity::class,
        ConversationEntity::class,
        ChatMessageEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class LearnMateDatabase : RoomDatabase() {

    abstract fun learnerProfileDao(): LearnerProfileDao
    abstract fun skillDao(): SkillDao
    abstract fun roadmapDao(): RoadmapDao
    abstract fun studyTaskDao(): StudyTaskDao
    abstract fun assessmentRecordDao(): AssessmentRecordDao
    abstract fun conversationDao(): ConversationDao

    companion object {
        @Volatile
        private var INSTANCE: LearnMateDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Migrate learner_skills to composite PK (profileId, skillName)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `learner_skills_new` (
                        `skillName` TEXT NOT NULL,
                        `currentCompetency` INTEGER NOT NULL,
                        `targetCompetency` INTEGER NOT NULL,
                        `calculatedGap` INTEGER NOT NULL,
                        `isAssessed` INTEGER NOT NULL,
                        `priority` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `lastUpdatedTimestamp` INTEGER NOT NULL,
                        `profileId` TEXT NOT NULL,
                        PRIMARY KEY(`profileId`, `skillName`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO `learner_skills_new`
                    (`skillName`, `currentCompetency`, `targetCompetency`, `calculatedGap`, `isAssessed`, `priority`, `category`, `lastUpdatedTimestamp`, `profileId`)
                    SELECT `skillName`, `currentCompetency`, `targetCompetency`, `calculatedGap`, `isAssessed`, `priority`, `category`, `lastUpdatedTimestamp`, 'primary_learner'
                    FROM `learner_skills`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `learner_skills`")
                db.execSQL("ALTER TABLE `learner_skills_new` RENAME TO `learner_skills`")

                // 2. Migrate roadmap_phases to composite PK (profileId, id)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `roadmap_phases_new` (
                        `id` TEXT NOT NULL,
                        `phaseNumber` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `objective` TEXT NOT NULL,
                        `topicsCsv` TEXT NOT NULL,
                        `practicalProject` TEXT NOT NULL,
                        `learningOutcomesCsv` TEXT NOT NULL,
                        `estimatedWeeks` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `isCurrentActive` INTEGER NOT NULL,
                        `profileId` TEXT NOT NULL,
                        PRIMARY KEY(`profileId`, `id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO `roadmap_phases_new`
                    (`id`, `phaseNumber`, `title`, `objective`, `topicsCsv`, `practicalProject`, `learningOutcomesCsv`, `estimatedWeeks`, `isCompleted`, `isCurrentActive`, `profileId`)
                    SELECT `id`, `phaseNumber`, `title`, `objective`, `topicsCsv`, `practicalProject`, `learningOutcomesCsv`, `estimatedWeeks`, `isCompleted`, `isCurrentActive`, 'primary_learner'
                    FROM `roadmap_phases`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `roadmap_phases`")
                db.execSQL("ALTER TABLE `roadmap_phases_new` RENAME TO `roadmap_phases`")

                // 3. Migrate study_tasks to composite PK (profileId, id)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `study_tasks_new` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `relatedSkill` TEXT NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `scheduledDate` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `isRevisionTask` INTEGER NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        `profileId` TEXT NOT NULL,
                        PRIMARY KEY(`profileId`, `id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO `study_tasks_new`
                    (`id`, `title`, `relatedSkill`, `durationMinutes`, `scheduledDate`, `isCompleted`, `isRevisionTask`, `orderIndex`, `profileId`)
                    SELECT `id`, `title`, `relatedSkill`, `durationMinutes`, `scheduledDate`, `isCompleted`, `isRevisionTask`, `orderIndex`, 'primary_learner'
                    FROM `study_tasks`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `study_tasks`")
                db.execSQL("ALTER TABLE `study_tasks_new` RENAME TO `study_tasks`")

                // 4. Add profileId column to assessment_records cleanly (preserve rows)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `assessment_records_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `topicName` TEXT NOT NULL,
                        `relatedSkill` TEXT NOT NULL,
                        `scorePercentage` INTEGER NOT NULL,
                        `totalQuestions` INTEGER NOT NULL,
                        `correctQuestions` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `triggeredRemediation` INTEGER NOT NULL,
                        `profileId` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `assessment_records_new`
                    (`id`, `topicName`, `relatedSkill`, `scorePercentage`, `totalQuestions`, `correctQuestions`, `timestamp`, `triggeredRemediation`, `profileId`)
                    SELECT `id`, `topicName`, `relatedSkill`, `scorePercentage`, `totalQuestions`, `correctQuestions`, `timestamp`, `triggeredRemediation`, 'primary_learner'
                    FROM `assessment_records`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `assessment_records`")
                db.execSQL("ALTER TABLE `assessment_records_new` RENAME TO `assessment_records`")

                // 5. Create conversations and chat_messages tables
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `conversations` (
                        `id` TEXT NOT NULL,
                        `profileId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_conversations_profileId_updatedAt` ON `conversations` (`profileId`, `updatedAt`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chat_messages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `conversationId` TEXT NOT NULL,
                        `sender` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `isAi` INTEGER NOT NULL,
                        `runtimeSource` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        FOREIGN KEY(`conversationId`) REFERENCES `conversations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_chat_messages_conversationId_orderIndex` ON `chat_messages` (`conversationId`, `orderIndex`)"
                )
            }
        }

        fun getDatabase(context: Context): LearnMateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LearnMateDatabase::class.java,
                    "learnmate_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
