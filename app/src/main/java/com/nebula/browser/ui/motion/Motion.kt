package com.nebula.browser.ui.motion

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

/**
 * Set to true by [com.nebula.browser.ui.NebulaApp] when the user enables "Reduce motion".
 * Every animation helper in this file collapses to an instant jump when it is enabled.
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Shared duration and easing tokens so the whole app moves at the same rhythm. */
object Motion {
    const val Instant = 90
    const val Fast = 160
    const val Normal = 240
    const val Slow = 380
    const val Slower = 520

    val Ease: Easing = FastOutSlowInEasing
}

@Composable
fun motionMillis(base: Int): Int = if (LocalReduceMotion.current) 0 else base

@Composable
fun <T> motionTween(base: Int = Motion.Normal, easing: Easing = Motion.Ease): TweenSpec<T> =
    tween(durationMillis = motionMillis(base), easing = easing)

@Composable
fun <T> motionSpring(
    dampingRatio: Float = Spring.DampingRatioMediumBouncy,
    stiffness: Float = Spring.StiffnessMediumLow,
): SpringSpec<T> = if (LocalReduceMotion.current) {
    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
} else {
    spring(dampingRatio = dampingRatio, stiffness = stiffness)
}

@Composable
fun motionEasing(): Easing = if (LocalReduceMotion.current) androidx.compose.animation.core.LinearEasing else Motion.Ease

/** Standard "press" feedback: the surface dips slightly while the finger is down. */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.955f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !LocalReduceMotion.current) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return this.scale(scale)
}

/** A gentle breathing alpha, used for the Nebula mark and live status dots. */
@Composable
fun rememberPulse(
    active: Boolean = true,
    minAlpha: Float = 0.55f,
    maxAlpha: Float = 1f,
    periodMillis: Int = 2_000,
): State<Float> {
    if (!active || LocalReduceMotion.current) {
        return remember { mutableFloatStateOf(1f) }
    }
    val transition = rememberInfiniteTransition(label = "pulse")
    return transition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = periodMillis, easing = Motion.Ease),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
}

/** A slow drifting offset (-1..1) for parallax-style background movement. */
@Composable
fun rememberDrift(active: Boolean = true, periodMillis: Int = 14_000): State<Float> {
    if (!active || LocalReduceMotion.current) {
        return remember { mutableFloatStateOf(0f) }
    }
    val transition = rememberInfiniteTransition(label = "drift")
    return transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = periodMillis, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "driftOffset",
    )
}
