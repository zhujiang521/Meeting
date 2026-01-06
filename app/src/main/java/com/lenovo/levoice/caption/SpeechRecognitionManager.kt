package com.lenovo.levoice.caption

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileInputStream

class SpeechRecognitionManager(private val context: Context) {
    companion object {
        private const val TAG = "SpeechRecognition"
    }

    private var speechRecognizer: SpeechRecognizer? = null

    private val _recognitionText = MutableStateFlow("")
    val recognitionText: StateFlow<String> = _recognitionText

    private val _isRecognizing = MutableStateFlow(false)
    val isRecognizing: StateFlow<Boolean> = _isRecognizing

    private val _recognitionError = MutableStateFlow<String?>(null)
    val recognitionError: StateFlow<String?> = _recognitionError

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            setupRecognitionListener()
        } else {
            Log.e(TAG, "Speech recognition not available")
        }
    }

    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                _isRecognizing.value = true
                _recognitionError.value = null
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Volume level changed
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Partial audio buffer
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
                _isRecognizing.value = false
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "音频录制错误"
                    SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                    SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                    SpeechRecognizer.ERROR_NO_MATCH -> "未识别到语音"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙"
                    SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音超时"
                    else -> "未知错误: $error"
                }
                Log.e(TAG, "Recognition error: $errorMessage")
                _recognitionError.value = errorMessage
                _isRecognizing.value = false
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    Log.d(TAG, "Recognized: $recognizedText")

                    // Append to existing text
                    val currentText = _recognitionText.value
                    _recognitionText.value = if (currentText.isEmpty()) {
                        recognizedText
                    } else {
                        "$currentText\n$recognizedText"
                    }
                }
                _isRecognizing.value = false
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Log.d(TAG, "Partial: ${matches[0]}")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Event received
            }
        })
    }

    fun startRecognitionFromFile(audioFile: File) {
        if (speechRecognizer == null) {
            _recognitionError.value = "语音识别不可用"
            return
        }

        // Clear previous results
        _recognitionText.value = ""
        _recognitionError.value = null

        // Note: Android SpeechRecognizer requires real-time audio from microphone
        // For file-based recognition, we need to use a different approach
        // Here we'll use a workaround by playing the audio

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            speechRecognizer?.startListening(intent)
            Log.d(TAG, "Started speech recognition")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recognition", e)
            _recognitionError.value = "启动识别失败: ${e.message}"
        }
    }

    fun stopRecognition() {
        try {
            speechRecognizer?.stopListening()
            _isRecognizing.value = false
            Log.d(TAG, "Stopped speech recognition")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognition", e)
        }
    }

    fun clearText() {
        _recognitionText.value = ""
        _recognitionError.value = null
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying recognizer", e)
        }
    }
}