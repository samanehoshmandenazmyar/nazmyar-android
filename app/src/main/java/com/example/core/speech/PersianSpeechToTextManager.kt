package com.example.core.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class SpeechState {
    data object Idle : SpeechState()
    data object Listening : SpeechState()
    data object Processing : SpeechState()
    data class Success(val recognizedText: String) : SpeechState()
    data class Error(val message: String) : SpeechState()
}

class PersianSpeechToTextManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(onPartial: ((String) -> Unit)? = null, onResult: (String) -> Unit) {
        if (!isAvailable) {
            _speechState.value = SpeechState.Error("سرویس تشخیص گفتار گوگل روی این دستگاه در دسترس نیست. می‌توانید متن را به صورت دستی تایپ کنید.")
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _speechState.value = SpeechState.Listening
                    }

                    override fun onBeginningOfSpeech() {
                        _speechState.value = SpeechState.Listening
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        _soundLevel.value = (rmsdB.coerceIn(0f, 10f) / 10f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _speechState.value = SpeechState.Processing
                    }

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "خطا در ضبط صدا از میکروفون."
                            SpeechRecognizer.ERROR_CLIENT -> "خطای اتصال کلاینت صوتی."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "دسترسی به میکروفون داده نشده است."
                            SpeechRecognizer.ERROR_NETWORK -> "خطای شبکه اینترنت در اتصال به سرویس تشخیص گفتار."
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "زمان اتصال به اینترنت برای تشخیص گفتار به پایان رسید."
                            SpeechRecognizer.ERROR_NO_MATCH -> "صدایی با وضوح کافی تشخیص داده نشد. لطفاً دوباره صحبت کنید."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "سرویس گفتار در حال حاضر مشغول است."
                            SpeechRecognizer.ERROR_SERVER -> "خطایی از سمت سرور تبدیل گفتار رخ داد."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "هیچ گفتاری شنیده نشد."
                            else -> "خطای نامشخص در تشخیص گفتار ($error)."
                        }
                        _speechState.value = SpeechState.Error(message)
                        _soundLevel.value = 0f
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim().orEmpty()
                        _soundLevel.value = 0f
                        if (text.isNotBlank()) {
                            _speechState.value = SpeechState.Success(text)
                            _partialText.value = text
                            onResult(text)
                        } else {
                            _speechState.value = SpeechState.Error("متنی از گفتار دریافت نشد.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim().orEmpty()
                        if (text.isNotBlank()) {
                            _partialText.value = text
                            onPartial?.invoke(text)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("fa", "fa-IR"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            speechRecognizer?.startListening(intent)
            _speechState.value = SpeechState.Listening
        } catch (e: Exception) {
            _speechState.value = SpeechState.Error("امکان شروع ضبط صدا فراهم نشد: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        if (_speechState.value is SpeechState.Listening) {
            _speechState.value = SpeechState.Idle
        }
        _soundLevel.value = 0f
    }

    fun reset() {
        stopListening()
        _speechState.value = SpeechState.Idle
        _partialText.value = ""
        _soundLevel.value = 0f
    }
}
