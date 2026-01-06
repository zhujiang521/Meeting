package com.lenovo.levoice.caption

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AudioVisualizerScreen(
    microphoneVolume: Float,
    deviceAudioVolume: Float,
    microphoneWaveform: FloatArray,
    deviceAudioWaveform: FloatArray,
    isCapturing: Boolean,
    recordingStatus: String,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onShowRecordings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        // Landscape layout
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left side - Audio sources
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "双音频采集",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                AudioSourceCard(
                    title = "麦克风音频",
                    volume = microphoneVolume,
                    waveform = microphoneWaveform,
                    color = Color(0xFF2196F3),
                    isActive = isCapturing
                )

                AudioSourceCard(
                    title = "设备音频",
                    volume = deviceAudioVolume,
                    waveform = deviceAudioWaveform,
                    color = Color(0xFF4CAF50),
                    isActive = isCapturing
                )
            }

            // Right side - Controls
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (recordingStatus.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF9C4)
                        )
                    ) {
                        Text(
                            text = recordingStatus,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 11.sp,
                            color = Color(0xFF827717)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (isCapturing) {
                            onStopCapture()
                        } else {
                            onStartCapture()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCapturing) Color(0xFFF44336) else Color(0xFF2196F3)
                    )
                ) {
                    Text(
                        text = if (isCapturing) "停止采集" else "开始采集",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onShowRecordings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isCapturing
                ) {
                    Text(
                        text = "查看录音",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    } else {
        // Portrait layout
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "双音频采集",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            AudioSourceCard(
                title = "麦克风音频",
                volume = microphoneVolume,
                waveform = microphoneWaveform,
                color = Color(0xFF2196F3),
                isActive = isCapturing
            )

            AudioSourceCard(
                title = "设备音频",
                volume = deviceAudioVolume,
                waveform = deviceAudioWaveform,
                color = Color(0xFF4CAF50),
                isActive = isCapturing
            )

            if (recordingStatus.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF9C4)
                    )
                ) {
                    Text(
                        text = recordingStatus,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.sp,
                        color = Color(0xFF827717)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (isCapturing) {
                            onStopCapture()
                        } else {
                            onStartCapture()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCapturing) Color(0xFFF44336) else Color(0xFF2196F3)
                    )
                ) {
                    Text(
                        text = if (isCapturing) "停止采集" else "开始采集",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onShowRecordings,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    enabled = !isCapturing
                ) {
                    Text(
                        text = "查看录音",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AudioSourceCard(
    title: String,
    volume: Float,
    waveform: FloatArray,
    color: Color,
    isActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Status Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                if (isActive) color else Color.Gray,
                                shape = RoundedCornerShape(6.dp)
                            )
                    )
                    Text(
                        text = if (isActive) "采集中" else "未激活",
                        fontSize = 12.sp,
                        color = if (isActive) color else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Volume Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "音量: ",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${(volume * 100).toInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Volume Bar
            LinearProgressIndicator(
                progress = volume,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = color,
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Waveform
            WaveformVisualizer(
                waveform = waveform,
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
        }
    }
}

@Composable
fun WaveformVisualizer(
    waveform: FloatArray,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val barWidth = width / waveform.size
        val maxBarHeight = height / 2

        waveform.forEachIndexed { index, amplitude ->
            val x = index * barWidth
            val barHeight = amplitude * maxBarHeight

            drawLine(
                color = color,
                start = Offset(x, centerY - barHeight),
                end = Offset(x, centerY + barHeight),
                strokeWidth = barWidth * 0.8f,
                cap = StrokeCap.Round
            )
        }
    }
}