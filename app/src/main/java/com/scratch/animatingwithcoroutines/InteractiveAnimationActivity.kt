package com.scratch.animatingwithcoroutines

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.View.INVISIBLE
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class InteractiveAnimationActivity : AppCompatActivity(), CoroutineScope {

    companion object {
        const val START_LOOP_END_FRAME = 60
        const val END_LOOP_START_FRAME = 205
    }

    private lateinit var job: Job
    private var animationJob: Job? = null

    private lateinit var debugText: TextView
    private lateinit var continueButton: View

    override val coroutineContext: CoroutineContext
        get() = job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
        setContentView(R.layout.activity_interactive_animation)

        val animationView = findViewById<LottieAnimationView>(R.id.activity_main_animation)

        debugText = findViewById(R.id.debug_text)
        continueButton = findViewById(R.id.continue_button)

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
        animationJob = launch(context = Dispatchers.Main) {
            playLoopedAnimation(animation)
        }
    }

    private suspend fun playLoopedAnimation(animation: LottieAnimationView) = coroutineScope {
        // Get the animation
        continueButton.visibility = INVISIBLE
        val composition = animation.composition ?: animation.suspendForComposition()
        val endLoopEndFrame = composition.durationFrames.toInt() - 1

        // Restart
        debug("starting")
        animation.frame = 0
        animation.setLoopBounds(0, START_LOOP_END_FRAME)
        animation.playAnimation()
        continueButton.suspendForClick(animation)

        // Box wiggles
        val repeatJob = launch {

            // Boring version, can't suspend in block
            //animation.suspendForRepetitions(2) { rep ->
            //      debug("repetition $rep")
            //  }

            // Spicy version, suspend all the way down!
            animation.suspendForRepetitionsWithSuspendingCallback(2) { rep ->
                debug("repetition $rep")
                continueButton.suspendForClick(animation)
            }
        }
        animation.setOnClickListener { repeatJob.cancel() }
        repeatJob.join()
        animation.setOnClickListener(null)

        // Box opens, coin flies out
        animation.setMaxFrame(endLoopEndFrame)
        animation.suspendForFrame(START_LOOP_END_FRAME)
        debug("box opening")
        continueButton.suspendForClick(animation)
        animation.resumeAnimation()

        // Coin floating
        animation.suspendForFrame(END_LOOP_START_FRAME)
        debug("coin floating")
        continueButton.suspendForClick(animation)
        animation.resumeAnimation()
        animation.setMinFrame(END_LOOP_START_FRAME)
    }

    private fun debug(text: String) {
        debugText.text = text
    }
}
