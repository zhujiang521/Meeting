package com.lenovo.levoice.caption

import android.Manifest
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.lenovo.levoice.caption.ui.theme.MeetingTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AudioViewModel by viewModels()
    private lateinit var mediaProjectionManager: MediaProjectionManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            requestMediaProjection()
        } else {
            Toast.makeText(this, "需要录音权限才能使用该功能", Toast.LENGTH_SHORT).show()
        }
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val mediaProjection = mediaProjectionManager.getMediaProjection(
                result.resultCode,
                result.data!!
            )

            // Start capturing both audio sources
            viewModel.audioCaptureManager.startMicrophoneCapture()
            mediaProjection?.let { viewModel.audioCaptureManager.startDeviceAudioCapture(it) }

            Toast.makeText(this, "开始采集音频", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "需要屏幕录制权限才能采集设备音频", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        setContent {
            MeetingTheme {
                val showRecordingsList by viewModel.showRecordingsList.collectAsState()
                val currentPlayingFile by viewModel.currentPlayingFile.collectAsState()
                val refreshTrigger by viewModel.refreshTrigger.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val showTranscription by viewModel.showTranscription.collectAsState()

                    if (showRecordingsList) {
                        val recordings by remember(refreshTrigger) {
                            derivedStateOf { viewModel.getRecordings() }
                        }
                        val isPlaying by viewModel.audioCaptureManager.isPlaying.collectAsState()
                        val playbackProgress by viewModel.audioCaptureManager.playbackProgress.collectAsState()

                        RecordingsListScreen(
                            recordings = recordings,
                            isPlaying = isPlaying,
                            playbackProgress = playbackProgress,
                            currentPlayingFile = currentPlayingFile,
                            onPlayRecording = { file ->
                                viewModel.playRecording(file)
                            },
                            onStopPlayback = {
                                viewModel.stopPlayback()
                            },
                            onDeleteRecording = { file ->
                                if (viewModel.deleteRecording(file)) {
                                    Toast.makeText(this@MainActivity, "删除成功", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@MainActivity, "删除失败", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onTranscribeRecording = { _ ->
                                viewModel.setShowTranscription(true)
                            },
                            onBack = {
                                viewModel.setShowRecordingsList(false)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )

                        // Transcription dialog
                        if (showTranscription && currentPlayingFile != null) {
                            val transcriptionText by viewModel.speechRecognitionManager.recognitionText.collectAsState()
                            val isRecognizing by viewModel.speechRecognitionManager.isRecognizing.collectAsState()
                            val recognitionError by viewModel.speechRecognitionManager.recognitionError.collectAsState()

                            TranscriptionDialog(
                                file = currentPlayingFile!!,
                                transcriptionText = transcriptionText,
                                isRecognizing = isRecognizing,
                                recognitionError = recognitionError,
                                onStartTranscription = {
                                    // Start playing the audio if not already playing
                                    if (!isPlaying) {
                                        viewModel.playRecording(currentPlayingFile!!)
                                    }
                                    viewModel.startTranscription(currentPlayingFile!!)
                                },
                                onStopTranscription = {
                                    viewModel.stopTranscription()
                                },
                                onDismiss = {
                                    viewModel.setShowTranscription(false)
                                }
                            )
                        }
                    } else {
                        val microphoneVolume by viewModel.audioCaptureManager.microphoneVolume.collectAsState()
                        val deviceAudioVolume by viewModel.audioCaptureManager.deviceAudioVolume.collectAsState()
                        val microphoneWaveform by viewModel.audioCaptureManager.microphoneWaveform.collectAsState()
                        val deviceAudioWaveform by viewModel.audioCaptureManager.deviceAudioWaveform.collectAsState()
                        val isCapturing by viewModel.audioCaptureManager.isCapturing.collectAsState()
                        val recordingStatus by viewModel.audioCaptureManager.recordingStatus.collectAsState()

                        AudioVisualizerScreen(
                            microphoneVolume = microphoneVolume,
                            deviceAudioVolume = deviceAudioVolume,
                            microphoneWaveform = microphoneWaveform,
                            deviceAudioWaveform = deviceAudioWaveform,
                            isCapturing = isCapturing,
                            recordingStatus = recordingStatus,
                            onStartCapture = { checkPermissionsAndStart() },
                            onStopCapture = { stopCapture() },
                            onShowRecordings = { viewModel.setShowRecordingsList(true) },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    private fun checkPermissionsAndStart() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO
        )

        val allPermissionsGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allPermissionsGranted) {
            requestMediaProjection()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun requestMediaProjection() {
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        mediaProjectionLauncher.launch(intent)
    }

    private fun stopCapture() {
        viewModel.audioCaptureManager.stopCapture()
        Toast.makeText(this, "停止采集音频", Toast.LENGTH_SHORT).show()
    }
}