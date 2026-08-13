package com.phchaves.musicvisualizer.audio

import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow

class AudioCaptureManager {
    private val bandasSuaves = FloatArray(10)
    private var ganhoAdaptativo = 1.0f

    private var audioRecord: AudioRecord? = null
    @Volatile private var running = false

    suspend fun start(
        projection: MediaProjection,
        onAudioData: (FloatArray) -> Unit
    ) = withContext(Dispatchers.IO) {

        stop()

        val sampleRate = 44100

        val config = AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(android.media.AudioAttributes.USAGE_GAME)
            .addMatchingUsage(android.media.AudioAttributes.USAGE_UNKNOWN)
            .build()

        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()

        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        android.util.Log.d("AUDIO", "buffer=$bufferSize")

        try {
            audioRecord = AudioRecord.Builder()
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setAudioPlaybackCaptureConfig(config)
                .build()

            android.util.Log.d("AUDIO", "AudioRecord criado")
        } catch(e: SecurityException){
            android.util.Log.e("AUDIO", "Sem permissão: ${e.message}")
            return@withContext
        } catch(e: Exception){
            android.util.Log.e("AUDIO", "Erro criando AudioRecord: ${e.message}")
            return@withContext
        }

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            android.util.Log.e("AUDIO", "Falha ao inicializar AudioRecord")
            return@withContext
        }

        android.util.Log.d("AUDIO", "AudioRecord inicializado")

        val buffer = ShortArray(bufferSize / 2)

        try {
            audioRecord?.startRecording()
            android.util.Log.d("AUDIO", "Captura iniciada")
            running = true
        } catch(e: Exception){
            android.util.Log.e("AUDIO", "Erro startRecording: ${e.message}")
            return@withContext
        }

        CoroutineScope(Dispatchers.IO).launch {
            val fftSize = 1024
            val pcmBuffer = FloatArray(fftSize)
            var pcmIndex = 0

            while (running) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0

                if (read > 0) {
                    for (i in 0 until read) {
                        pcmBuffer[pcmIndex] = buffer[i] / 32768f
                        pcmIndex++

                        if (pcmIndex >= fftSize) {
                            val fftInput = pcmBuffer.clone()

                            // CHAMADA do FFTProcessor
                            val magnitudes = FftProcessor.calcular(fftInput)

                            val numBandas = 10
                            val bandasBrutas = FloatArray(numBandas)

                            val limitesFrequencia = intArrayOf(
                                2,   4,   7,   13,  23,  40,  70,  122, 250, 512
                            )

                            var maiorFrequenciaDoMomento = 0.0f

                            for (b in 0 until numBandas) {
                                val inicio = if (b == 0) 0 else limitesFrequencia[b - 1]
                                val fim = limitesFrequencia[b].coerceAtMost(magnitudes.size)

                                var soma = 0f
                                var contagem = 0

                                for (j in inicio until fim) {
                                    soma += magnitudes[j]
                                    contagem++
                                }

                                if (contagem > 0) {
                                    val media = soma / contagem

                                    // Curva de agudos suavizada para 10 bandas
                                    val ganhoAgudos = 1.20.pow(b.toDouble()).toFloat()
                                    val equalizacaoOuvido = 1.0f * ganhoAgudos

                                    val sinalLinear = media * equalizacaoOuvido
                                    bandasBrutas[b] = kotlin.math.sqrt(sinalLinear.toDouble()).toFloat() * 0.5f

                                    if (bandasBrutas[b] > maiorFrequenciaDoMomento) {
                                        maiorFrequenciaDoMomento = bandasBrutas[b]
                                    }
                                }
                            }

                            // GANHO AUTOMÁTICO ADAPTATIVO MAIS AGRESSIVO
                            if (maiorFrequenciaDoMomento * ganhoAdaptativo > 1.1f) {
                                ganhoAdaptativo -= (ganhoAdaptativo - (1.1f / maiorFrequenciaDoMomento)) * 0.7f
                            } else {
                                ganhoAdaptativo += (1.1f - (maiorFrequenciaDoMomento * ganhoAdaptativo)) * 0.01f
                            }
                            ganhoAdaptativo = ganhoAdaptativo.coerceIn(0.01f, 30.0f)

                            val bandasFinais = FloatArray(numBandas)
                            for (b in 0 until numBandas) {
                                var valorNormalizado = bandasBrutas[b] * ganhoAdaptativo

                                // COMPRESSÃO DE TETO (Atenuação logarítmica suave para não bater em 1.00 direto)
                                if (valorNormalizado > 0.5f) {
                                    if (valorNormalizado > 0.8f) {
                                        valorNormalizado = 0.68f + ((valorNormalizado - 0.8f) * 0.35f)
                                    } else {
                                        valorNormalizado = 0.5f + ((valorNormalizado - 0.5f) * 0.6f)
                                    }
                                }

                                // Suavidade de decaimento
                                if (valorNormalizado > bandasSuaves[b]) {
                                    bandasSuaves[b] = valorNormalizado
                                } else {
                                    bandasSuaves[b] -= (bandasSuaves[b] - valorNormalizado) * 0.25f
                                }

                                // Trava de segurança final
                                bandasFinais[b] = bandasSuaves[b].coerceIn(0.0f, 1.0f)
                            }

                            onAudioData(bandasFinais)
                            pcmIndex = 0

                        }
                    }
                }
            }
        }
    }



    fun stop(){
        running = false
        try {
            audioRecord?.stop()
        } catch(_: Exception){}

        audioRecord?.release()
        audioRecord = null
        android.util.Log.d("AUDIO", "Captura interrompida com sucesso")
    }
}
