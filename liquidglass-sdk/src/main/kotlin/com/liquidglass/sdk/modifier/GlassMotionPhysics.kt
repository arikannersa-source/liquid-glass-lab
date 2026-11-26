package com.liquidglass.sdk.modifier

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.tanh

/**
 * Elastic spring physics and velocity-driven gel deformation helpers inspired by
 * Apple iOS 26/27 Liquid Glass touch response and fluid morphing.
 */
object GlassMotionPhysics {

    /** Creates an elastic spring spec scaled by the user's [animationSpeed] multiplier. */
    fun elasticFloatSpring(
        animationSpeed: Float = 1.0f,
        dampingRatio: Float = 0.62f,
        baseStiffness: Float = Spring.StiffnessMediumLow
    ): SpringSpec<Float> {
        val speedSq = (animationSpeed.coerceIn(0.25f, 2.5f)).let { it * it }
        return spring(
            dampingRatio = dampingRatio,
            stiffness = (baseStiffness * speedSq).coerceIn(80f, 4000f)
        )
    }

    /** Creates an elastic Offset spring for draggable glass pills and morphing lenses. */
    fun elasticOffsetSpring(
        animationSpeed: Float = 1.0f,
        dampingRatio: Float = 0.68f,
        baseStiffness: Float = 420f
    ): SpringSpec<Offset> {
        val speedSq = (animationSpeed.coerceIn(0.25f, 2.5f)).let { it * it }
        return spring(
            dampingRatio = dampingRatio,
            stiffness = (baseStiffness * speedSq).coerceIn(80f, 5000f)
        )
    }

    /**
     * Computes elastic press scale X and Y from press state, velocity, and [touchDeformation].
     * Implements subtle area-preserving gel squash-and-stretch when pressed or dragged rapidly.
     */
    fun computeGelScale(
        pressProgress: Float,
        velocityPxPerSec: Offset = Offset.Zero,
        touchDeformation: Float = 1.0f
    ): Pair<Float, Float> {
        val amp = touchDeformation.coerceIn(0f, 2.5f)
        if (amp <= 0.001f) return 1f to 1f
        val pressSwell = 1f + 0.045f * pressProgress.coerceIn(0f, 1.5f) * amp
        val vx = tanh((abs(velocityPxPerSec.x) / 2800f).toDouble()).toFloat() * 0.08f * amp
        val vy = tanh((abs(velocityPxPerSec.y) / 2800f).toDouble()).toFloat() * 0.08f * amp
        val sx = (pressSwell + vx - vy * 0.55f).coerceIn(0.86f, 1.18f)
        val sy = (pressSwell + vy - vx * 0.55f).coerceIn(0.86f, 1.18f)
        return sx to sy
    }
}
