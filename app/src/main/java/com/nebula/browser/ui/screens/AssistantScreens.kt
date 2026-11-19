package com.nebula.browser.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nebula.browser.PendingAiRequest
import java.util.Locale
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(title: String, text: String, onBack: () -> Unit, onAskAi: () -> Unit) {
    val context = LocalContext.current
    var fontSize by remember { mutableStateOf(20f) }
    var speaking by remember { mutableStateOf(false) }
    val tts = remember {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) engine.language = Locale.getDefault()
        }
        engine
    }
    DisposableEffect(Unit) { onDispose { tts.stop(); tts.shutdown() } }
    Scaffold(topBar = {
        LibraryTopBar("Reader mode", onBack) {
            IconButton(onClick = {
                if (speaking) { tts.stop(); speaking = false } else {
                    speaking = true
                    tts.speak(text.take(12000), TextToSpeech.QUEUE_FLUSH, null, "nebula_reader")
                }
            }) { Icon(if (speaking) Icons.Default.Stop else Icons.Default.VolumeUp, "Read aloud") }
            IconButton(onClick = onAskAi) { Icon(Icons.Default.SmartToy, "Ask AI") }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(22.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("${ceil(text.split(Regex("\\s+")).size / 220.0).toInt()} min read · Distraction-free local view", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Text size")
                Slider(fontSize, { fontSize = it }, valueRange = 15f..30f, modifier = Modifier.weight(1f))
            }
            SelectionContainer { Text(text, fontSize = fontSize.sp, lineHeight = (fontSize * 1.55).sp) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantSheet(
    loading: Boolean,
    response: String,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onAsk: (String) -> Unit,
    onClear: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var question by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Nebula page intelligence", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Nothing is uploaded when you open this panel. You will see exactly when page text is about to be sent.")
            if (!enabled) {
                Text("AI is disabled. Configure an OpenAI-compatible provider in Settings.")
                Button(onClick = onOpenSettings) { Text("Open AI settings") }
            } else {
                OutlinedTextField(question, { question = it }, Modifier.fillMaxWidth(), label = { Text("Ask about this page") }, minLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onAsk(question.ifBlank { "Summarize the key points of this page." }) }, enabled = !loading) { Text("Review & send") }
                    OutlinedButton(onClick = { onAsk("Summarize this page with concise bullet points and important caveats.") }, enabled = !loading) { Text("Summarize") }
                }
                if (loading) Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(); Text("  Waiting for your provider…") }
                if (response.isNotBlank()) {
                    HorizontalDivider()
                    SelectionContainer { Text(response) }
                    TextButton(onClick = onClear) { Text("Clear response") }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun AiConsentDialog(pending: PendingAiRequest, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share this page with your AI provider?") },
        text = {
            Column {
                Text(pending.pageTitle, fontWeight = FontWeight.Bold)
                Text(pending.pageUrl)
                Spacer(Modifier.height(10.dp))
                Text("Up to ${pending.pageText.length.coerceAtMost(45_000)} characters of page text, its title, URL, and your question will leave this device. Do not continue on pages containing secrets or personal data.")
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("I understand, send") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
