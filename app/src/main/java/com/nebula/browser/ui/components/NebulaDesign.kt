package com.nebula.browser.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nebula.browser.ui.motion.pressScale
import kotlin.math.sin

internal val BrandPurple = Color(0xFF9D6CFF)
internal val BrandViolet = Color(0xFF6638E8)
internal val BrandCyan = Color(0xFF61E4F2)

/** Theme-aware accent aliases: they follow the selected accent (or Material You palette). */
val NebulaPurple: Color
    @Composable get() = MaterialTheme.colorScheme.primary
val NebulaViolet: Color
    @Composable get() = MaterialTheme.colorScheme.primary
val NebulaCyan: Color
    @Composable get() = MaterialTheme.colorScheme.secondary
val NebulaGlass: Color
    @Composable get() = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
val NebulaOutline: Color
    @Composable get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)

@Composable
fun SpaceBackground(modifier: Modifier = Modifier, style: String = "nebula", liquidEffects: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    val dark = MaterialTheme.colorScheme.background.red < 0.2f
    val stars = remember {
        List(65) { index ->
            Triple(
                ((index * 73) % 997) / 997f,
                ((index * 137) % 991) / 991f,
                0.7f + (index % 3) * 0.45f,
            )
        }
    }
    val backgroundBrush = when {
        !dark -> Brush.verticalGradient(listOf(Color(0xFFF8F4FF), Color(0xFFEDE6FF), Color(0xFFF9F7FF)))
        style == "aurora" -> Brush.verticalGradient(listOf(Color(0xFF010A12), Color(0xFF071C28), Color(0xFF03060D)))
        style == "midnight" -> Brush.verticalGradient(listOf(Color(0xFF03040D), Color(0xFF0D1028), Color(0xFF03040D)))
        style == "minimal" -> Brush.verticalGradient(listOf(Color(0xFF08080A), Color(0xFF101014)))
        else -> Brush.verticalGradient(listOf(Color(0xFF030209), Color(0xFF0B0619), Color(0xFF030209)))
    }
    Box(modifier.fillMaxSize().background(backgroundBrush)) {
        val drift = com.nebula.browser.ui.motion.rememberDrift(active = liquidEffects && dark).value
        Canvas(Modifier.fillMaxSize()) {
            if (dark) {
                if (liquidEffects) drawCircle(
                    Brush.radialGradient(listOf(if (style == "aurora") BrandCyan.copy(alpha = .24f) else Color(0xFF4B1599).copy(alpha = .30f), Color(0xFF120626).copy(alpha = .08f), Color.Transparent)),
                    radius = size.width * (.85f + drift * .04f), center = Offset(size.width * (.96f + drift * .02f), size.height * .05f),
                )
                if (liquidEffects) drawCircle(
                    Brush.radialGradient(listOf(Color(0xFF321176).copy(alpha = .24f), Color.Transparent)),
                    radius = size.width * (.72f - drift * .03f), center = Offset(-size.width * (.08f - drift * .015f), size.height * .82f),
                )
                stars.forEach { star ->
                    drawCircle(Color.White.copy(alpha = 0.35f + star.third / 7f), star.third, Offset(size.width * star.first, size.height * star.second))
                }
                val waveBrush = Brush.linearGradient(listOf(BrandViolet.copy(alpha = .05f), BrandPurple.copy(alpha = .7f), BrandCyan.copy(alpha = .32f), BrandViolet.copy(alpha = .05f)))
                if (liquidEffects) listOf(0f, 22f, 44f).forEachIndexed { index, offset ->
                    drawArc(
                        waveBrush, startAngle = 190f + index * 5, sweepAngle = 160f, useCenter = false,
                        topLeft = Offset(-size.width * .28f, size.height * .77f + offset),
                        size = androidx.compose.ui.geometry.Size(size.width * 1.56f, size.height * .30f),
                        style = Stroke(width = (2.2f - index * .4f).dp.toPx()),
                    )
                }
            }
        }
        content()
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    active: Boolean = false,
    radius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val colors = CardDefaults.cardColors(
        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = .78f),
        contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    )
    val border = androidx.compose.foundation.BorderStroke(
        if (active) 1.5.dp else 1.dp,
        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = .28f),
    )
    if (onClick != null) Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, border = border, content = content)
    else Card(modifier = modifier, shape = shape, colors = colors, border = border, content = content)
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Button(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier.height(50.dp)
            .pressScale(interaction)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .7f), RoundedCornerShape(18.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        leadingIcon?.invoke()
        Text(text, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
