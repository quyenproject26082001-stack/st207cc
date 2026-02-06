package com.cat.emoji.fpf.maker.core.helper

import android.content.Context
import android.media.MediaPlayer
import com.cat.emoji.fpf.maker.R

object MusicHelper {
    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false

    fun init(context: Context) {
        // Music disabled
//        if (mediaPlayer == null) {
//            mediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.sound)
//            mediaPlayer?.isLooping = true
//            isPrepared = true
//        }
    }

    fun play() {
        // Music disabled
//        if (isPrepared && mediaPlayer?.isPlaying == false) {
//            mediaPlayer?.start()
//        }
    }

    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        isPrepared = false
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPrepared = false
    }
}
