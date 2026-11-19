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
import kotlin.math.sin

val NebulaPurple = Color(0xFF9D6CFF)
val NebulaViolet = Color(0xFF6638E8)
val NebulaCyan = Color(0xFF61E4F2)
val NebulaGlass = Color(0xB0181428)
val NebulaOutline = Color(0x589D6CFF)

@Composable
fun SpaceBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
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
    Box(modifier.fillMaxSize().background(
        if (dark) Brush.verticalGradient(listOf(Color(0xFF030209), Color(0xFF0B0619), Color(0xFF030209)))
        else Brush.verticalGradient(listOf(Color(0xFFF8F4FF), Color(0xFFEDE6FF), Color(0xFFF9F7FF)))
    )) {
        Canvas(Modifier.fillMaxSize()) {
            if (dark) {
                stars.forEach { star ->
                    drawCircle(Color.White.copy(alpha = 0.25f + star.third / 8f), star.third, Offset(size.width * star.first, size.height * star.second))
                }
                drawArc(
                    Brush.linearGradient(listOf(NebulaViolet.copy(alpha = 0f), NebulaPurple.copy(alpha = .45f), NebulaCyan.copy(alpha = .05f))),
                    startAngle = 192f, sweepAngle = 156f, useCenter = false,
                    topLeft = Offset(-size.width * .35f, size.height * .72f),
                    size = androidx.compose.ui.geometry.Size(size.width * 1.7f, size.height * .35f),
                    style = Stroke(width = 3.dp.toPx()),
                )
                drawCircle(
                    Brush.radialGradient(listOf(NebulaViolet.copy(alpha = .12f), Color.Transparent)),
                    radius = size.width * .7f,
                    center = Offset(size.width * .9f, size.height * .08f),
                )
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
    val colors = CardDefaults.cardColors(containerColor = if (active) Color(0xD92D1A51) else MaterialTheme.colorScheme.surface.copy(alpha = .68f))
    val border = androidx.compose.foundation.BorderStroke(if (active) 1.5.dp else 1.dp, if (active) NebulaPurple else NebulaOutline)
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
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp).clip(RoundedCornerShape(18.dp)).border(1.dp, NebulaPurple.copy(alpha = .7f), RoundedCornerShape(18.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = NebulaViolet),
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
