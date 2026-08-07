package com.phchaves.musicvisualizer.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.app.Activity

class AudioCaptureService : Service() {

    private lateinit var captureManager: AudioCaptureManager

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"
        const val CHANNEL_ID = "audio_capture"
        const val ACTION_STOP = "STOP_CAPTURE"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("SERVICE","onCreate")
        captureManager = AudioCaptureManager()
        UdpClient.inicializar()

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Captura de áudio",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SERVICE","onStartCommand")

        if (intent?.action == ACTION_STOP) {
            Log.d("SERVICE","STOP recebido")
            captureManager.stop()
            MediaProjectionHolder.projection?.stop()
            MediaProjectionHolder.projection = null

            // Reseta o espectro enviando um array de 16 posições zeradas
            AudioState.updateEspectro(FloatArray(16))

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Music Visualizer")
            .setContentText("Capturando áudio")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

        startForeground(1, notification)
        Log.d("SERVICE","Foreground iniciado")

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        Log.d("SERVICE","resultCode=$resultCode")

        val data: Intent? = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra<Intent>(EXTRA_DATA)
        }
        Log.d("SERVICE","data=$data")

        if (resultCode == Activity.RESULT_OK && data != null) {
            Log.d("SERVICE", "Criando MediaProjection")
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = manager.getMediaProjection(resultCode, data)
            Log.d("SERVICE", "projection=$projection")

            if (projection == null) {
                Log.d("SERVICE", "Projection é NULL")
                stopSelf()
                return START_NOT_STICKY
            }

            MediaProjectionHolder.projection = projection
            Log.d("SERVICE", "MediaProjection criada")

            serviceScope.launch {
                Log.d("SERVICE", "Chamando captureManager.start()")

                captureManager.start(projection) { bandas ->
                    // Atualiza o espectro completo no estado global
                    AudioState.updateEspectro(bandas)

                    // Envia para o ESP32
                    UdpClient.enviarBandas(bandas)
                }
            }
        } else {
            Log.d("SERVICE", "Permissão inválida ou dados nulos")
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("SERVICE","onDestroy")
        captureManager.stop()
        MediaProjectionHolder.projection?.stop()
        MediaProjectionHolder.projection = null
        UdpClient.fechar()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
