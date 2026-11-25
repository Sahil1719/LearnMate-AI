package com.learnmate.ai.domain.quiz

data class QuizQuestion(
    val id: String,
    val domain: String,
    val topic: String,
    val relatedSkill: String,
    val questionText: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String
)

data class QuizSet(
    val id: String,
    val topicName: String,
    val relatedSkill: String,
    val domain: String,
    val description: String,
    val questions: List<QuizQuestion>
)

data class QuizSummary(
    val quizSet: QuizSet,
    val totalQuestions: Int,
    val correctCount: Int,
    val scorePercentage: Int,
    val passed: Boolean,
    val triggeredRemediation: Boolean
)
