package com.lenovo.levoice.caption

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

class AudioCaptureManager {
    companion object {
        private const val TAG = "AudioCaptureManager"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var microphoneRecorder: AudioRecord? = null
    private var deviceAudioRecorder: AudioRecord? = null
    private var microphoneJob: Job? = null
    private var deviceAudioJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _microphoneVolume = MutableStateFlow(0f)
    val microphoneVolume: StateFlow<Float> = _microphoneVolume

    private val _deviceAudioVolume = MutableStateFlow(0f)
    val deviceAudioVolume: StateFlow<Float> = _deviceAudioVolume

    private val _microphoneWaveform = MutableStateFlow(FloatArray(100))
    val microphoneWaveform: StateFlow<FloatArray> = _microphoneWaveform

    private val _deviceAudioWaveform = MutableStateFlow(FloatArray(100))
    val deviceAudioWaveform: StateFlow<FloatArray> = _deviceAudioWaveform

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing

    private var microphoneOutputStream: FileOutputStream? = null
    private var deviceAudioOutputStream: FileOutputStream? = null
    private var recordingDir: File? = null

    private val _recordingStatus = MutableStateFlow("")
    val recordingStatus: StateFlow<String> = _recordingStatus

    private var currentMicFile: File? = null
    private var currentDeviceFile: File? = null

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress

    @SuppressLint("MissingPermission")
    fun startMicrophoneCapture() {
        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )

            microphoneRecorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            // Create output file for microphone audio
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            currentMicFile = File(recordingDir, "microphone_$timestamp.pcm")
            microphoneOutputStream = FileOutputStream(currentMicFile)

            microphoneRecorder?.startRecording()

            microphoneJob = scope.launch {
                val buffer = ShortArray(bufferSize / 2)
                val byteBuffer = ByteArray(bufferSize)
                while (_isCapturing.value && microphoneRecorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = microphoneRecorder?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        processAudioData(buffer, read, true)

                        // Save to file
                        for (i in 0 until read) {
                            byteBuffer[i * 2] = (buffer[i].toInt() and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = ((buffer[i].toInt() shr 8) and 0xFF).toByte()
                        }
                        microphoneOutputStream?.write(byteBuffer, 0, read * 2)
                    }
                }
            }

            updateRecordingStatus()
            Log.d(TAG, "Microphone capture started, saving to: ${currentMicFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting microphone capture", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun startDeviceAudioCapture(mediaProjection: MediaProjection) {
        try {
            _isCapturing.value = true

            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )

            val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(android.media.AudioAttributes.USAGE_GAME)
                .addMatchingUsage(android.media.AudioAttributes.USAGE_UNKNOWN)
                .build()

            deviceAudioRecorder = AudioRecord.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setAudioPlaybackCaptureConfig(config)
                .build()

            // Create output file for device audio
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            currentDeviceFile = File(recordingDir, "device_audio_$timestamp.pcm")
            deviceAudioOutputStream = FileOutputStream(currentDeviceFile)

            deviceAudioRecorder?.startRecording()

            deviceAudioJob = scope.launch {
                val buffer = ShortArray(bufferSize / 2)
                val byteBuffer = ByteArray(bufferSize)
                while (_isCapturing.value && deviceAudioRecorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = deviceAudioRecorder?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        processAudioData(buffer, read, false)

                        // Save to file
                        for (i in 0 until read) {
                            byteBuffer[i * 2] = (buffer[i].toInt() and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = ((buffer[i].toInt() shr 8) and 0xFF).toByte()
                        }
                        deviceAudioOutputStream?.write(byteBuffer, 0, read * 2)
                    }
                }
            }

            updateRecordingStatus()
            Log.d(TAG, "Device audio capture started, saving to: ${currentDeviceFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting device audio capture", e)
            _isCapturing.value = false
        }
    }

    private fun processAudioData(buffer: ShortArray, size: Int, isMicrophone: Boolean) {
        // Calculate RMS volume
        var sum = 0.0
        for (i in 0 until size) {
            sum += (buffer[i] * buffer[i]).toDouble()
        }
        val rms = sqrt(sum / size)
        val volume = (rms / Short.MAX_VALUE).toFloat()

        // Update waveform (downsample to 100 points)
        val waveform = FloatArray(100)
        val step = size / 100
        for (i in 0 until 100) {
            val index = i * step
            if (index < size) {
                waveform[i] = abs(buffer[index].toFloat() / Short.MAX_VALUE)
            }
        }

        if (isMicrophone) {
            _microphoneVolume.value = volume
            _microphoneWaveform.value = waveform
        } else {
            _deviceAudioVolume.value = volume
            _deviceAudioWaveform.value = waveform
        }
    }

    fun setRecordingDirectory(dir: File) {
        recordingDir = dir
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    private fun updateRecordingStatus() {
        val status = "录制保存至: ${recordingDir?.absolutePath}"
        _recordingStatus.value = status
        Log.d(TAG, status)
    }

    fun stopCapture() {
        _isCapturing.value = false

        microphoneJob?.cancel()
        deviceAudioJob?.cancel()

        try {
            microphoneRecorder?.stop()
            microphoneRecorder?.release()
            microphoneRecorder = null

            microphoneOutputStream?.flush()
            microphoneOutputStream?.close()
            microphoneOutputStream = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping microphone", e)
        }

        try {
            deviceAudioRecorder?.stop()
            deviceAudioRecorder?.release()
            deviceAudioRecorder = null

            deviceAudioOutputStream?.flush()
            deviceAudioOutputStream?.close()
            deviceAudioOutputStream = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping device audio", e)
        }

        // Mix the two audio files
        scope.launch {
            if (currentMicFile?.exists() == true && currentDeviceFile?.exists() == true) {
                val mixedFile = mixAudioFiles(currentMicFile!!, currentDeviceFile!!)
                _recordingStatus.value = "录制完成，已保存混音文件: ${mixedFile.name}"
                Log.d(TAG, "Mixed audio saved to: ${mixedFile.absolutePath}")
            } else {
                _recordingStatus.value = ""
            }

            // Reset current files
            currentMicFile = null
            currentDeviceFile = null

            // Reset volume and waveform
            _microphoneVolume.value = 0f
            _deviceAudioVolume.value = 0f
            _microphoneWaveform.value = FloatArray(100)
            _deviceAudioWaveform.value = FloatArray(100)
        }

        Log.d(TAG, "Audio capture stopped")
    }

    private suspend fun mixAudioFiles(micFile: File, deviceFile: File): File = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val mixedFile = File(recordingDir, "mixed_$timestamp.pcm")

        FileInputStream(micFile).use { micInput ->
            FileInputStream(deviceFile).use { deviceInput ->
                FileOutputStream(mixedFile).use { output ->
                    val bufferSize = 4096
                    val micBuffer = ByteArray(bufferSize)
                    val deviceBuffer = ByteArray(bufferSize)
                    val mixedBuffer = ShortArray(bufferSize / 2)

                    while (true) {
                        val micRead = micInput.read(micBuffer)
                        val deviceRead = deviceInput.read(deviceBuffer)

                        if (micRead <= 0 && deviceRead <= 0) break

                        val readLength = min(micRead, deviceRead)
                        if (readLength <= 0) break

                        // Mix the audio samples (average of both)
                        for (i in 0 until readLength / 2) {
                            val micSample = if (i * 2 + 1 < micRead) {
                                ((micBuffer[i * 2 + 1].toInt() shl 8) or (micBuffer[i * 2].toInt() and 0xFF)).toShort()
                            } else 0

                            val deviceSample = if (i * 2 + 1 < deviceRead) {
                                ((deviceBuffer[i * 2 + 1].toInt() shl 8) or (deviceBuffer[i * 2].toInt() and 0xFF)).toShort()
                            } else 0

                            mixedBuffer[i] = ((micSample.toInt() + deviceSample.toInt()) / 2).toShort()
                        }

                        // Write mixed buffer to output
                        val outputBuffer = ByteArray(readLength)
                        for (i in 0 until readLength / 2) {
                            outputBuffer[i * 2] = (mixedBuffer[i].toInt() and 0xFF).toByte()
                            outputBuffer[i * 2 + 1] = ((mixedBuffer[i].toInt() shr 8) and 0xFF).toByte()
                        }
                        output.write(outputBuffer, 0, readLength)
                    }
                }
            }
        }

        mixedFile
    }

    fun getRecordingFiles(): List<File> {
        return recordingDir?.listFiles { file ->
            file.name.startsWith("mixed_") && file.name.endsWith(".pcm")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun deleteRecording(file: File): Boolean {
        return try {
            val deleted = file.delete()
            if (deleted) {
                Log.d(TAG, "Deleted recording: ${file.name}")
            }
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting recording", e)
            false
        }
    }

    fun playRecording(file: File) {
        if (_isPlaying.value) {
            stopPlayback()
        }

        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AUDIO_FORMAT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        _isPlaying.value = true

        playbackJob = scope.launch {
            try {
                FileInputStream(file).use { input ->
                    val buffer = ByteArray(bufferSize)
                    val fileSize = file.length()
                    var totalRead = 0L

                    while (_isPlaying.value) {
                        val read = input.read(buffer)
                        if (read <= 0) break

                        audioTrack?.write(buffer, 0, read)
                        totalRead += read
                        _playbackProgress.value = totalRead.toFloat() / fileSize
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during playback", e)
            } finally {
                stopPlayback()
            }
        }

        Log.d(TAG, "Started playback of ${file.name}")
    }

    fun stopPlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()

        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback", e)
        }

        _playbackProgress.value = 0f
        Log.d(TAG, "Playback stopped")
    }
}