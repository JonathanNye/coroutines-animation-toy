package com.scratch.animatingwithcoroutines

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.suspendCoroutine

class LoopingAnimationActivity : AppCompatActivity(), CoroutineScope {

    companion object {
        const val START_LOOP_END_FRAME = 60
        const val END_LOOP_START_FRAME = 205
    }

    private lateinit var job: Job
    private var animationJob: Job? = null

    override val coroutineContext: CoroutineContext
        get() = Main + job

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
        animationJob = launch {
            playLoopedAnimation(animation)
        }
    }

    private suspend fun playLoopedAnimation(animation: LottieAnimationView) = coroutineScope {
        // Get the animation
        val composition = animation.composition ?: animation.suspendForComposition()
        val endLoopEndFrame = composition.durationFrames.toInt() - 1

        // Restart
        animation.frame = 0
        animation.setLoopBounds(0, START_LOOP_END_FRAME)
        animation.playAnimation()

        // Box wiggles
        val repeatJob = launch {
            animation.suspendForRepetitions(2)
        }
        animation.setOnClickListener { repeatJob.cancel() }
        repeatJob.join()
        animation.setOnClickListener(null)

        // Box opens, coin flies out
        animation.setMaxFrame(endLoopEndFrame)

        // Coin floating
        animation.suspendForFrame(END_LOOP_START_FRAME)
        animation.setMinFrame(END_LOOP_START_FRAME)
    }
}
