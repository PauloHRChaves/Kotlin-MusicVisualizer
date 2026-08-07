package com.phchaves.musicvisualizer.audio

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.sqrt

object FftProcessor {
    // O tamanho do buffer para a FFT deve ser obrigatoriamente potência de 2 (ex: 512, 1024, 2048)
    fun calcular(real: FloatArray): FloatArray {
        val n = real.size
        if (n <= 1) return real

        val imag = FloatArray(n)

        var i = 0
        for (j in 1 until n - 1) {
            var bit = n shr 1
            while (i and bit != 0) {
                i = i xor bit
                bit = bit shr 1
            }
            i = i xor bit
            if (j < i) {
                val tempR = real[j]; real[j] = real[i]; real[i] = tempR
                val tempI = imag[j]; imag[j] = imag[i]; imag[i] = tempI
            }
        }

        var len = 2
        while (len <= n) {
            val angle = -2.0 * PI / len
            val wlenR = cos(angle).toFloat()
            val wlenI = sin(angle).toFloat()

            for (m in 0 until n step len) {
                var wR = 1.0f
                var wI = 0.0f
                val len2 = len shr 1
                for (j in 0 until len2) {
                    val uR = real[m + j]
                    val uI = imag[m + j]
                    val vR = real[m + j + len2] * wR - imag[m + j + len2] * wI
                    val vI = real[m + j + len2] * wI + imag[m + j + len2] * wR

                    real[m + j] = uR + vR
                    imag[m + j] = uI + vI
                    real[m + j + len2] = uR - vR
                    imag[m + j + len2] = uI - vI

                    val nextWR = wR * wlenR - wI * wlenI
                    wI = wR * wlenI + wI * wlenR
                    wR = nextWR
                }
            }
            len = len shl 1
        }

        // Calcula as magnitudes (frequências puras)
        val magnitudes = FloatArray(n / 2)
        for (k in 0 until n / 2) {
            magnitudes[k] = sqrt(real[k] * real[k] + imag[k] * imag[k])
        }
        return magnitudes
    }
}