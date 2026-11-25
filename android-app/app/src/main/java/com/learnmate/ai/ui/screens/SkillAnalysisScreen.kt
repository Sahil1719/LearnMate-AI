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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.learnmate.ai.data.local.entities.SkillEntity
import com.learnmate.ai.ui.components.SkillGapBar
import com.learnmate.ai.ui.theme.AmberWarning
import com.learnmate.ai.ui.theme.DeepBlueBorder
import com.learnmate.ai.ui.theme.DeepBlueCard
import com.learnmate.ai.ui.theme.ElectricBlue
import com.learnmate.ai.ui.theme.EmeraldGreen
import com.learnmate.ai.ui.theme.MidnightNavy
import com.learnmate.ai.ui.theme.RoseError
import com.learnmate.ai.ui.theme.TextPrimary
import com.learnmate.ai.ui.theme.TextSecondary
import com.learnmate.ai.ui.theme.Typography

@Composable
fun SkillAnalysisScreen(
    skills: List<SkillEntity>,
    onUpdateSkill: (String, Int) -> Unit = { _, _ -> },
    onNavigateToQuiz: (String) -> Unit = {}
) {
    val totalGaps = skills.sumOf { it.calculatedGap }
    val criticalGaps = skills.count { it.calculatedGap >= 3 || !it.isAssessed }
    val unassessedCount = skills.count { !it.isAssessed }
    val masteredSkills = skills.count { it.isAssessed && it.calculatedGap == 0 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightNavy)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Skill Gap Analysis",
                style = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(
                text = "Dynamic prerequisite assessment for your personalized goal",
                style = Typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Summary Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricSummaryCard(
                    title = "Total Gap",
                    value = "$totalGaps Pts",
                    color = ElectricBlue,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Critical Gaps",
                    value = "$criticalGaps",
                    color = RoseError,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Mastered",
                    value = "$masteredSkills",
                    color = EmeraldGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (unassessedCount > 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ℹ $unassessedCount skills are currently unassessed. Tap below to rate your competency or take a diagnostic test.",
                            style = Typography.bodySmall,
                            color = AmberWarning
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Competencies for Your Goal",
                style = Typography.titleMedium,
                color = TextPrimary
            )
        }

        items(skills) { skill ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SkillGapBar(
                        skillName = skill.skillName,
                        currentScore = skill.currentCompetency,
                        targetScore = skill.targetCompetency
                    )

                    val actionText = when {
                        !skill.isAssessed -> "Unassessed: Diagnostic quiz recommended to establish baseline."
                        skill.calculatedGap == 0 -> "Competency achieved. Retain through advanced review."
                        skill.calculatedGap >= 3 -> "High Priority: Foundational bottleneck. Focus on initial modules."
                        else -> "Moderate Priority: Practice applied exercises to bridge gap."
                    }
                    Text(
                        text = actionText,
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick rating adjustment row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Update Level:", style = Typography.labelSmall, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (score in 0..5) {
                                val isSelected = (skill.currentCompetency == score)
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) ElectricBlue else MidnightNavy)
                                        .border(1.dp, if (isSelected) ElectricBlue else DeepBlueBorder, CircleShape)
                                        .clickable { onUpdateSkill(skill.skillName, score) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$score",
                                        style = Typography.labelSmall,
                                        color = if (isSelected) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    if (!skill.isAssessed || skill.calculatedGap > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { onNavigateToQuiz(skill.skillName) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarning),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp), tint = AmberWarning)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (!skill.isAssessed) "Take Baseline Diagnostic Quiz" else "Take Targeted Skill Quiz",
                                style = Typography.labelMedium,
                                color = AmberWarning
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricSummaryCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DeepBlueCard)
            .padding(14.dp)
    ) {
        Column {
            Text(text = title, style = Typography.labelSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = Typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
        }
    }
}
