package com.scratch.animatingwithcoroutines

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun LottieAnimationView.setLoopBounds(start: Int, end: Int) {
    setMinFrame(start)
    setMaxFrame(end)
}

suspend fun LottieAnimationView.suspendForComposition() = suspendCoroutine<LottieComposition> { continuation ->
    addLottieOnCompositionLoadedListener { composition ->
        continuation.resume(composition)
    }
}

suspend fun LottieAnimationView.suspendForFrame(targetFrame: Int) = suspendCoroutine<Unit> { continuation ->
    addAnimatorUpdateListener(object: ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator?) {
            if (this@suspendForFrame.frame >= targetFrame) {
                removeUpdateListener(this)
                continuation.resume(Unit)
            }
        }
    })
}

suspend fun LottieAnimationView.suspendForRepetitions(targetRepetitions: Int) = suspendCancellableCoroutine<Unit> { continuation ->
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

// ====================
// These extensions are specific to the "interactive" variant of the animation
// ====================


suspend fun LottieAnimationView.suspendForRepetitions(targetRepetitions: Int, onRepeat: ((Int) -> Unit)) = suspendCancellableCoroutine<Unit> { continuation ->
    val listener = object: AnimatorListenerAdapter() {
        var repetitions = 0
        override fun onAnimationRepeat(animation: Animator?) {
            repetitions += 1
            onRepeat.invoke(repetitions)
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

suspend fun LottieAnimationView.suspendForRepetitionsWithSuspendingCallback(targetRepetitions: Int, onRepeat: suspend (Int) -> Unit) = coroutineScope {
    suspendCancellableCoroutine<Unit> { continuation ->
        val listener = object : AnimatorListenerAdapter() {
            var repetitions = 0
            override fun onAnimationRepeat(animation: Animator?) {
                repetitions += 1
                val listenerRef = this // AnimatorListenerAdapter
                launch {
                    onRepeat.invoke(repetitions)
                    if (repetitions >= targetRepetitions) {
                        removeAnimatorListener(listenerRef)
                        continuation.resume(Unit)
                    }
                }
            }
        }
        continuation.invokeOnCancellation {
            removeAnimatorListener(listener)
        }
        addAnimatorListener(listener)
    }
}

suspend fun View.suspendForClick(animation: LottieAnimationView) = suspendCoroutine<Unit> { continuation ->
    visibility = VISIBLE
    animation.pauseAnimation()
    setOnClickListener { v ->
        v.visibility = INVISIBLE
        setOnClickListener(null)
        animation.resumeAnimation()
        continuation.resume(Unit)
    }
}
