package com.learnmate.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.learnmate.ai.domain.engine.ModelPreparationState
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
fun ModelPreparationScreen(
    state: ModelPreparationState,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightNavy)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DeepBlueBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val isError = state is ModelPreparationState.Error
                val icon = when (state) {
                    is ModelPreparationState.Error -> if (state.isStorageError) Icons.Default.Storage else Icons.Default.Warning
                    is ModelPreparationState.Verifying -> Icons.Default.VerifiedUser
                    else -> Icons.Default.Memory
                }
                val accent = if (isError) RoseError else ElectricBlue

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.14f))
                        .border(1.5.dp, accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = if (isError) (state as ModelPreparationState.Error).title else "Preparing Offline AI Engine",
                    style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Meta Llama 3.2 3B Instruct • INT4 8da4w • XNNPACK NEON",
                    style = Typography.labelSmall,
                    color = EmeraldGreen,
                    textAlign = TextAlign.Center
                )

                when (state) {
                    is ModelPreparationState.Checking -> {
                        CircularProgressIndicator(color = ElectricBlue)
                        Text(
                            text = "Checking verified on-device model storage...",
                            style = Typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    is ModelPreparationState.Extracting -> {
                        val copiedMb = state.bytesCopied / (1024 * 1024)
                        val totalMb = state.totalBytes / (1024 * 1024)
                        val pct = (state.progress * 100).toInt()
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ElectricBlue,
                            trackColor = DeepBlueBorder
                        )
                        Text(
                            text = "${state.stage}\n$copiedMb MB / $totalMb MB ($pct%)",
                            style = Typography.bodyMedium,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "First-launch setup extracts and SHA-256 verifies the bundled model once for fast memory-mapped reuse.",
                            style = Typography.labelSmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    is ModelPreparationState.Verifying -> {
                        CircularProgressIndicator(color = EmeraldGreen)
                        Text(
                            text = state.stage,
                            style = Typography.bodyMedium,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }

                    is ModelPreparationState.InitializingEngine -> {
                        CircularProgressIndicator(color = ElectricBlue)
                        Text(
                            text = state.message,
                            style = Typography.bodyMedium,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "SHA-256 verified. Mapping weights into app memory...",
                            style = Typography.labelSmall,
                            color = AmberWarning,
                            textAlign = TextAlign.Center
                        )
                    }

                    is ModelPreparationState.Error -> {
                        Text(
                            text = state.message,
                            style = Typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = MidnightNavy),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Retry Setup", fontWeight = FontWeight.Bold)
                        }
                    }

                    is ModelPreparationState.Ready -> {
                        Text(
                            text = "Model verified and ready.",
                            style = Typography.bodyMedium,
                            color = EmeraldGreen
                        )
                    }
                }
            }
        }
    }
}
