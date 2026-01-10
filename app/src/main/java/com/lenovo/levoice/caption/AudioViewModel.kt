package com.lenovo.levoice.caption

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class AudioViewModel(application: Application) : AndroidViewModel(application) {
    val audioCaptureManager = AudioCaptureManager()
    val speechRecognitionManager = SpeechRecognitionManager(application)

    private val _showRecordingsList = MutableStateFlow(false)
    val showRecordingsList: StateFlow<Boolean> = _showRecordingsList

    private val _currentPlayingFile = MutableStateFlow<File?>(null)
    val currentPlayingFile: StateFlow<File?> = _currentPlayingFile

    private val _refreshTrigger = MutableStateFlow(0)
    val refreshTrigger: StateFlow<Int> = _refreshTrigger

    private val _showTranscription = MutableStateFlow(false)
    val showTranscription: StateFlow<Boolean> = _showTranscription

    private val _showOverlaySettings = MutableStateFlow(false)
    val showOverlaySettings: StateFlow<Boolean> = _showOverlaySettings

    init {
        // Set recording directory
        val recordingDir = File(application.getExternalFilesDir(null), "AudioRecordings")
        audioCaptureManager.setRecordingDirectory(recordingDir)
    }

    fun setShowRecordingsList(show: Boolean) {
        _showRecordingsList.value = show
        if (!show) {
            // Stop playback when returning to main screen
            audioCaptureManager.stopPlayback()
            _currentPlayingFile.value = null
        }
    }

    fun playRecording(file: File) {
        _currentPlayingFile.value = file
        audioCaptureManager.playRecording(file)
    }

    fun stopPlayback() {
        audioCaptureManager.stopPlayback()
        _currentPlayingFile.value = null
    }

    fun deleteRecording(file: File): Boolean {
        if (_currentPlayingFile.value == file) {
            stopPlayback()
        }
        val deleted = audioCaptureManager.deleteRecording(file)
        if (deleted) {
            _refreshTrigger.value++
        }
        return deleted
    }

    fun getRecordings(): List<File> {
        return audioCaptureManager.getRecordingFiles()
    }

    fun setShowTranscription(show: Boolean) {
        _showTranscription.value = show
        if (!show) {
            speechRecognitionManager.stopRecognition()
        }
    }

    fun startTranscription(file: File) {
        speechRecognitionManager.clearText()
        speechRecognitionManager.startRecognitionFromFile(file)
    }

    fun stopTranscription() {
        speechRecognitionManager.stopRecognition()
    }

    fun setShowOverlaySettings(show: Boolean) {
        _showOverlaySettings.value = show
    }

    override fun onCleared() {
        super.onCleared()
        audioCaptureManager.stopCapture()
        audioCaptureManager.stopPlayback()
        speechRecognitionManager.destroy()
    }
}