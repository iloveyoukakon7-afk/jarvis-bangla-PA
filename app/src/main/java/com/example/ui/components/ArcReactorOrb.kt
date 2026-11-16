package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.OrbStatus
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.BrightGold
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CrimsonShield
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.WarmCream

@Composable
fun ArcReactorOrb(
    status: OrbStatus,
    audioLevel: Float,
    wakeName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    orbSize: Dp = 200.dp
) {
    val targetCoreColor = when (status) {
        OrbStatus.IDLE -> RoyalGold
        OrbStatus.WAKE_STANDBY -> BrightGold
        OrbStatus.LISTENING -> ChampagneGold
        OrbStatus.THINKING -> RoyalGold
        OrbStatus.SPEAKING -> BrightGold
        OrbStatus.CALL_SCREENING -> AmberAlert
        OrbStatus.RECORDING_CALL -> CrimsonShield
        OrbStatus.BLOCKED_SHIELD -> CrimsonShield
    }

    val coreColor by animateColorAsState(
        targetValue = targetCoreColor,
        animationSpec = tween(350),
        label = "gold_orb_color"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "chhotu_gold_orb")

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    OrbStatus.THINKING -> 2400
                    OrbStatus.LISTENING, OrbStatus.SPEAKING -> 3800
                    else -> 11000
                },
                easing = LinearEasing
            )
        ),
        label = "outer_gold_rotation"
    )

    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    OrbStatus.THINKING -> 1700
                    OrbStatus.LISTENING -> 2900
                    else -> 8000
                },
                easing = LinearEasing
            )
        ),
        label = "inner_gold_rotation"
    )

    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gold_pulse"
    )

    val reactiveScale by animateFloatAsState(
        targetValue = if (status == OrbStatus.LISTENING) {
            1.0f + (audioLevel * 0.22f)
        } else if (status == OrbStatus.SPEAKING) {
            1.08f
        } else {
            1.0f
        },
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "reactive_gold_scale"
    )

    val statusText = when (status) {
        OrbStatus.IDLE -> "\"$wakeName\" বইলা ডাকেন বস"
        OrbStatus.WAKE_STANDBY -> "\"$wakeName\" ডাকের অপেক্ষায়..."
        OrbStatus.LISTENING -> "শুনতাছি বস, কন..."
        OrbStatus.THINKING -> "বসের কথা ভাবতাছি..."
        OrbStatus.SPEAKING -> "বসরে মুখে কইতাছি..."
        OrbStatus.CALL_SCREENING -> "দারোয়ান কল ধরছে..."
        OrbStatus.RECORDING_CALL -> "কলটি রেকর্ড হচ্ছে..."
        OrbStatus.BLOCKED_SHIELD -> "ব্যাংক সুরক্ষা চালু"
    }

    val centerIcon = when (status) {
        OrbStatus.BLOCKED_SHIELD -> Icons.Filled.Shield
        OrbStatus.RECORDING_CALL -> Icons.Filled.FiberManualRecord
        OrbStatus.CALL_SCREENING -> Icons.Filled.Call
        OrbStatus.THINKING -> Icons.Filled.Psychology
        OrbStatus.SPEAKING -> Icons.Filled.GraphicEq
        else -> Icons.Filled.Mic
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(orbSize)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = coreColor),
                    onClick = onClick
                )
                .semantics {
                    contentDescription = "ছোটু ভয়েস বাটন: $statusText"
                }
                .testTag("arc_reactor_orb"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension / 2f

                // Luxury golden halo glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            coreColor.copy(alpha = 0.36f * idlePulse),
                            coreColor.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRadius * reactiveScale
                    ),
                    radius = maxRadius,
                    center = center
                )

                // Outer golden crown ring segments
                rotate(outerRotation, pivot = center) {
                    val ringRadius = maxRadius * 0.85f
                    val arcSize = Size(ringRadius * 2f, ringRadius * 2f)
                    val topLeft = Offset(center.x - ringRadius, center.y - ringRadius)
                    val stroke = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)

                    for (i in 0 until 4) {
                        drawArc(
                            color = coreColor.copy(alpha = 0.9f),
                            startAngle = i * 90f + 12f,
                            sweepAngle = 58f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = stroke
                        )
                    }
                }

                // Inner counter-rotating luxury bezel
                rotate(innerRotation, pivot = center) {
                    val midRadius = maxRadius * 0.68f
                    val arcSize = Size(midRadius * 2f, midRadius * 2f)
                    val topLeft = Offset(center.x - midRadius, center.y - midRadius)
                    val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Butt)

                    for (i in 0 until 6) {
                        drawArc(
                            color = coreColor.copy(alpha = if (i % 2 == 0) 0.95f else 0.42f),
                            startAngle = i * 60f + 8f,
                            sweepAngle = 36f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = stroke
                        )
                    }
                }

                // Obsidian core with golden border
                val coreRadius = maxRadius * 0.46f * reactiveScale.coerceAtMost(1.12f)
                drawCircle(
                    color = ObsidianBlack.copy(alpha = 0.94f),
                    radius = coreRadius,
                    center = center
                )
                drawCircle(
                    color = coreColor,
                    radius = coreRadius,
                    center = center,
                    style = Stroke(width = 2.8.dp.toPx())
                )
            }

            Icon(
                imageVector = centerIcon,
                contentDescription = null,
                tint = WarmCream,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleMedium,
            color = if (status == OrbStatus.BLOCKED_SHIELD || status == OrbStatus.RECORDING_CALL) {
                CrimsonShield
            } else {
                BrightGold
            }
        )
        Text(
            text = "মালিক: সুমন বস • ব্যাংক সুরক্ষা চালু",
            style = MaterialTheme.typography.labelSmall,
            color = MutedSand
        )
    }
}
