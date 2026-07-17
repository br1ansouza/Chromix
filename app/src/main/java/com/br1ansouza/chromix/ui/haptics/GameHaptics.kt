package com.br1ansouza.chromix.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Feedback tátil do jogo. Usa [VibrationEffect] (API 26+) com fallback para o
 * vibrate legado em API 24–25. Intensidades conforme a spec:
 * movimento válido sutil, erro curto, tubo completo médio, vitória em padrão duplo.
 */
class GameHaptics(context: Context) {

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    fun tubeSelected() = oneShot(10)

    fun validMove() = oneShot(12)

    fun invalidMove() = oneShot(30)

    fun tubeCompleted() = oneShot(40)

    fun levelWon() {
        val vibrator = vibrator ?: return
        val timings = longArrayOf(0, 45, 90, 45)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }

    private fun oneShot(durationMs: Long) {
        val vibrator = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }
}
