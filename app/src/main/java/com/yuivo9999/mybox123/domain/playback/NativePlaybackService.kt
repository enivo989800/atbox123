package com.yuivo9999.mybox123.domain.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import java.util.Locale

@OptIn(UnstableApi::class)
class NativePlaybackService(private val context: Context) {

    val bandwidthMeter: DefaultBandwidthMeter by lazy {
        DefaultBandwidthMeter.Builder(context).build()
    }

    /**
     * Builds a high-stability DefaultLoadControl specifically configured to buffer up to
     * 60 seconds ahead for live and VOD streams, with 1.5s fast initial startup and 30s back buffer.
     */
    fun createLiveLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 60_000,
                /* bufferForPlaybackMs = */ 1_500,
                /* bufferForPlaybackAfterRebufferMs = */ 3_000
            )
            .setBackBuffer(
                /* backBufferDurationMs = */ 30_000,
                /* retainBackBufferFromKeyframe = */ true
            )
            .build()
    }

    fun formatSpeed(bitrateEstimate: Long): String {
        if (bitrateEstimate <= 0) return "测量中…"
        val bytesPerSec = bitrateEstimate / 8.0
        return if (bytesPerSec >= 1024 * 1024) {
            String.format(Locale.CHINA, "%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
        } else {
            String.format(Locale.CHINA, "%d KB/s", (bytesPerSec / 1024.0).toLong())
        }
    }

    fun createPlayer(
        url: String,
        headers: Map<String, String> = emptyMap(),
        onPlayerError: (String) -> Unit = {},
    ): ExoPlayer {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setTransferListener(bandwidthMeter)

        if (headers.isNotEmpty()) {
            httpDataSourceFactory.setDefaultRequestProperties(headers)
        }

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val mediaItem = MediaItem.fromUri(url)
        val mediaSource: MediaSource = if (url.contains(".m3u8", ignoreCase = true) || url.contains("hls", ignoreCase = true)) {
            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        }

        return ExoPlayer.Builder(context)
            .setLoadControl(createLiveLoadControl())
            .setBandwidthMeter(bandwidthMeter)
            .build().apply {
                setMediaSource(mediaSource)
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        onPlayerError(error.localizedMessage ?: "播放错误")
                    }
                })
                prepare()
                playWhenReady = true
            }
    }
}
