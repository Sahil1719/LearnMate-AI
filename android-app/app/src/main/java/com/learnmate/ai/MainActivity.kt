package com.learnmate.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.learnmate.ai.domain.engine.ModelPreparationState
import com.learnmate.ai.ui.components.LearnMateBottomNavBar
import com.learnmate.ai.ui.components.NavItem
import com.learnmate.ai.ui.screens.AiTutorScreen
import com.learnmate.ai.ui.screens.DashboardScreen
import com.learnmate.ai.ui.screens.ModelPreparationScreen
import com.learnmate.ai.ui.screens.OnboardingFlow
import com.learnmate.ai.ui.screens.QuizScreen
import com.learnmate.ai.ui.screens.RoadmapScreen
import com.learnmate.ai.ui.screens.SkillAnalysisScreen
import com.learnmate.ai.ui.screens.StudyPlannerScreen
import com.learnmate.ai.ui.theme.LearnMateAITheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val queryState = mutableStateOf<String?>(null)
    private val routeState = mutableStateOf<String?>(null)

    private val queryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            val q = intent?.getStringExtra("query")
            val r = intent?.getStringExtra("route")
            android.util.Log.i("MainActivity", "Broadcast query received: q='$q', route='$r'")
            if (!r.isNullOrBlank()) {
                routeState.value = r
            }
            if (!q.isNullOrBlank()) {
                queryState.value = q
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val r = intent.getStringExtra("route")
        val q = intent.getStringExtra("query")
        android.util.Log.i("MainActivity", "onNewIntent received: q='$q', route='$r'")
        if (!r.isNullOrBlank()) {
            routeState.value = r
        }
        if (!q.isNullOrBlank()) {
            queryState.value = q
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(queryReceiver)
        } catch (_: Exception) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as LearnMateApplication
        val repository = app.repository
        val modelAssetManager = app.modelAssetManager
        val llmEngine = app.llmEngine

        val initialQuery = intent.getStringExtra("query")
        val initialRouteParam = intent.getStringExtra("route")
        android.util.Log.i("MainActivity", "onCreate with initialQuery='$initialQuery', initialRoute='$initialRouteParam'")
        if (!initialQuery.isNullOrBlank()) {
            queryState.value = initialQuery
        }
        if (!initialRouteParam.isNullOrBlank()) {
            routeState.value = initialRouteParam
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(queryReceiver, android.content.IntentFilter("com.learnmate.ai.QUERY"), android.content.Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(queryReceiver, android.content.IntentFilter("com.learnmate.ai.QUERY"))
        }

        setContent {
            LearnMateAITheme {
                val prepState by modelAssetManager.state.collectAsState()
                var prepAttempt by remember { mutableIntStateOf(0) }

                LaunchedEffect(prepAttempt) {
                    modelAssetManager.prepareAndInitialize(llmEngine)
                }

                if (prepState !is ModelPreparationState.Ready) {
                    ModelPreparationScreen(
                        state = prepState,
                        onRetry = { prepAttempt++ }
                    )
                } else {
                    val allProfiles by repository.allProfilesFlow.collectAsState(initial = emptyList())
                    val profile by repository.profileFlow.collectAsState(initial = null)
                    val skills by repository.skillsFlow.collectAsState(initial = emptyList())
                    val phases by repository.phasesFlow.collectAsState(initial = emptyList())
                    val tasks by repository.tasksFlow.collectAsState(initial = emptyList())

                    val coroutineScope = rememberCoroutineScope()
                    val initialRoute = intent.getStringExtra("route")
                    val currentIncomingRoute by routeState
                    val currentIncomingQuery by queryState
                    var currentRoute by remember { mutableStateOf(currentIncomingRoute ?: initialRoute ?: NavItem.Dashboard.route) }
                    LaunchedEffect(currentIncomingRoute) {
                        currentIncomingRoute?.let { currentRoute = it }
                    }
                    var isEditingProfile by remember { mutableStateOf(false) }
                    var isCreatingNewProfile by remember { mutableStateOf(false) }
                    var activeQuizTopic by remember { mutableStateOf<String?>(intent.getStringExtra("topic")) }

                    val initialStage = intent.getStringExtra("stage")
                    var pendingPreset by remember { mutableStateOf(intent.getStringExtra("preset")) }
                    val commitDirectly = intent.getBooleanExtra("commit", false)
                    val forceReset = intent.getBooleanExtra("reset", false)
                    val initialShowDiag = intent.getBooleanExtra("diag", false)

                    LaunchedEffect(forceReset) {
                        if (forceReset) {
                            repository.resetProfile()
                        }
                    }

                    if (profile == null || isEditingProfile || isCreatingNewProfile || !initialStage.isNullOrBlank() || !pendingPreset.isNullOrBlank()) {
                        OnboardingFlow(
                            hasExistingProfile = (profile != null && !isCreatingNewProfile),
                            existingProfileName = profile?.name,
                            editingProfile = if (isEditingProfile) profile else null,
                            isCreatingNewProfile = isCreatingNewProfile,
                            initialStageName = initialStage,
                            initialPreset = pendingPreset,
                            commitPresetDirectly = commitDirectly,
                            onCompleteOnboarding = { name, learnerType, eduLevel, eduStream, institution,
                                                    domain, goal, lang, hours, days, sessionMins, timeline, assessedSkills ->
                                coroutineScope.launch {
                                    repository.saveFullProfile(
                                        name = name,
                                        learnerType = learnerType,
                                        educationLevel = eduLevel,
                                        educationStream = eduStream,
                                        educationInstitution = institution,
                                        targetDomain = domain,
                                        targetGoalTitle = goal,
                                        preferredLanguage = lang,
                                        availableHoursPerDay = hours,
                                        preferredStudyDays = days,
                                        preferredSessionMinutes = sessionMins,
                                        targetCompletionDate = timeline,
                                        assessedSkills = assessedSkills,
                                        existingProfileId = if (isEditingProfile) profile?.id else null
                                    )
                                    isEditingProfile = false
                                    isCreatingNewProfile = false
                                    pendingPreset = null
                                    currentRoute = currentIncomingRoute ?: initialRoute ?: NavItem.Dashboard.route
                                }
                            },
                            onNavigateToExistingDashboard = {
                                isEditingProfile = false
                                isCreatingNewProfile = false
                                currentRoute = NavItem.Dashboard.route
                            }
                        )
                    } else {
                        Scaffold(
                            bottomBar = {
                                LearnMateBottomNavBar(
                                    currentRoute = currentRoute,
                                    onNavigate = { currentRoute = it }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (currentRoute) {
                                    NavItem.Dashboard.route -> DashboardScreen(
                                        profile = profile,
                                        allProfiles = allProfiles,
                                        phases = phases,
                                        tasks = tasks,
                                        onToggleTask = { taskId, completed ->
                                            coroutineScope.launch {
                                                repository.updateTaskCompletion(taskId, completed)
                                            }
                                        },
                                        onNavigateToRoadmap = { currentRoute = NavItem.Roadmap.route },
                                        onNavigateToTutor = { currentRoute = NavItem.Tutor.route },
                                        onNavigateToQuiz = { topic ->
                                            activeQuizTopic = topic
                                            currentRoute = NavItem.Quiz.route
                                        },
                                        onEditProfile = {
                                            isCreatingNewProfile = false
                                            isEditingProfile = true
                                        },
                                        onCreateNewProfile = {
                                            isEditingProfile = false
                                            isCreatingNewProfile = true
                                        },
                                        onSwitchProfile = { targetId ->
                                            coroutineScope.launch {
                                                repository.switchProfile(targetId)
                                            }
                                        },
                                        onDeleteProfile = { targetId ->
                                            coroutineScope.launch {
                                                repository.deleteProfile(targetId)
                                            }
                                        }
                                    )
                                    NavItem.SkillAnalysis.route -> SkillAnalysisScreen(
                                        skills = skills,
                                        onUpdateSkill = { skillName, newScore ->
                                            coroutineScope.launch {
                                                repository.updateSkillCompetency(skillName, newScore)
                                            }
                                        },
                                        onNavigateToQuiz = { topic ->
                                            activeQuizTopic = topic
                                            currentRoute = NavItem.Quiz.route
                                        }
                                    )
                                    NavItem.Roadmap.route -> RoadmapScreen(
                                        phases = phases
                                    )
                                    NavItem.Planner.route -> StudyPlannerScreen(
                                        tasks = tasks,
                                        availableHoursPerDay = profile?.availableHoursPerDay ?: 2,
                                        onToggleTask = { taskId, completed ->
                                            coroutineScope.launch {
                                                repository.updateTaskCompletion(taskId, completed)
                                            }
                                        }
                                    )
                                    NavItem.Tutor.route -> AiTutorScreen(
                                        profile = profile,
                                        repository = repository,
                                        engine = llmEngine,
                                        initialShowDiagnostics = initialShowDiag,
                                        incomingQuery = currentIncomingQuery,
                                        onQueryConsumed = { queryState.value = null }
                                    )
                                    NavItem.Quiz.route -> QuizScreen(
                                        profile = profile,
                                        skills = skills,
                                        repository = repository,
                                        initialTopic = activeQuizTopic,
                                        onNavigateBack = { currentRoute = NavItem.Dashboard.route },
                                        onNavigateToPlan = { currentRoute = NavItem.Planner.route }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
