package com.scratch.animatingwithcoroutines

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun LottieAnimationView.setLoopBounds(start: Int, end: Int) {
    setMinFrame(start)
    setMaxFrame(end)
}

suspend fun LottieAnimationView.waitForComposition() = suspendCoroutine<LottieComposition> { continuation ->
    addLottieOnCompositionLoadedListener { composition ->
        continuation.resume(composition)
    }
}

suspend fun LottieAnimationView.waitForFrame(targetFrame: Int) = suspendCoroutine<Unit> { continuation ->
    addAnimatorUpdateListener(object: ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator?) {
            if (this@waitForFrame.frame >= targetFrame) {
                removeUpdateListener(this)
                continuation.resume(Unit)
            }
        }
    })
}

suspend fun LottieAnimationView.waitForRepetitions(targetRepetitions: Int) = suspendCancellableCoroutine<Unit> { continuation ->
    val listener = object: AnimatorListenerAdapter() {
        var repetitions = 0
        override fun onAnimationRepeat(animation: Animator?) {
            repetitions += 1
            if (repetitions >= targetRepetitions) {
                removeAnimatorListener(this)
                continuation.resume(Unit)
            }
        }
    }
    continuation.invokeOnCancellation {
        removeAnimatorListener(listener)
    }
    addAnimatorListener(listener)
}
