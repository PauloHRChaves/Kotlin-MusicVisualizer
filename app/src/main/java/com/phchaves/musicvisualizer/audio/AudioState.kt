package com.phchaves.musicvisualizer.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AudioState {
    // Armazena uma lista com as 16 bandas do espectro
    private val _espectro = MutableStateFlow(FloatArray(16))
    val espectro = _espectro.asStateFlow()

    private val _volume = MutableStateFlow(0f)
    val volume = _volume.asStateFlow()

    fun updateEspectro(novasBandas: FloatArray) {
        _espectro.value = novasBandas
        // O volume geral passa a ser a maior batida do momento
        _volume.value = novasBandas.maxOfOrNull { it } ?: 0f
    }
}
