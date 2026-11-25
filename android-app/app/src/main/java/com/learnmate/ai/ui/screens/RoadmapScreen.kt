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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learnmate.ai.data.local.entities.RoadmapPhaseEntity
import com.learnmate.ai.ui.theme.DeepBlueBorder
import com.learnmate.ai.ui.theme.DeepBlueCard
import com.learnmate.ai.ui.theme.ElectricBlue
import com.learnmate.ai.ui.theme.EmeraldGreen
import com.learnmate.ai.ui.theme.MidnightNavy
import com.learnmate.ai.ui.theme.SoftViolet
import com.learnmate.ai.ui.theme.TextPrimary
import com.learnmate.ai.ui.theme.TextSecondary
import com.learnmate.ai.ui.theme.TextTertiary
import com.learnmate.ai.ui.theme.Typography

@Composable
fun RoadmapScreen(
    phases: List<RoadmapPhaseEntity>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightNavy)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Learning Roadmap",
                style = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(
                text = "Sequential progression tailored to your target goal and identified gaps.",
                style = Typography.bodyMedium,
                color = TextSecondary
            )
        }

        items(phases) { phase ->
            RoadmapPhaseCard(phase = phase)
        }
    }
}

@Composable
fun RoadmapPhaseCard(phase: RoadmapPhaseEntity) {
    val statusColor = when {
        phase.isCompleted -> EmeraldGreen
        phase.isCurrentActive -> ElectricBlue
        else -> TextTertiary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            phase.isCompleted -> Icon(Icons.Default.Check, contentDescription = "Completed", tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                            phase.isCurrentActive -> Icon(Icons.Default.PlayArrow, contentDescription = "Active", tint = ElectricBlue, modifier = Modifier.size(18.dp))
                            else -> Icon(Icons.Default.Lock, contentDescription = "Locked", tint = TextTertiary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Phase ${phase.phaseNumber} • ${phase.estimatedWeeks} Weeks",
                        style = Typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (phase.isCurrentActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElectricBlue.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "IN PROGRESS", style = Typography.labelSmall, color = ElectricBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = phase.title,
                style = Typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = phase.objective,
                style = Typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Topics Breakdown
            Text(text = "KEY TOPICS & CONCEPTS", style = Typography.labelSmall, color = SoftViolet)
            Spacer(modifier = Modifier.height(6.dp))

            val topicsList = phase.topicsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
            topicsList.forEach { topic ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = topic, style = Typography.bodyMedium, color = TextPrimary)
                }
            }

            if (phase.practicalProject.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MidnightNavy)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Assignment, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "PRACTICAL ASSIGNMENT", style = Typography.labelSmall, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                        Text(text = phase.practicalProject, style = Typography.bodySmall, color = TextPrimary)
                    }
                }
            }
        }
    }
}
