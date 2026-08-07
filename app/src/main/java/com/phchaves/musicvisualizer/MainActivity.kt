package com.phchaves.musicvisualizer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phchaves.musicvisualizer.audio.AudioCaptureService
import com.phchaves.musicvisualizer.ui.screens.HomeScreen
import com.phchaves.musicvisualizer.ui.theme.MusicVisualizerTheme
import com.phchaves.musicvisualizer.viewmodel.MainViewModel
import com.phchaves.musicvisualizer.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {

    // Captura a resposta da janela de gravação de tela do sistema
    private val captureLauncher = registerForActivityResult( ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = Intent(this, AudioCaptureService::class.java).apply {
                putExtra(AudioCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(AudioCaptureService.EXTRA_DATA, result.data)
            }
            startForegroundService(intent)
        } else {
            Toast.makeText(this, "Permissão de captura negada", Toast.LENGTH_SHORT).show()
        }
    }

    // Pede a permissão de áudio obrigatória em tempo de execução
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Se o usuário aceitou a permissão
                dispararJanelaMediaProjection()
            } else {
                Toast.makeText(this, "O app precisa da permissão de áudio para funcionar", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicVisualizerTheme {
                val vm: MainViewModel = viewModel(factory = MainViewModelFactory(application))

                HomeScreen(vm,onStartCapture = {verificarEPedirPermissoes()})
            }
        }
    }

    private fun verificarEPedirPermissoes() {
        // Verifica se a permissão de gravação de áudio já foi aceita anteriormente
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            dispararJanelaMediaProjection()
        } else {
            // Se não foi aceita, dispara o pedido nativo do Android
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun dispararJanelaMediaProjection() {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        captureLauncher.launch(manager.createScreenCaptureIntent())
    }
}
