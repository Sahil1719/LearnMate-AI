package com.learnmate.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learnmate.ai.data.local.entities.AssessmentRecordEntity
import com.learnmate.ai.data.local.entities.LearnerProfileEntity
import com.learnmate.ai.data.local.entities.SkillEntity
import com.learnmate.ai.data.repository.LearningRepository
import com.learnmate.ai.domain.quiz.OfflineQuizBank
import com.learnmate.ai.domain.quiz.QuizQuestion
import com.learnmate.ai.domain.quiz.QuizSet
import com.learnmate.ai.ui.theme.AmberWarning
import com.learnmate.ai.ui.theme.DeepBlueBorder
import com.learnmate.ai.ui.theme.DeepBlueCard
import com.learnmate.ai.ui.theme.ElectricBlue
import com.learnmate.ai.ui.theme.EmeraldGreen
import com.learnmate.ai.ui.theme.MidnightNavy
import com.learnmate.ai.ui.theme.RoseError
import com.learnmate.ai.ui.theme.SoftViolet
import com.learnmate.ai.ui.theme.TextPrimary
import com.learnmate.ai.ui.theme.TextSecondary
import com.learnmate.ai.ui.theme.Typography
import kotlinx.coroutines.launch

@Composable
fun QuizScreen(
    profile: LearnerProfileEntity?,
    skills: List<SkillEntity>,
    repository: LearningRepository,
    initialTopic: String? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateToPlan: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val pastAssessments by repository.assessmentRecordsFlow.collectAsState(initial = emptyList())

    // Determine initial active quiz
    val activeQuiz = remember(initialTopic, profile?.targetDomain) {
        if (!initialTopic.isNullOrBlank()) {
            OfflineQuizBank.getQuizForSkill(initialTopic)
                ?: OfflineQuizBank.getQuizById(initialTopic)
                ?: OfflineQuizBank.getRecommendedQuiz(
                    targetDomain = profile?.targetDomain ?: "Engineering",
                    activeSkills = skills.map { it.skillName }
                )
        } else {
            OfflineQuizBank.getRecommendedQuiz(
                targetDomain = profile?.targetDomain ?: "Engineering",
                activeSkills = skills.map { it.skillName }
            )
        }
    }

    var selectedQuizSet by remember { mutableStateOf(activeQuiz) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var isQuizCompleted by remember { mutableStateOf(false) }

    // Track user answers for the current session
    val userAnswers = remember { mutableStateListOf<Int>() }
    var recordedSummary by remember { mutableStateOf<AssessmentRecordEntity?>(null) }

    val allQuizzes = remember { OfflineQuizBank.getAllQuizzes() }
    val questions = selectedQuizSet.questions
    val currentQuestion = questions.getOrNull(currentQuestionIndex)

    fun resetQuiz(newSet: QuizSet) {
        selectedQuizSet = newSet
        currentQuestionIndex = 0
        selectedOptionIndex = null
        isSubmitted = false
        isQuizCompleted = false
        userAnswers.clear()
        recordedSummary = null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightNavy)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header Bar ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DeepBlueCard)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Knowledge Assessment",
                        style = Typography.headlineSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Interactive Diagnostic & Remediation Engine",
                        style = Typography.bodySmall,
                        color = ElectricBlue
                    )
                }
            }
        }

        // --- Quiz Topic Selector Pills ---
        item {
            Text(
                text = "Select Assessment Track",
                style = Typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allQuizzes) { quiz ->
                    val isSelected = quiz.id == selectedQuizSet.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) ElectricBlue else DeepBlueCard)
                            .clickable {
                                if (!isSelected) resetQuiz(quiz)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = quiz.relatedSkill,
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) MidnightNavy else TextPrimary
                        )
                    }
                }
            }
        }

        // --- Main Quiz Card / Content ---
        if (!isQuizCompleted && currentQuestion != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // Progress bar & Counter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question ${currentQuestionIndex + 1} of ${questions.size}",
                                style = Typography.labelMedium,
                                color = SoftViolet,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selectedQuizSet.topicName,
                                style = Typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { (currentQuestionIndex + 1).toFloat() / questions.size },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = ElectricBlue,
                            trackColor = MidnightNavy
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Question Prompt
                        Text(
                            text = currentQuestion.questionText,
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Options
                        currentQuestion.options.forEachIndexed { index, optionText ->
                            val isChosen = selectedOptionIndex == index
                            val isCorrectAnswer = index == currentQuestion.correctOptionIndex

                            val backgroundColor = when {
                                !isSubmitted && isChosen -> ElectricBlue.copy(alpha = 0.2f)
                                isSubmitted && isCorrectAnswer -> EmeraldGreen.copy(alpha = 0.25f)
                                isSubmitted && isChosen && !isCorrectAnswer -> RoseError.copy(alpha = 0.25f)
                                else -> MidnightNavy
                            }

                            val borderColor = when {
                                !isSubmitted && isChosen -> ElectricBlue
                                isSubmitted && isCorrectAnswer -> EmeraldGreen
                                isSubmitted && isChosen && !isCorrectAnswer -> RoseError
                                else -> DeepBlueBorder
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(backgroundColor)
                                    .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isSubmitted) {
                                        selectedOptionIndex = index
                                    }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Option index marker (A, B, C, D)
                                    val optionMarker = ('A' + index).toString()
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSubmitted && isCorrectAnswer -> EmeraldGreen
                                                    isSubmitted && isChosen && !isCorrectAnswer -> RoseError
                                                    isChosen -> ElectricBlue
                                                    else -> DeepBlueCard
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = optionMarker,
                                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isChosen || (isSubmitted && isCorrectAnswer)) MidnightNavy else TextSecondary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = optionText,
                                        style = Typography.bodyMedium,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isSubmitted) {
                                        if (isCorrectAnswer) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Correct",
                                                tint = EmeraldGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else if (isChosen) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Incorrect",
                                                tint = RoseError,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Explanation card shown after submission
                        if (isSubmitted) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MidnightNavy),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = if (selectedOptionIndex == currentQuestion.correctOptionIndex) EmeraldGreen else AmberWarning,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (selectedOptionIndex == currentQuestion.correctOptionIndex) "Correct! Explanation:" else "Explanation:",
                                            style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (selectedOptionIndex == currentQuestion.correctOptionIndex) EmeraldGreen else AmberWarning
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = currentQuestion.explanation,
                                        style = Typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Action Button (Submit or Next)
                        if (!isSubmitted) {
                            Button(
                                onClick = {
                                    if (selectedOptionIndex != null) {
                                        isSubmitted = true
                                        userAnswers.add(selectedOptionIndex!!)
                                    }
                                },
                                enabled = selectedOptionIndex != null,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Confirm Answer",
                                    color = MidnightNavy,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            val isLastQuestion = currentQuestionIndex == questions.size - 1
                            Button(
                                onClick = {
                                    if (isLastQuestion) {
                                        // Calculate score and persist to repository
                                        val total = questions.size
                                        var correct = 0
                                        questions.forEachIndexed { idx, q ->
                                            if (idx < userAnswers.size && userAnswers[idx] == q.correctOptionIndex) {
                                                correct++
                                            }
                                        }
                                        coroutineScope.launch {
                                            val summary = repository.recordQuizResult(
                                                topicName = selectedQuizSet.topicName,
                                                relatedSkill = selectedQuizSet.relatedSkill,
                                                totalQuestions = total,
                                                correctQuestions = correct
                                            )
                                            recordedSummary = summary
                                            isQuizCompleted = true
                                        }
                                    } else {
                                        currentQuestionIndex++
                                        selectedOptionIndex = null
                                        isSubmitted = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isLastQuestion) EmeraldGreen else ElectricBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isLastQuestion) "Finish & View Results" else "Next Question",
                                    color = MidnightNavy,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Results Screen (When Quiz Completed) ---
        if (isQuizCompleted) {
            val total = questions.size
            var correct = 0
            questions.forEachIndexed { idx, q ->
                if (idx < userAnswers.size && userAnswers[idx] == q.correctOptionIndex) {
                    correct++
                }
            }
            val percentage = if (total > 0) (correct * 100) / total else 0
            val passed = percentage >= 60

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Badge icon
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (passed) EmeraldGreen.copy(alpha = 0.2f) else AmberWarning.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (passed) EmeraldGreen else AmberWarning,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (passed) "Assessment Passed!" else "Skill Gap Detected",
                            style = Typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (passed) EmeraldGreen else AmberWarning
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${selectedQuizSet.topicName} • ${selectedQuizSet.relatedSkill}",
                            style = Typography.bodyMedium,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Score display
                        Text(
                            text = "$percentage%",
                            style = Typography.headlineLarge.copy(fontSize = 44.sp, fontWeight = FontWeight.ExtraBold),
                            color = if (passed) EmeraldGreen else AmberWarning
                        )
                        Text(
                            text = "$correct out of $total questions correct",
                            style = Typography.bodyMedium,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Remediation status banner
                        if (!passed) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = AmberWarning,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Adaptive Remediation Triggered",
                                            style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = AmberWarning
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Because score fell below 60%, a targeted 45-minute revision task has been scheduled for Today in your Study Planner to reinforce foundational concepts.",
                                        style = Typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Competency Validated",
                                            style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = EmeraldGreen
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Solid understanding demonstrated. Your skill competency rating has been validated and recorded offline in your local profile.",
                                        style = Typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { resetQuiz(selectedQuizSet) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Replay, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retake", color = ElectricBlue)
                            }

                            Button(
                                onClick = onNavigateToPlan,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("View Plan", color = MidnightNavy, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- Past Assessment History ---
        if (pastAssessments.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Diagnostic History",
                    style = Typography.titleMedium,
                    color = TextPrimary
                )
            }

            items(pastAssessments.take(5)) { record ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.topicName,
                                style = Typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${record.relatedSkill} • ${record.correctQuestions}/${record.totalQuestions} correct",
                                style = Typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${record.scorePercentage}%",
                                style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (record.scorePercentage >= 60) EmeraldGreen else AmberWarning
                            )
                            if (record.triggeredRemediation) {
                                Text(
                                    text = "Remediation",
                                    style = Typography.labelSmall,
                                    color = AmberWarning
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
