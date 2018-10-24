package com.scratch.animatingwithcoroutines

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LoopingAnimationActivity : AppCompatActivity(), CoroutineScope {

    companion object {
        const val START_LOOP_END_FRAME = 60
        const val END_LOOP_START_FRAME = 205
    }

    private lateinit var job: Job
    private var animationJob: Job? = null

    override val coroutineContext: CoroutineContext
        get() = job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
        setContentView(R.layout.activity_main)

        val animationView = findViewById<LottieAnimationView>(R.id.activity_main_animation)

        findViewById<View>(R.id.activity_main_reset).setOnClickListener {
            startAnimationLooping(animationView)
        }

        startAnimationLooping(animationView)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun startAnimationLooping(animation: LottieAnimationView) {
        animationJob?.cancel()
        animationJob = launch(context = Main) {
            playLoopedAnimation(animation)
        }
    }

    private fun LottieAnimationView.setLoopBounds(start: Int, end: Int) {
        setMinFrame(start)
        setMaxFrame(end)
    }

    private suspend fun LottieAnimationView.waitForComposition() = suspendCoroutine<LottieComposition> { continuation ->
        addLottieOnCompositionLoadedListener { composition ->
            continuation.resume(composition)
        }
    }

    private suspend fun LottieAnimationView.waitForFrame(targetFrame: Int) = suspendCoroutine<Unit> { continuation ->
        addAnimatorUpdateListener(object: ValueAnimator.AnimatorUpdateListener {
            override fun onAnimationUpdate(animation: ValueAnimator?) {
                if (this@waitForFrame.frame >= targetFrame) {
                    removeUpdateListener(this)
                    continuation.resume(Unit)
                }
            }
        })
    }

    private suspend fun LottieAnimationView.waitForRepetitions(targetRepetitions: Int) = suspendCancellableCoroutine<Unit> { continuation ->
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

    private suspend fun playLoopedAnimation(animation: LottieAnimationView) = coroutineScope {
        // Get the animation
        val composition = animation.composition ?: animation.waitForComposition()
        val endLoopEndFrame = composition.durationFrames.toInt() - 1

        // Restart
        animation.frame = 0
        animation.setLoopBounds(0, START_LOOP_END_FRAME)
        animation.playAnimation()

        // Box wiggles
        val repeatJob = launch {
            animation.waitForRepetitions(2)
        }
        animation.setOnClickListener {
            repeatJob.cancel()
        }
        repeatJob.join()
        animation.setOnClickListener(null)

        // Box opens, coin flies out
        animation.setMaxFrame(endLoopEndFrame)

        // Coin floating
        animation.waitForFrame(END_LOOP_START_FRAME)
        animation.setMinFrame(END_LOOP_START_FRAME)
    }

    private fun dlog(s: String) {
        Log.d("DEBUG", s)
    }
}
