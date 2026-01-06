package com.lenovo.levoice.caption
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordingsListScreen(
    recordings: List<File>,
    isPlaying: Boolean,
    playbackProgress: Float,
    currentPlayingFile: File?,
    onPlayRecording: (File) -> Unit,
    onStopPlayback: () -> Unit,
    onDeleteRecording: (File) -> Unit,
    onTranscribeRecording: (File) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var recordingsList by remember { mutableStateOf(recordings) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(recordings) {
        recordingsList = recordings
    }

    if (showDeleteDialog && fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这个录音吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        fileToDelete?.let { onDeleteRecording(it) }
                        showDeleteDialog = false
                        fileToDelete = null
                    }
                ) {
                    Text("删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "录音列表",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onBack) {
                Text("返回")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无录音",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = recordingsList,
                    key = { it.absolutePath }
                ) { file ->
                    RecordingItem(
                        file = file,
                        isPlaying = isPlaying && currentPlayingFile == file,
                        playbackProgress = if (currentPlayingFile == file) playbackProgress else 0f,
                        onPlay = { onPlayRecording(file) },
                        onStop = onStopPlayback,
                        onDelete = {
                            fileToDelete = file
                            showDeleteDialog = true
                        },
                        onTranscribe = { onTranscribeRecording(file) }
                    )
                }
            }
        }
    }
}

@Composable
fun RecordingItem(
    file: File,
    isPlaying: Boolean,
    playbackProgress: Float,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    onTranscribe: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.nameWithoutExtension.removePrefix("mixed_"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatFileInfo(file),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { if (isPlaying) onStop() else onPlay() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isPlaying) Color(0xFFF44336) else Color(0xFF2196F3)
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "停止" else "播放",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onTranscribe,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFF9C27B0)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "转文字",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFFFF5252)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = Color.White
                        )
                    }
                }
            }

            if (isPlaying) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = playbackProgress,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF2196F3)
                )
                Text(
                    text = "${(playbackProgress * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private fun formatFileInfo(file: File): String {
    val sizeKB = file.length() / 1024
    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        .format(Date(file.lastModified()))
    return "$date · ${sizeKB}KB"
}