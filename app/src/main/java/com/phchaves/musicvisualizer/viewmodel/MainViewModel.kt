package com.phchaves.musicvisualizer.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import com.phchaves.musicvisualizer.audio.AudioCaptureService
import com.phchaves.musicvisualizer.audio.AudioState

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    val volume = AudioState.volume
    val espectro = AudioState.espectro

    fun stopCapture() {
        val context = getApplication<Application>()
        val intent = Intent(context, AudioCaptureService::class.java).apply {
            action = AudioCaptureService.ACTION_STOP
        }
        context.startForegroundService(intent)
    }
}
