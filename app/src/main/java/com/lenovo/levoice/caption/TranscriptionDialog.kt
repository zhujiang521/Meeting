package com.lenovo.levoice.caption

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.File

@Composable
fun TranscriptionDialog(
    file: File,
    transcriptionText: String,
    isRecognizing: Boolean,
    recognitionError: String?,
    onStartTranscription: () -> Unit,
    onStopTranscription: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "语音转文字",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onDismiss) {
                        Text("关闭")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // File name
                Text(
                    text = file.nameWithoutExtension.removePrefix("mixed_"),
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status indicator
                if (isRecognizing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "识别中...",
                            fontSize = 12.sp,
                            color = Color(0xFF2196F3)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Error message
                if (recognitionError != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFEBEE)
                        )
                    ) {
                        Text(
                            text = recognitionError,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp,
                            color = Color(0xFFC62828)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Transcription text
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        if (transcriptionText.isEmpty() && !isRecognizing && recognitionError == null) {
                            Text(
                                text = "点击\"开始识别\"按钮开始语音转文字\n\n注意：需要在播放录音的同时进行识别",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            Text(
                                text = transcriptionText.ifEmpty { "" },
                                fontSize = 14.sp,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Control buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (isRecognizing) {
                                onStopTranscription()
                            } else {
                                onStartTranscription()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecognizing) Color(0xFFF44336) else Color(0xFF2196F3)
                        )
                    ) {
                        Text(if (isRecognizing) "停止识别" else "开始识别")
                    }
                }
            }
        }
    }
}