package com.br1ansouza.chromix.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.br1ansouza.chromix.R

/**
 * Efeitos sonoros curtos via SoundPool (baixa latência). Sem música de fundo —
 * apenas feedback pontual, com toggle próprio no HUD.
 */
class GameSounds(context: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val selectId = pool.load(context, R.raw.ball_select, 1)
    private val winId = pool.load(context, R.raw.level_win, 1)

    fun ballSelected() = play(selectId)

    fun levelWon() = play(winId)

    private fun play(soundId: Int) {
        pool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() = pool.release()
}
