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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learnmate.ai.data.local.entities.ChatMessageEntity
import com.learnmate.ai.data.local.entities.ConversationEntity
import com.learnmate.ai.data.local.entities.LearnerProfileEntity
import com.learnmate.ai.data.repository.LearningRepository
import com.learnmate.ai.domain.engine.ExecuTorchLlmEngine
import com.learnmate.ai.domain.engine.LlmInferenceEngine
import com.learnmate.ai.domain.engine.NativeLlamaBridge
import com.learnmate.ai.domain.model.InferenceState
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val sender: String,
    var text: String,
    val isAi: Boolean,
    val runtimeSource: String = "XNNPACK (ARM NEON)"
)

@Composable
fun AiTutorScreen(
    profile: LearnerProfileEntity?,
    repository: LearningRepository? = null,
    engine: LlmInferenceEngine? = null,
    initialShowDiagnostics: Boolean = false,
    incomingQuery: String? = null,
    onQueryConsumed: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activeEngine = engine ?: remember { ExecuTorchLlmEngine(context = context) }
    var queryText by remember { mutableStateOf("") }
    var showDiagnosticsDialog by remember { mutableStateOf(initialShowDiagnostics) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(activeEngine) {
        if ((activeEngine as? ExecuTorchLlmEngine)?.getNativeHandle() == 0L &&
            activeEngine.state.value !is InferenceState.Loading
        ) {
            activeEngine.initialize()
        }
    }

    val inferenceState by activeEngine.state.collectAsState()
    val profileManager = (activeEngine as? ExecuTorchLlmEngine)?.profileManager
    val memSnapshot by (profileManager?.currentSnapshot ?: remember { MutableStateFlow(null) }).collectAsState()
    val currentProfile by (profileManager?.activeProfile ?: remember { MutableStateFlow(null) }).collectAsState()

    val profileId = profile?.id ?: "primary_learner"
    val goalTitle = profile?.targetGoalTitle?.ifBlank { "Personalized Learning" } ?: "Personalized Learning"
    val learnerName = profile?.name?.ifBlank { "Learner" } ?: "Learner"

    val conversations by (repository?.conversationsFlow ?: remember { MutableStateFlow(emptyList()) })
        .collectAsState(initial = emptyList())

    var activeConversationId by remember(profileId) { mutableStateOf<String?>(null) }
    var startFreshChatExplicitly by remember(profileId) { mutableStateOf(false) }

    LaunchedEffect(profileId, conversations) {
        if (activeConversationId != null && conversations.none { it.id == activeConversationId }) {
            activeConversationId = null
        }
        if (activeConversationId == null && !startFreshChatExplicitly && conversations.isNotEmpty()) {
            activeConversationId = conversations.first().id
        }
    }

    val persistedEntities by (activeConversationId?.let { repository?.getMessagesFlow(it) }
        ?: remember { MutableStateFlow(emptyList<ChatMessageEntity>()) })
        .collectAsState(initial = emptyList())

    // In-memory fallback messages only when repository is null (e.g., standalone Compose tests)
    val transientMessages = remember(profileId) { mutableStateListOf<ChatMessage>() }
    var streamingMessageId by remember { mutableStateOf<Long?>(null) }
    var streamingText by remember { mutableStateOf("") }

    val displayedMessages: List<ChatMessage> = if (repository != null) {
        if (persistedEntities.isEmpty()) {
            emptyList()
        } else {
            persistedEntities.map { entity ->
                val textValue = if (streamingMessageId == entity.id) streamingText else entity.text
                ChatMessage(
                    sender = entity.sender,
                    text = textValue,
                    isAi = entity.isAi,
                    runtimeSource = entity.runtimeSource
                )
            }
        }
    } else {
        transientMessages
    }

    fun sendPrompt(rawPrompt: String) {
        val userPrompt = rawPrompt.trim()
        if (userPrompt.isBlank()) return
        if (inferenceState is InferenceState.Generating || inferenceState is InferenceState.Loading) return

        // Do not silently fake AI replies if native model failed on device
        if (NativeLlamaBridge.isAvailable() && inferenceState !is InferenceState.Ready) {
            return
        }

        val activeSource = when (val s = inferenceState) {
            is InferenceState.Ready -> s.delegateBackend
            else -> "ExecuTorch Offline Engine"
        }

        val prevAiContext = displayedMessages.lastOrNull { it.isAi && it.text.isNotBlank() }?.text

        coroutineScope.launch {
            if (repository != null) {
                val convId = activeConversationId ?: repository.createConversation(
                    profileId = profileId,
                    title = userPrompt.take(48)
                ).also {
                    startFreshChatExplicitly = false
                    activeConversationId = it
                }

                val baseOrder = repository.getMessages(convId).size
                repository.appendChatMessage(
                    conversationId = convId,
                    sender = learnerName,
                    text = userPrompt,
                    isAi = false,
                    runtimeSource = "User",
                    orderIndex = baseOrder
                )
                val placeholderId = repository.appendChatMessage(
                    conversationId = convId,
                    sender = "LearnMate AI",
                    text = "",
                    isAi = true,
                    runtimeSource = activeSource,
                    orderIndex = baseOrder + 1
                )

                streamingMessageId = placeholderId
                streamingText = ""
                val builder = StringBuilder()

                val fullPrompt = if (activeEngine is ExecuTorchLlmEngine) {
                    activeEngine.buildCompletePrompt(
                        userQuestion = userPrompt,
                        goalTitle = goalTitle,
                        conversationContext = prevAiContext?.take(150)
                    )
                } else {
                    "Goal: $goalTitle\nQuestion: $userPrompt"
                }

                try {
                    activeEngine.generateStream(
                        fullPrompt,
                        maxTokens = currentProfile?.maxContextLength ?: 128
                    ).collect { token ->
                        builder.append(token)
                        streamingText = builder.toString()
                    }
                    val finalText = builder.toString().ifBlank { "[No tokens generated before EOS]" }
                    repository.updateChatMessage(convId, placeholderId, finalText, activeSource)
                } catch (e: Exception) {
                    val errText = "[Native Inference Error] ${e.message ?: "Model execution failed."}"
                    repository.updateChatMessage(convId, placeholderId, errText, "Runtime Error")
                } finally {
                    streamingMessageId = null
                    streamingText = ""
                }
            } else {
                transientMessages.add(ChatMessage(sender = learnerName, text = userPrompt, isAi = false))
                val aiMsg = ChatMessage(sender = "LearnMate AI", text = "", isAi = true, runtimeSource = activeSource)
                transientMessages.add(aiMsg)
                val targetIndex = transientMessages.lastIndex
                val builder = StringBuilder()
                val fullPrompt = if (activeEngine is ExecuTorchLlmEngine) {
                    activeEngine.buildCompletePrompt(
                        userQuestion = userPrompt,
                        goalTitle = goalTitle,
                        conversationContext = prevAiContext?.take(150)
                    )
                } else {
                    "Goal: $goalTitle\nQuestion: $userPrompt"
                }
                try {
                    activeEngine.generateStream(
                        fullPrompt,
                        maxTokens = currentProfile?.maxContextLength ?: 128
                    ).collect { token ->
                        builder.append(token)
                        if (targetIndex in transientMessages.indices) {
                            transientMessages[targetIndex] = transientMessages[targetIndex].copy(text = builder.toString())
                        }
                    }
                } catch (e: Exception) {
                    if (targetIndex in transientMessages.indices) {
                        transientMessages[targetIndex] = transientMessages[targetIndex].copy(
                            text = "[Native Inference Error] ${e.message ?: "Model execution failed."}",
                            runtimeSource = "Runtime Error"
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(incomingQuery, inferenceState) {
        val q = incomingQuery
        if (!q.isNullOrBlank() && inferenceState is InferenceState.Ready) {
            onQueryConsumed?.invoke()
            sendPrompt(q.replace("\\ ", " "))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightNavy)
            .padding(16.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "LearnMate AI Tutor", style = Typography.headlineSmall, color = TextPrimary)
            }

            val (badgeBg, badgeTextColor, badgeText) = when (inferenceState) {
                is InferenceState.Ready -> Triple(EmeraldGreen.copy(alpha = 0.18f), EmeraldGreen, "ON-DEVICE READY")
                is InferenceState.Generating -> Triple(SoftViolet.copy(alpha = 0.25f), SoftViolet, "GENERATING...")
                is InferenceState.Loading -> Triple(ElectricBlue.copy(alpha = 0.2f), ElectricBlue, "MMAP LOADING...")
                is InferenceState.ModelMissing -> Triple(RoseError.copy(alpha = 0.2f), RoseError, "MODEL MISSING")
                is InferenceState.Error -> Triple(RoseError.copy(alpha = 0.2f), RoseError, "INIT ERROR")
                else -> Triple(TextSecondary.copy(alpha = 0.15f), TextSecondary, "UNINITIALIZED")
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBg)
                    .clickable { showDiagnosticsDialog = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    style = Typography.labelSmall,
                    color = badgeTextColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Conversation Toolbar (History + New Chat)
        val activeConvo = conversations.firstOrNull { it.id == activeConversationId }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = activeConvo?.title ?: "New Conversation",
                style = Typography.labelMedium,
                color = ElectricBlue,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (repository != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DeepBlueCard)
                            .clickable { showHistoryDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = "History", tint = TextPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chats (${conversations.size})", style = Typography.labelSmall, color = TextPrimary)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricBlue.copy(alpha = 0.16f))
                            .clickable {
                                startFreshChatExplicitly = true
                                activeConversationId = null
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = "New Chat", tint = ElectricBlue, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New", style = Typography.labelSmall, color = ElectricBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Grounding / Error Card
        val isRuntimeBroken = NativeLlamaBridge.isAvailable() &&
            (inferenceState is InferenceState.Error || inferenceState is InferenceState.ModelMissing)
        Card(
            colors = CardDefaults.cardColors(containerColor = if (isRuntimeBroken) RoseError.copy(alpha = 0.14f) else DeepBlueCard),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isRuntimeBroken) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = RoseError, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Local AI model failed to initialize. No synthetic fallback responses are used.",
                            style = Typography.labelSmall,
                            color = RoseError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { coroutineScope.launch { activeEngine.initialize() } }
                    ) {
                        Text("Retry Model Initialization", color = ElectricBlue)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = SoftViolet, modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Learner: $learnerName • Goal: $goalTitle • 100% offline, saved locally.",
                        style = Typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Message Feed
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (displayedMessages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DeepBlueCard)
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = "LearnMate AI",
                                style = Typography.labelSmall,
                                color = SoftViolet,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Hello $learnerName! Ask any concept question related to $goalTitle. Your conversation will be saved automatically to your local profile history.",
                                style = Typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            } else {
                items(displayedMessages) { message ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (message.isAi) Arrangement.Start else Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 14.dp,
                                        topEnd = 14.dp,
                                        bottomStart = if (message.isAi) 2.dp else 14.dp,
                                        bottomEnd = if (message.isAi) 14.dp else 2.dp
                                    )
                                )
                                .background(if (message.isAi) DeepBlueCard else ElectricBlue)
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = message.sender,
                                        style = Typography.labelSmall,
                                        color = if (message.isAi) SoftViolet else MidnightNavy,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (message.isAi) {
                                        Text(
                                            text = message.runtimeSource,
                                            style = Typography.labelSmall.copy(fontSize = 9.sp),
                                            color = TextSecondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message.text.ifBlank { "..." },
                                    style = Typography.bodyMedium,
                                    color = if (message.isAi) TextPrimary else MidnightNavy
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val canSend = queryText.isNotBlank() &&
            inferenceState !is InferenceState.Generating &&
            inferenceState !is InferenceState.Loading &&
            (!NativeLlamaBridge.isAvailable() || inferenceState is InferenceState.Ready)

        // Message Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = queryText,
                onValueChange = { queryText = it },
                placeholder = { Text("Ask a question about $goalTitle...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = DeepBlueBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                enabled = canSend,
                onClick = {
                    val toSend = queryText
                    queryText = ""
                    sendPrompt(toSend)
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (canSend) ElectricBlue else DeepBlueBorder)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (canSend) MidnightNavy else TextSecondary
                )
            }
        }
    }

    if (showHistoryDialog && repository != null) {
        val formatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            containerColor = DeepBlueCard,
            title = {
                Text("Saved Conversations ($learnerName)", style = Typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (conversations.isEmpty()) {
                        Text(
                            text = "No saved conversations for this profile yet.",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    } else {
                        conversations.forEach { convo ->
                            val isSelected = convo.id == activeConversationId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) ElectricBlue.copy(alpha = 0.14f) else MidnightNavy)
                                    .clickable {
                                        startFreshChatExplicitly = false
                                        activeConversationId = convo.id
                                        showHistoryDialog = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = convo.title,
                                        style = Typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) ElectricBlue else TextPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = formatter.format(Date(convo.updatedAt)),
                                        style = Typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.deleteConversation(convo.id)
                                            if (activeConversationId == convo.id) {
                                                activeConversationId = null
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Conversation", tint = AmberWarning, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            startFreshChatExplicitly = true
                            activeConversationId = null
                            showHistoryDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start New Conversation", color = ElectricBlue, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Close", color = ElectricBlue)
                }
            }
        )
    }

    if (showDiagnosticsDialog) {
        AlertDialog(
            onDismissRequest = { showDiagnosticsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = ElectricBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Inference Engine Diagnostics", style = Typography.titleMedium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Current State: ${inferenceState::class.simpleName}",
                        style = Typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    val prof = currentProfile
                    if (prof != null) {
                        Text("• Active Profile: ${prof.name}", style = Typography.bodySmall, fontWeight = FontWeight.SemiBold, color = ElectricBlue)
                        Text("• Target Model: ${prof.modelTarget}", style = Typography.bodySmall, color = TextSecondary)
                        Text("• Bounded Context: ${prof.maxContextLength} tokens (KV-cache: ~${prof.estimatedKvCacheMb} MB)", style = Typography.bodySmall, color = TextSecondary)
                        Text("• Concurrency: ${if (prof.sequentialExecution) "Sequential (${prof.numThreads} threads)" else "${prof.numThreads} threads"}", style = Typography.bodySmall, color = TextSecondary)
                    }

                    val snap = memSnapshot
                    if (snap != null) {
                        Text(
                            text = "• Device RAM: ${snap.availableRamMb} MB free / ${snap.totalRamMb} MB total",
                            style = Typography.bodySmall,
                            color = if (snap.isLowMemory) AmberWarning else TextSecondary
                        )
                    }

                    when (val s = inferenceState) {
                        is InferenceState.Ready -> {
                            Text("• Model File: ${s.modelName}", style = Typography.bodySmall, color = TextSecondary)
                            Text("• Size: ${s.modelSizeBytes / (1024 * 1024)} MB", style = Typography.bodySmall, color = TextSecondary)
                            Text("• Memory Mapping: ${if (s.isMmap) "mmap enabled" else "In-memory"}", style = Typography.bodySmall, color = TextSecondary)
                            Text("• Backend: ${s.delegateBackend}", style = Typography.bodySmall, color = TextSecondary)
                        }
                        is InferenceState.ModelMissing -> {
                            Text("• Status: Model files not found or not extracted.", style = Typography.bodySmall, color = RoseError)
                            Text("• Expected Path: ${s.expectedModelPath}", style = Typography.bodySmall, color = TextSecondary)
                        }
                        is InferenceState.Generating -> {
                            Text("• Tokens Generated: ${s.tokensGeneratedSoFar}", style = Typography.bodySmall, color = TextSecondary)
                            Text("• Speed: %.1f tokens/sec".format(s.instantaneousTokensPerSec), style = Typography.bodySmall, color = TextSecondary)
                        }
                        is InferenceState.Error -> {
                            Text("• Error: ${s.errorMessage}", style = Typography.bodySmall, color = RoseError)
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDiagnosticsDialog = false }) {
                    Text("Close", color = ElectricBlue)
                }
            },
            containerColor = DeepBlueCard
        )
    }
}
