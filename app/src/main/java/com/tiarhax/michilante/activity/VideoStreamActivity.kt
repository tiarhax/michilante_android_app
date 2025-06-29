package com.tiarhax.michilante.activity

import android.app.Activity
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.SurfaceView
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tiarhax.michilante.R
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.Media

class VideoStreamActivity : Activity() {
    private lateinit var libVLC: LibVLC
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var surfaceView: SurfaceView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_stream)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val rtspUrl = intent.getStringExtra("rtspUrl") ?: ""


        surfaceView = findViewById(R.id.surfaceView)

        val options = arrayListOf("--rtsp-tcp", "--no-gnutls-system-trust") // Force TCP for RTSP

        libVLC = LibVLC(this, options)
        mediaPlayer = MediaPlayer(libVLC)

        surfaceView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Important: remove the listener to avoid repeated calls
                surfaceView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // Now surfaceView.width and height are valid

                mediaPlayer.vlcVout.setVideoView(surfaceView)
                mediaPlayer.vlcVout.setWindowSize(surfaceView.width, surfaceView.height)
                mediaPlayer.vlcVout.attachViews()
                val media = Media(libVLC, Uri.parse(rtspUrl))

                media.setHWDecoderEnabled(true, false)
                media.addOption(":network-caching=150")
                mediaPlayer.media = media
                mediaPlayer.play()
            }
        })

    }


    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()
        mediaPlayer.vlcVout.detachViews()
        mediaPlayer.release()
        libVLC.release()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // No need to recreate media player — it persists
    }


}