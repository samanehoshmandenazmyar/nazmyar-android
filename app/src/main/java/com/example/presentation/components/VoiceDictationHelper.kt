package com.example.presentation.components

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.IncidentRed
import com.example.ui.theme.SchoolBluePrimary
import com.example.ui.theme.VazirmatnFontFamily

@Composable
fun VoiceDictationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 3,
    testTagPrefix: String = "voice_input"
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isListening = true
        }
    }

    DisposableEffect(isListening) {
        if (isListening) {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            var committedText = value

            fun startListeningIntent() {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "fa")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
                }

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        // Keep listening continuously until user clicks stop icon!
                        if (isListening) {
                            try { recognizer.startListening(intent) } catch (_: Exception) {}
                        }
                    }

                    override fun onError(error: Int) {
                        // Keep listening continuously despite noise pauses!
                        if (isListening) {
                            committedText = value
                            try { recognizer.startListening(intent) } catch (_: Exception) {}
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        if (!text.isNullOrBlank()) {
                            val updated = if (committedText.isBlank()) text else "$committedText $text"
                            committedText = updated
                            onValueChange(updated)
                        }
                        if (isListening) {
                            try { recognizer.startListening(intent) } catch (_: Exception) {}
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        if (!text.isNullOrBlank()) {
                            val updated = if (committedText.isBlank()) text else "$committedText $text"
                            onValueChange(updated)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                try {
                    recognizer.startListening(intent)
                } catch (e: Exception) {
                    Log.w("VoiceDictation", "Failed starting speech recognizer: ${e.message}")
                }
            }

            startListeningIntent()

            onDispose {
                try {
                    recognizer.stopListening()
                    recognizer.destroy()
                } catch (_: Exception) {}
            }
        } else {
            onDispose { }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = VazirmatnFontFamily,
                        fontWeight = FontWeight.Medium
                    )
                )
            },
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = VazirmatnFontFamily,
                        fontWeight = FontWeight.Light
                    )
                )
            },
            minLines = minLines,
            trailingIcon = {
                Surface(
                    shape = CircleShape,
                    color = if (isListening) IncidentRed.copy(alpha = 0.15f) else SchoolBluePrimary.copy(alpha = 0.1f),
                    modifier = Modifier
                        .padding(8.dp)
                        .scale(if (isListening) pulseScale else 1f)
                ) {
                    IconButton(
                        onClick = {
                            if (isListening) {
                                isListening = false
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.testTag("${testTagPrefix}_mic_btn")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isListening) "توقف ضبط صوتی" else "شروع ضبط صوتی",
                            tint = if (isListening) IncidentRed else SchoolBluePrimary
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        AnimatedVisibility(visible = isListening) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(10.dp),
                color = IncidentRed.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, IncidentRed.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .scale(pulseScale)
                                .background(IncidentRed, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "سیستم صوتی فعال است... 🎙️",
                            style = MaterialTheme.typography.labelSmall,
                            color = IncidentRed,
                            fontWeight = FontWeight.Bold,
                            fontFamily = VazirmatnFontFamily
                        )
                    }
                    Text(
                        text = "توقف 🛑",
                        style = MaterialTheme.typography.labelSmall,
                        color = IncidentRed,
                        fontWeight = FontWeight.Bold,
                        fontFamily = VazirmatnFontFamily,
                        modifier = Modifier.clickable { isListening = false }
                    )
                }
            }
        }
    }
}
