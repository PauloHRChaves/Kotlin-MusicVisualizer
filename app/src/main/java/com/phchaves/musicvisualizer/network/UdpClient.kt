package com.phchaves.musicvisualizer.audio

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object UdpClient {
    private var socket: DatagramSocket? = null
    private var address: InetAddress? = null
    private const val PORT = 1234
    private const val ESP32_IP = "192.168.4.1"

    fun inicializar() {
        if (socket == null) {
            try {
                socket = DatagramSocket()
                address = InetAddress.getByName(ESP32_IP)
            } catch (e: Exception) {
                android.util.Log.e("UDP", "Erro ao iniciar Socket: ${e.message}")
            }
        }
    }

    fun enviarBandas(bandas: FloatArray) {
        val currentSocket = socket ?: return
        val currentAddress = address ?: return

        try {
            // Envia 16 bytes (1 byte por coluna representando a altura)
            val bufferSaida = ByteArray(bandas.size)

            for (i in bandas.indices) {
                // Converte a amplitude (0.0 a 1.0) em um valor de altura (0 a 255)
                bufferSaida[i] = (bandas[i] * 255).toInt().coerceIn(0, 255).toByte()
            }

            val pacote = DatagramPacket(bufferSaida, bufferSaida.size, currentAddress, PORT)
            currentSocket.send(pacote)
        } catch (e: Exception) {
            // Ignora erros
        }
    }

    fun fechar() {
        socket?.close()
        socket = null
        address = null
    }
}
