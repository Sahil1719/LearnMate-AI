package com.learnmate.ai.domain

import com.learnmate.ai.data.local.entities.LearnerProfileEntity
import com.learnmate.ai.domain.engine.GoalBasedLearningEngine
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scenario G Acceptance Test:
 * Available time: 2 hours per day.
 * Expected output: Total scheduled learning time MUST NOT exceed 120 minutes on ANY day.
 */
class ScenarioG_TimeConstraintTest {

    @Test
    fun testTwoHoursPerDayNeverExceeds120MinutesOnAnyDay() {
        val availableHours = 2
        val maxDailyMinutes = availableHours * 60 // 120 minutes

        val profile = LearnerProfileEntity(
            id = "primary_learner",
            name = "Test Learner",
            learnerType = "Undergraduate student",
            educationLevel = "Undergraduate (Bachelor's)",
            educationStream = "Science",
            targetDomain = "Science",
            targetGoalTitle = "Quantum Computing Basics",
            availableHoursPerDay = availableHours,
            preferredSessionDurationMinutes = 45
        )

        val pathway = GoalBasedLearningEngine.generatePathway(profile, emptyMap())
        val tasks = pathway.initialDailyTasks

        // Group tasks by scheduled date / day
        val tasksByDay = tasks.groupBy { it.scheduledDate }

        for ((day, dayTasks) in tasksByDay) {
            val totalMinutes = dayTasks.sumOf { it.durationMinutes }
            assertTrue(
                "Day '$day' scheduled $totalMinutes minutes, exceeding strict constraint of $maxDailyMinutes minutes!",
                totalMinutes <= maxDailyMinutes
            )
        }
    }

    @Test
    fun testOneHourPerDayNeverExceeds60MinutesOnAnyDay() {
        val availableHours = 1
        val maxDailyMinutes = 60

        val profile = LearnerProfileEntity(
            id = "primary_learner",
            name = "Busy Professional",
            learnerType = "Working professional",
            educationLevel = "Master's",
            educationStream = "Commerce",
            targetDomain = "Commerce",
            targetGoalTitle = "Financial Statement Analysis",
            availableHoursPerDay = availableHours,
            preferredSessionDurationMinutes = 30
        )

        val pathway = GoalBasedLearningEngine.generatePathway(profile, emptyMap())
        val tasks = pathway.initialDailyTasks

        val tasksByDay = tasks.groupBy { it.scheduledDate }

        for ((day, dayTasks) in tasksByDay) {
            val totalMinutes = dayTasks.sumOf { it.durationMinutes }
            assertTrue(
                "Day '$day' scheduled $totalMinutes minutes, exceeding 60 minutes limit!",
                totalMinutes <= maxDailyMinutes
            )
        }
    }
}
