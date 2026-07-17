package com.br1ansouza.chromix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.br1ansouza.chromix.ui.game.GameScreen
import com.br1ansouza.chromix.ui.home.HomeScreen
import com.br1ansouza.chromix.ui.levels.LevelsScreen
import com.br1ansouza.chromix.ui.theme.ChromixTheme
import com.br1ansouza.chromix.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChromixTheme {
                // ViewModel no escopo da Activity, compartilhado pelas duas telas.
                val gameViewModel: GameViewModel = viewModel()
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "home",
                    enterTransition = {
                        fadeIn(tween(200)) + slideInHorizontally(tween(200)) { it / 8 }
                    },
                    exitTransition = { fadeOut(tween(200)) },
                    popEnterTransition = {
                        fadeIn(tween(200)) + slideInHorizontally(tween(200)) { -it / 8 }
                    },
                    popExitTransition = { fadeOut(tween(200)) },
                ) {
                    composable("home") {
                        val state by gameViewModel.uiState.collectAsState()
                        HomeScreen(
                            currentLevel = state?.levelNumber ?: 1,
                            onStart = { navController.navigate("game") },
                            onOpenLevels = { navController.navigate("levels") },
                        )
                    }
                    composable("game") {
                        GameScreen(
                            viewModel = gameViewModel,
                            onOpenLevels = { navController.navigate("levels") },
                        )
                    }
                    composable("levels") {
                        val state by gameViewModel.uiState.collectAsState()
                        LevelsScreen(
                            currentLevel = state?.levelNumber ?: 1,
                            bestLevelReached = state?.bestLevelReached ?: 1,
                            onLevelSelected = { level ->
                                // Tocar no nível em andamento não reinicia o progresso.
                                if (level != state?.levelNumber) {
                                    gameViewModel.loadLevel(level)
                                }
                                // Sempre cai na tela de jogo, viesse da home ou do jogo.
                                navController.navigate("game") { popUpTo("home") }
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
