package com.yourname.moneypilot.ui.theme.motion

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

object MotionConstants {
    // Timings
    const val DurationButton = 120
    const val DurationCard = 200
    const val DurationScreen = 250
    const val DurationTab = 200
    const val DurationFAB = 180
    const val DurationDialog = 220
    const val StaggerDelay = 50

    // Easing
    val DefaultEasing: Easing = FastOutSlowInEasing
    val EnteringEasing: Easing = LinearOutSlowInEasing
}

fun <T> motionTween(duration: Int = MotionConstants.DurationCard) = 
    tween<T>(durationMillis = duration, easing = MotionConstants.DefaultEasing)

val FadeThroughTransition = fadeIn(animationSpec = tween(MotionConstants.DurationScreen)) togetherWith 
    fadeOut(animationSpec = tween(MotionConstants.DurationScreen))

val SharedAxisXForward = (slideInHorizontally(animationSpec = motionTween(MotionConstants.DurationScreen)) { it / 10 } + fadeIn(animationSpec = motionTween(MotionConstants.DurationScreen))) togetherWith
    (slideOutHorizontally(animationSpec = motionTween(MotionConstants.DurationScreen)) { -it / 10 } + fadeOut(animationSpec = motionTween(MotionConstants.DurationScreen)))

val SharedAxisXBackward = (slideInHorizontally(animationSpec = motionTween(MotionConstants.DurationScreen)) { -it / 10 } + fadeIn(animationSpec = motionTween(MotionConstants.DurationScreen))) togetherWith
    (slideOutHorizontally(animationSpec = motionTween(MotionConstants.DurationScreen)) { it / 10 } + fadeOut(animationSpec = motionTween(MotionConstants.DurationScreen)))
