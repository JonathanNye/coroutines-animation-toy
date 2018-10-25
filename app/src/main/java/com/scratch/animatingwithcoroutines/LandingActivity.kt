package com.scratch.animatingwithcoroutines

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class LandingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        findViewById<View>(R.id.activity_landing_bare_button).setOnClickListener {
            startActivity(Intent(this, BareAnimationActivity::class.java))
        }

        findViewById<View>(R.id.activity_landing_looping_button).setOnClickListener {
            startActivity(Intent(this, LoopingAnimationActivity::class.java))
        }

        findViewById<View>(R.id.activity_landing_interactive_button).setOnClickListener {
            startActivity(Intent(this, InteractiveAnimationActivity::class.java))
        }
    }
}
