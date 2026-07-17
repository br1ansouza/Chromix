package com.br1ansouza.chromix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.br1ansouza.chromix.ui.game.GameScreen
import com.br1ansouza.chromix.ui.theme.ChromixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChromixTheme {
                GameScreen()
            }
        }
    }
}
