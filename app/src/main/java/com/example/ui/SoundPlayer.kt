package com.example.ui

import android.content.Context
import android.media.MediaPlayer
import com.example.R

object SoundPlayer {
    private val activePlayers = mutableSetOf<MediaPlayer>()

    fun playSuccess(context: Context) {
        playSound(context, R.raw.success)
    }

    fun playBeep(context: Context) {
        playSound(context, R.raw.beep)
    }

    fun playEvm(context: Context) {
        playSound(context, R.raw.evm, 3000L)
    }

    fun playCrowdCheering(context: Context) {
        playSound(context, R.raw.crowd_cheering)
    }

    private fun playSound(context: Context, resId: Int, durationMs: Long? = null) {
        try {
            if (resId != 0) {
                MediaPlayer.create(context, resId)?.apply {
                    activePlayers.add(this)
                    setOnCompletionListener {
                        activePlayers.remove(it)
                        it.release()
                    }
                    start()
                    if (durationMs != null) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            try {
                                if (isPlaying) {
                                    stop()
                                }
                                activePlayers.remove(this)
                                release()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, durationMs)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
