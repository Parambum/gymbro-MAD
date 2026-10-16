package com.gymbro.mad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * The only Activity. Everything else is a composable, and navigation happens
 * inside the one graph in [GymBroApp].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // before super, so the first frame is already edge to edge
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { GymBroApp() }
    }
}
