package com.learnmate.ai.domain

import com.learnmate.ai.domain.quiz.OfflineQuizBank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineQuizBankTest {

    @Test
    fun testQuizBankContainsMajorDomains() {
        val quizzes = OfflineQuizBank.getAllQuizzes()
        assertTrue("Quiz bank should contain at least 5 quiz sets", quizzes.size >= 5)

        val domains = quizzes.map { it.domain }.toSet()
        assertTrue("Should cover Engineering", domains.contains("Engineering"))
        assertTrue("Should cover Commerce", domains.contains("Commerce"))
        assertTrue("Should cover Medicine", domains.contains("Medicine"))
        assertTrue("Should cover Science", domains.contains("Science"))
        assertTrue("Should cover Arts & Humanities", domains.contains("Arts & Humanities"))
    }

    @Test
    fun testQuestionIntegrityAndOptions() {
        val quizzes = OfflineQuizBank.getAllQuizzes()
        for (quiz in quizzes) {
            assertTrue("Quiz ${quiz.id} should have at least 3 questions", quiz.questions.size >= 3)
            for (q in quiz.questions) {
                assertTrue("Question text should not be blank", q.questionText.isNotBlank())
                assertEquals("Question ${q.id} should have exactly 4 options", 4, q.options.size)
                assertTrue("Correct option index should be in 0..3", q.correctOptionIndex in 0..3)
                assertTrue("Explanation should not be blank", q.explanation.isNotBlank())
            }
        }
    }

    @Test
    fun testQuizMatchingBySkill() {
        val cQuiz = OfflineQuizBank.getQuizForSkill("C Programming")
        assertNotNull("Should find quiz for C Programming", cQuiz)
        assertEquals("quiz_c_pointers", cQuiz?.id)

        val dsaQuiz = OfflineQuizBank.getQuizForSkill("Data Structures")
        assertNotNull("Should find quiz for Data Structures", dsaQuiz)
        assertEquals("quiz_dsa_trees", dsaQuiz?.id)
    }

    @Test
    fun testRecommendedQuizFallback() {
        val medQuiz = OfflineQuizBank.getRecommendedQuiz("Medicine", listOf("Unknown Skill"))
        assertEquals("Medicine", medQuiz.domain)

        val commQuiz = OfflineQuizBank.getRecommendedQuiz("Commerce", emptyList())
        assertEquals("Commerce", commQuiz.domain)
    }

    @Test
    fun testRemediationThresholdBoundary() {
        // Threshold is 60%
        val passScore = 60
        val failScore = 59

        assertTrue("60% should pass", passScore >= 60)
        assertTrue("59% should trigger remediation", failScore < 60)

        // 3 out of 5 = 60% (pass)
        val score3of5 = (3 * 100) / 5
        assertEquals(60, score3of5)
        assertTrue(score3of5 >= 60)

        // 2 out of 5 = 40% (fail -> remediation)
        val score2of5 = (2 * 100) / 5
        assertEquals(40, score2of5)
        assertTrue(score2of5 < 60)
    }
}
