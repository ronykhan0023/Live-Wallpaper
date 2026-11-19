package com.example.engine

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/**
 * Enforces strict audio-track mute isolation for live wallpaper and preview players.
 * Setting only player.volume = 0f keeps an AudioTrack open in Android's AudioFlinger,
 * which OEM audio policies (Vivo/MediaTek/etc.) can reset to audible gain when a phone
 * or WhatsApp call ends or another app releases audio focus.
 * Disabling C.TRACK_TYPE_AUDIO prevents an AudioTrack from opening at all when muted.
 */
object ExoPlayerAudioHelper {

    @OptIn(UnstableApi::class)
    fun configureBuilderAudio(builder: ExoPlayer.Builder): ExoPlayer.Builder {
        return builder.setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ false)
    }

    fun applyMuteState(player: Player, isMuted: Boolean) {
        applyAudioState(player = player, isMuted = isMuted, volume = 1.0f)
    }

    fun applyAudioState(player: Player, isMuted: Boolean, volume: Float = 1.0f) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        val effectivelyMuted = isMuted || clampedVolume <= 0.005f
        val targetVolume = if (effectivelyMuted) 0f else clampedVolume
        if (kotlin.math.abs(player.volume - targetVolume) > 0.001f) {
            player.volume = targetVolume
        }
        val currentParams = player.trackSelectionParameters
        val audioDisabled = C.TRACK_TYPE_AUDIO in currentParams.disabledTrackTypes
        if (audioDisabled != effectivelyMuted) {
            player.trackSelectionParameters = currentParams
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, effectivelyMuted)
                .build()
        }
        if (effectivelyMuted && player.volume != 0f) {
            player.volume = 0f
        }
    }
}
