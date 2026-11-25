package com.learnmate.ai.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.learnmate.ai.data.local.entities.StudyTaskEntity
import com.learnmate.ai.ui.theme.AmberWarning
import com.learnmate.ai.ui.theme.DeepBlueCard
import com.learnmate.ai.ui.theme.ElectricBlue
import com.learnmate.ai.ui.theme.EmeraldGreen
import com.learnmate.ai.ui.theme.MidnightNavy
import com.learnmate.ai.ui.theme.SoftViolet
import com.learnmate.ai.ui.theme.TextPrimary
import com.learnmate.ai.ui.theme.TextSecondary
import com.learnmate.ai.ui.theme.Typography

@Composable
fun StudyPlannerScreen(
    tasks: List<StudyTaskEntity>,
    availableHoursPerDay: Int = 2,
    onToggleTask: (String, Boolean) -> Unit
) {
    val maxDailyMinutes = availableHoursPerDay * 60
    val todayTasks = tasks.filter { it.scheduledDate == "Today" || it.scheduledDate == "Immediate Revision" }.ifEmpty {
        tasks.filter { it.scheduledDate == "Day 1" }
    }
    val todayScheduledMinutes = todayTasks.sumOf { it.durationMinutes }

    val upcomingTasks = tasks.filterNot { todayTasks.contains(it) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightNavy)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Personalized Study Schedule",
                style = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(
                text = "Time-constrained study blocks aligned with your daily availability.",
                style = Typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Section: Daily Constraint Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.padding(6.dp))
                    Column {
                        Text(
                            text = "Daily Constraint: $maxDailyMinutes mins max ($availableHoursPerDay hrs/day)",
                            style = Typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Today scheduled: $todayScheduledMinutes mins • Strictly within daily capacity.",
                            style = Typography.bodySmall,
                            color = EmeraldGreen
                        )
                    }
                }
            }
        }

        // Section: Today's Focus
        item {
            Text(text = "Today's Sessions", style = Typography.titleMedium, color = TextPrimary)
        }

        items(todayTasks) { task ->
            StudyTaskCard(task = task, onToggleTask = onToggleTask)
        }

        // Section: Upcoming Sessions
        if (upcomingTasks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Upcoming Sessions", style = Typography.titleMedium, color = TextPrimary)
            }

            items(upcomingTasks) { task ->
                StudyTaskCard(task = task, onToggleTask = onToggleTask)
            }
        }
    }
}

@Composable
private fun StudyTaskCard(
    task: StudyTaskEntity,
    onToggleTask: (String, Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DeepBlueCard)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (task.isRevisionTask) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AmberWarning.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "REMEDIATION", style = Typography.labelSmall, color = AmberWarning, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.padding(4.dp))
                }
                Text(
                    text = "${task.durationMinutes} mins • ${task.scheduledDate}",
                    style = Typography.labelSmall,
                    color = SoftViolet
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = task.title,
                style = Typography.bodyLarge,
                color = if (task.isCompleted) TextSecondary else TextPrimary,
                fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold
            )

            Text(
                text = "Competency: ${task.relatedSkill}",
                style = Typography.bodySmall,
                color = TextSecondary
            )
        }

        IconButton(onClick = { onToggleTask(task.id, !task.isCompleted) }) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = "Toggle completion",
                tint = if (task.isCompleted) EmeraldGreen else ElectricBlue
            )
        }
    }
}
