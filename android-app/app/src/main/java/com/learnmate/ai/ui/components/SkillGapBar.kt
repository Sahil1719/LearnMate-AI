package com.learnmate.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.learnmate.ai.ui.theme.AmberWarning
import com.learnmate.ai.ui.theme.DeepBlueBorder
import com.learnmate.ai.ui.theme.ElectricBlue
import com.learnmate.ai.ui.theme.EmeraldGreen
import com.learnmate.ai.ui.theme.RoseError
import com.learnmate.ai.ui.theme.TextPrimary
import com.learnmate.ai.ui.theme.TextSecondary
import com.learnmate.ai.ui.theme.Typography

@Composable
fun SkillGapBar(
    skillName: String,
    currentScore: Int,
    targetScore: Int,
    modifier: Modifier = Modifier
) {
    val isAssessed = (currentScore >= 0)
    val gap = if (isAssessed) maxOf(0, targetScore - currentScore) else targetScore

    val gapColor = when {
        !isAssessed -> AmberWarning
        gap == 0 -> EmeraldGreen
        gap == 1 -> ElectricBlue
        gap == 2 -> AmberWarning
        else -> RoseError
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = skillName,
                style = Typography.titleMedium,
                color = TextPrimary
            )
            val scoreLabel = if (isAssessed) {
                "Level $currentScore / $targetScore (Gap: $gap)"
            } else {
                "Not Assessed (Target: $targetScore)"
            }
            Text(
                text = scoreLabel,
                style = Typography.bodyMedium,
                color = gapColor
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress track (5 segment bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(DeepBlueBorder),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (level in 1..5) {
                val segmentColor = when {
                    !isAssessed -> if (level <= targetScore) AmberWarning.copy(alpha = 0.25f) else Color.Transparent
                    level <= currentScore -> ElectricBlue
                    level <= targetScore -> gapColor.copy(alpha = 0.4f)
                    else -> Color.Transparent
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .background(segmentColor)
                )
            }
        }
    }
}
