package com.bottlecast.app.common.ui

import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibrationEffect.Composition
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat

/**
 * The app's haptics (spec §1.5, haptics table). Each moment plays composition primitives where the
 * phone supports them all, and a predefined effect where it doesn't. They play as touch feedback,
 * so the system's touch-feedback setting turns them off. They stay on under reduced motion, because
 * they say what happened rather than decorate it.
 */
@Stable
interface BottlingHaptics {

    /** A shake or tap that loosens the cork, [step] 1 or 2 of 3 (the third pops it). */
    fun shakeStep(step: Int)

    /** A held bottle reaching a third. */
    fun holdTick()

    /** The cork pops. [afterStep] when a third shake or tap popped it, which leads in with its own tick. */
    fun corkPop(afterStep: Boolean)

    /** A light tick, such as the tide turning at midnight. */
    fun tick()

    /** Something heavy lands: the cork going on, the splash (spec §1.5). */
    fun thud()

    /** Writing reaches the character limit (spec §3.4). */
    fun limit()

    fun confirm()

    fun reject()
}

/**
 * The haptics for this screen, unless Settings' Haptics switch is off (G16) or [enabled] is false.
 * Previews get haptics that do nothing.
 */
@Composable
fun rememberHaptics(enabled: Boolean = true): BottlingHaptics {
    val on = enabled && LocalFeelPreferences.current.haptics
    val view = LocalView.current
    val context = LocalContext.current
    val inspecting = LocalInspectionMode.current
    return remember(view, context, on, inspecting) {
        if (!on || inspecting) {
            NoHaptics
        } else {
            VibratorHaptics(view, context.getSystemService(VibratorManager::class.java)?.defaultVibrator)
        }
    }
}

private object NoHaptics : BottlingHaptics {
    override fun shakeStep(step: Int) = Unit
    override fun holdTick() = Unit
    override fun corkPop(afterStep: Boolean) = Unit
    override fun tick() = Unit
    override fun thud() = Unit
    override fun limit() = Unit
    override fun confirm() = Unit
    override fun reject() = Unit
}

private class VibratorHaptics(private val view: View, private val vibrator: Vibrator?) : BottlingHaptics {

    private val canVibrate = vibrator?.hasVibrator() == true

    // A composition has no fallback of its own, so it's only used when every primitive is there.
    private val hasPrimitives = canVibrate && vibrator!!.areAllPrimitivesSupported(
        Composition.PRIMITIVE_LOW_TICK,
        Composition.PRIMITIVE_TICK,
        Composition.PRIMITIVE_QUICK_RISE,
        Composition.PRIMITIVE_CLICK,
    )

    // the thud plays alone, so it needs only its own primitive
    private val hasThud = canVibrate && vibrator!!.areAllPrimitivesSupported(Composition.PRIMITIVE_THUD)

    private val touch = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)

    override fun shakeStep(step: Int) = play(VibrationEffect.EFFECT_TICK) {
        addPrimitive(Composition.PRIMITIVE_LOW_TICK, stepScale(step))
    }

    override fun holdTick() = play(VibrationEffect.EFFECT_TICK) {
        addPrimitive(Composition.PRIMITIVE_TICK)
    }

    // One composition rather than several vibrations, which would cut each other off.
    override fun corkPop(afterStep: Boolean) = play(VibrationEffect.EFFECT_HEAVY_CLICK) {
        if (afterStep) addPrimitive(Composition.PRIMITIVE_LOW_TICK, stepScale(STEPS))
        addPrimitive(Composition.PRIMITIVE_QUICK_RISE)
        addPrimitive(Composition.PRIMITIVE_CLICK)
    }

    override fun tick() = play(VibrationEffect.EFFECT_TICK) {
        addPrimitive(Composition.PRIMITIVE_TICK)
    }

    override fun thud() {
        if (!canVibrate) return
        val effect = if (hasThud) {
            VibrationEffect.startComposition().addPrimitive(Composition.PRIMITIVE_THUD).compose()
        } else {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        }
        vibrator!!.vibrate(effect, touch)
    }

    // SEGMENT_TICK arrived in API 34; the compat constant falls back to CLOCK_TICK before that
    override fun limit() {
        ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.SEGMENT_TICK)
    }

    override fun confirm() {
        ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
    }

    override fun reject() {
        ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.REJECT)
    }

    private fun play(fallback: Int, compose: Composition.() -> Unit) {
        if (!canVibrate) return
        val effect = if (hasPrimitives) {
            VibrationEffect.startComposition().apply(compose).compose()
        } else {
            VibrationEffect.createPredefined(fallback)
        }
        vibrator!!.vibrate(effect, touch)
    }
}

// Each shake ticks harder than the last: 0.3, 0.6, then 0.9 as the cork goes (spec §1.5).
private const val STEPS = 3

private fun stepScale(step: Int): Float = STEP_SCALE * step.coerceIn(1, STEPS)

private const val STEP_SCALE = 0.3f
