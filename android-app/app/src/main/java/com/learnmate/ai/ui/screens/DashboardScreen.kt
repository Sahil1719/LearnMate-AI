package com.learnmate.ai.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learnmate.ai.data.local.entities.LearnerProfileEntity
import com.learnmate.ai.data.local.entities.RoadmapPhaseEntity
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
fun DashboardScreen(
    profile: LearnerProfileEntity?,
    allProfiles: List<LearnerProfileEntity> = emptyList(),
    phases: List<RoadmapPhaseEntity>,
    tasks: List<StudyTaskEntity>,
    onToggleTask: (String, Boolean) -> Unit,
    onNavigateToRoadmap: () -> Unit,
    onNavigateToTutor: () -> Unit,
    onNavigateToQuiz: (String?) -> Unit = {},
    onEditProfile: () -> Unit = {},
    onCreateNewProfile: () -> Unit = {},
    onSwitchProfile: (String) -> Unit = {},
    onDeleteProfile: (String) -> Unit = {}
) {
    var showProfileManagerDialog by remember { mutableStateOf(false) }
    val activePhase = phases.firstOrNull { it.isCurrentActive } ?: phases.firstOrNull()
    val completedTasksCount = tasks.count { it.isCompleted }
    val progressPercent = if (tasks.isNotEmpty()) (completedTasksCount * 100) / tasks.size else 0

    // Filter strictly for Today's tasks (Part 4)
    val todayTasks = tasks.filter { it.scheduledDate == "Today" || it.scheduledDate == "Immediate Revision" }.ifEmpty {
        tasks.filter { it.scheduledDate == "Day 1" }
    }
    val todayScheduledMinutes = todayTasks.sumOf { it.durationMinutes }
    val maxDailyMinutes = (profile?.availableHoursPerDay ?: 2) * 60

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightNavy)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Personalised Greeting & Streak
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hello, ${profile?.name?.ifBlank { "Learner" } ?: "Learner"}",
                        style = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = profile?.targetGoalTitle?.ifBlank { "Your Learning Journey" } ?: "Your Learning Journey",
                        style = Typography.bodyMedium,
                        color = ElectricBlue
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Streak Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(AmberWarning.copy(alpha = 0.2f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = AmberWarning,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${profile?.learningStreak ?: 1}d",
                            style = Typography.titleSmall,
                            color = AmberWarning,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { showProfileManagerDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DeepBlueCard)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Manage Profiles",
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onEditProfile,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DeepBlueCard)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Section: Active Goal & Current Phase
        item {
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
                        Text(
                            text = "CURRENT PHASE",
                            style = Typography.labelSmall,
                            color = SoftViolet,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = profile?.targetDomain ?: "General",
                            style = Typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = activePhase?.title ?: "Phase 1: Foundations",
                        style = Typography.headlineSmall,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = activePhase?.objective ?: "Mastering core concepts and fundamental techniques.",
                        style = Typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Overall Progress", style = Typography.bodySmall, color = TextSecondary)
                        Text(text = "$progressPercent%", style = Typography.bodySmall, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = EmeraldGreen,
                        trackColor = MidnightNavy
                    )
                }
            }
        }

        // Section: Recommended Next Activity
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "RECOMMENDED NEXT ACTIVITY", style = Typography.labelSmall, color = ElectricBlue)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activePhase?.practicalProject?.ifBlank { "Core Application Assignment" }
                            ?: "Core Application Assignment",
                        style = Typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onNavigateToRoadmap,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MidnightNavy)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open Roadmap", color = MidnightNavy, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onNavigateToTutor,
                            colors = ButtonDefaults.buttonColors(containerColor = SoftViolet.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Ask AI Tutor", color = SoftViolet)
                        }
                    }
                }
            }
        }

        // Section: Knowledge Assessment & Diagnostic Quizzes
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "KNOWLEDGE ASSESSMENT",
                            style = Typography.labelSmall,
                            color = AmberWarning,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Adaptive Diagnostic",
                            style = Typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Verify Competency & Diagnostic Testing",
                        style = Typography.titleMedium,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Take interactive multiple-choice assessments offline. Scores below 60% automatically schedule targeted remediation tasks.",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onNavigateToQuiz(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Quiz, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Diagnostic Quiz", color = AmberWarning, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: Today's Study Plan (Enforcing daily time constraints)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Today's Study Plan", style = Typography.titleMedium, color = TextPrimary)
                Text(
                    text = "$todayScheduledMinutes / $maxDailyMinutes mins",
                    style = Typography.labelSmall,
                    color = if (todayScheduledMinutes <= maxDailyMinutes) EmeraldGreen else AmberWarning
                )
            }
        }

        if (todayTasks.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "All tasks for today completed! Excellent work.",
                            style = Typography.bodyMedium,
                            color = EmeraldGreen
                        )
                    }
                }
            }
        } else {
            items(todayTasks) { task ->
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
                        Text(
                            text = task.title,
                            style = Typography.bodyLarge,
                            color = if (task.isCompleted) TextSecondary else TextPrimary,
                            fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${task.durationMinutes} mins • ${task.relatedSkill}",
                            style = Typography.bodySmall,
                            color = if (task.isRevisionTask) AmberWarning else TextSecondary
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
        }
    }

    if (showProfileManagerDialog) {
        AlertDialog(
            onDismissRequest = { showProfileManagerDialog = false },
            containerColor = DeepBlueCard,
            title = {
                Text("Learner Profiles", style = Typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val profilesToShow = allProfiles.ifEmpty { listOfNotNull(profile) }
                    profilesToShow.forEach { p ->
                        val isActive = p.id == profile?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isActive) ElectricBlue.copy(alpha = 0.14f) else MidnightNavy)
                                .clickable {
                                    if (!isActive) {
                                        onSwitchProfile(p.id)
                                    }
                                    showProfileManagerDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isActive) "${p.name} (Active)" else p.name,
                                    style = Typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) ElectricBlue else TextPrimary
                                )
                                Text(
                                    text = "${p.targetGoalTitle.ifBlank { p.targetDomain }} • ${p.educationLevel}",
                                    style = Typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            if (isActive) {
                                IconButton(
                                    onClick = {
                                        showProfileManagerDialog = false
                                        onEditProfile()
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = ElectricBlue, modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(
                                onClick = {
                                    showProfileManagerDialog = false
                                    onDeleteProfile(p.id)
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Profile", tint = AmberWarning, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            showProfileManagerDialog = false
                            onCreateNewProfile()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create New Profile", color = ElectricBlue, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileManagerDialog = false }) {
                    Text("Close", color = ElectricBlue)
                }
            }
        )
    }
}
