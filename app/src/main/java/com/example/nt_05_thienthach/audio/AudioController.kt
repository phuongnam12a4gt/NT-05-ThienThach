package com.example.nt_05_thienthach.audio

import android.content.Context
import android.media.*
import android.os.*
import com.example.nt_05_thienthach.data.Settings
import com.example.nt_05_thienthach.game.GameEvent
import kotlin.math.*

/** Original synthesized ambient loop; no bundled third party recordings. */
class AudioController(private val context: Context, private val onFocusLost: () -> Unit) {
    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    @Volatile private var track: AudioTrack? = null
    private var released = false
    private val tones = ToneGenerator(AudioManager.STREAM_MUSIC, 45)
    private var focused = false
    private var running = false
    private var settings = Settings()
    private val listener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change != AudioManager.AUDIOFOCUS_GAIN) { focused = false; stop(); onFocusLost() }
    }
    private val focusRequest = if (Build.VERSION.SDK_INT >= 26) AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes).setOnAudioFocusChangeListener(listener).build() else null
    fun prepare() {
        val rate = 22050
        val samples = ShortArray(rate * 8)
        val notes = doubleArrayOf(130.8128, 164.8138, 195.9977, 246.9417, 195.9977, 164.8138, 146.8324, 195.9977)
        for (i in samples.indices) {
            val t = i.toDouble() / rate
            val beat = t % 1.0
            val envelope = sin(PI * beat).pow(2)
            val frequency = notes[t.toInt() % notes.size]
            val melody = sin(2 * PI * frequency * t) * .7 + sin(2 * PI * frequency * 2 * t) * .15
            val pad = sin(2 * PI * 65.4064 * t) * .12
            val fade = minOf(1.0, t * 8, (8 - t) * 8)
            samples[i] = ((melody * envelope + pad) * fade * 2800).toInt().toShort()
        }
        val audio = AudioTrack.Builder().setAudioAttributes(attributes)
            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(samples.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
        audio.write(samples, 0, samples.size)
        audio.setLoopPoints(0, samples.size, -1)
        synchronized(this) { if (released) audio.release() else track = audio }
    }
    fun update(active: Boolean, newSettings: Settings) {
        settings = newSettings
        running = active
        if (!active || (!settings.music && !settings.sound)) { stop(stopEffects = !settings.sound); return }
        if (!focused) {
            focused = if (Build.VERSION.SDK_INT >= 26) manager.requestAudioFocus(focusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            else @Suppress("DEPRECATION") (manager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            if (!focused) { onFocusLost(); return }
        }
        if (settings.music) track?.let { if (it.playState != AudioTrack.PLAYSTATE_PLAYING) it.play() }
        else track?.pause()
    }
    fun event(event: GameEvent) {
        if (!running || !focused) return
        if (settings.sound) tones.startTone(when (event) {
            GameEvent.PICKUP, GameEvent.NEAR_MISS, GameEvent.MILESTONE -> ToneGenerator.TONE_PROP_ACK
            GameEvent.HIT, GameEvent.WAVE -> ToneGenerator.TONE_PROP_NACK
            GameEvent.SHIELD_BREAK, GameEvent.PULSE -> ToneGenerator.TONE_PROP_BEEP2
            GameEvent.GAME_OVER -> ToneGenerator.TONE_SUP_ERROR
        }, if (event == GameEvent.GAME_OVER) 250 else 90)
        if (settings.vibration && (event == GameEvent.HIT || event == GameEvent.SHIELD_BREAK)) {
            val vibrator = if (Build.VERSION.SDK_INT >= 31) (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                else @Suppress("DEPRECATION") vibrator.vibrate(40)
            }
        }
    }
    fun purchase() { if (settings.sound) tones.startTone(ToneGenerator.TONE_PROP_ACK, 100) }
    fun stop(stopEffects: Boolean = true) {
        running = false
        track?.let { if (it.state == AudioTrack.STATE_INITIALIZED) it.pause() }
        if (stopEffects) tones.stopTone()
        if (focused) {
            if (Build.VERSION.SDK_INT >= 26) manager.abandonAudioFocusRequest(focusRequest!!)
            else @Suppress("DEPRECATION") manager.abandonAudioFocus(listener)
            focused = false
        }
    }
    fun release() { synchronized(this) { released = true; stop(); track?.release(); track = null; tones.release() } }
}


