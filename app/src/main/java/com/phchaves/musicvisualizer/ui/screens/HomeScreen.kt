package com.phchaves.musicvisualizer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.phchaves.musicvisualizer.viewmodel.MainViewModel
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onStartCapture: () -> Unit
) {
    val volume by vm.volume.collectAsState()
    val espectro by vm.espectro.collectAsState()

    val totalSegments = 18
    val picosAtuais = remember { mutableStateOf(FloatArray(16)) }
    val tempoUltimaAtualizacao = remember { mutableLongStateOf(System.currentTimeMillis()) }

    val tempoAnimacao = rememberInfiniteTransition(label = "RainbowTime")
    val offsetArcoIris by tempoAnimacao.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing), // Muda o ciclo completo a cada 10 segundos
            repeatMode = RepeatMode.Restart
        ),
        label = "RainbowOffset"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Volume Geral: %.2f".format(volume), style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(20.dp))

        // --- EQUALIZADOR GRÁFICO ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Calcula o tempo que passou para aplicar a gravidade no pixel de forma igual em qualquer celular
            val agora = System.currentTimeMillis()
            val deltaTempo = (agora - tempoUltimaAtualizacao.value) / 1000f // em segundos
            tempoUltimaAtualizacao.value = agora

            espectro.forEachIndexed { col, amplitude ->
                val intensidade = amplitude.coerceIn(0f, 1f)

                // LÓGICA DO PIXEL ROXO CADENTE (PEAK HOLD)
                // Se a onda atual subiu mais alto que o pico antigo, o pico gruda no topo da onda atual
                if (intensidade > picosAtuais.value[col]) {
                    picosAtuais.value[col] = intensidade
                } else {
                    // Se a onda desceu, o pixel roxo cai devagar simulando a gravidade física (ajustar o 0.8f para cair mais rápido/lento)
                    picosAtuais.value[col] -= deltaTempo * 0.8f
                    if (picosAtuais.value[col] < 0f) picosAtuais.value[col] = 0f
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val larguraBarra = size.width
                        val alturaTotalDisponivel = size.height
                        val alturaSegmento = (alturaTotalDisponivel / totalSegments) - 2f

                        val segmentosAcesos = (intensidade * totalSegments).toInt()
                        val linhaDoPicoRoxo = (picosAtuais.value[col] * totalSegments).toInt().coerceIn(0, totalSegments - 1)

                        for (linha in 0 until totalSegments) {
                            val posY = alturaTotalDisponivel - ((linha + 1) * (alturaSegmento + 2f))

                            // CÁLCULO DO RAINBOW SUAVE (Usa o sistema HSV de cor)
                            // A altura da linha (0 a 1) para um ângulo do arco-íris (0 a 240 graus)
                            // Somando o 'offsetArcoIris' para as cores se moverem sozinhas de forma psicodélica
                            val proporcaoLinha = linha.toFloat() / totalSegments.toFloat()
                            val matizHue = (proporcaoLinha * 240f + offsetArcoIris) % 360f
                            val corRainbowSuave = Color.hsv(hue = matizHue, saturation = 1f, value = 1f)

                            // Desenhando a onda sonora principal
                            if (linha < segmentosAcesos) {
                                drawRect(
                                    color = corRainbowSuave,
                                    topLeft = androidx.compose.ui.geometry.Offset(0f, posY),
                                    size = androidx.compose.ui.geometry.Size(larguraBarra, alturaSegmento)
                                )
                            }

                            // DESENHANDO O PIXEL ROXO NO LIMITE DA ONDA
                            // Se esta linha for exatamente a linha do pico e estiver acima da onda atual, desenha o pixel roxo
                            if (linha == linhaDoPicoRoxo && linha >= segmentosAcesos && picosAtuais.value[col] > 0.02f) {
                                drawRect(
                                    color = Color(0xFF9C27B0),
                                    topLeft = androidx.compose.ui.geometry.Offset(0f, posY),
                                    size = androidx.compose.ui.geometry.Size(larguraBarra, alturaSegmento)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        Button(
            modifier = Modifier.width(120.dp),
            onClick = { onStartCapture() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
        ) {
            Text("Iniciar captura")
        }

        Spacer(Modifier.height(16.dp))

        Button(
            modifier = Modifier.width(120.dp),
            onClick = { vm.stopCapture() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Parar")
        }
    }
}
