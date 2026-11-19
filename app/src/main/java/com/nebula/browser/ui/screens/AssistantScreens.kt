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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.graphics.Color
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
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, topBar = {
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
    state: com.nebula.browser.BrowserUiState,
    onDismiss: () -> Unit,
    onAsk: (String) -> Unit,
    onClear: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartAgent: (String) -> Unit,
    onConfirmAgent: (String) -> Unit,
    onDismissAgentPlan: () -> Unit,
    onCancelAgent: () -> Unit,
) {
    var question by remember { mutableStateOf("") }
    var task by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("Assistant") }
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("NEBULA INTELLIGENCE", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            androidx.compose.material3.TabRow(selectedTabIndex = if (mode == "Assistant") 0 else 1, containerColor = Color.Transparent) {
                androidx.compose.material3.Tab(selected = mode == "Assistant", onClick = { mode = "Assistant" }, text = { Text("Page assistant") })
                androidx.compose.material3.Tab(selected = mode == "Agent", onClick = { mode = "Agent" }, text = { Text("Research agent") })
            }
            if (!state.settings.aiEnabled) {
                Text("AI is disabled. Configure an OpenAI-compatible provider in Settings. Nothing is uploaded by opening this panel.")
                Button(onClick = onOpenSettings) { Text("Open AI settings") }
            } else if (mode == "Assistant") {
                Text("Ask only about the current page. A separate consent dialog appears before page content is sent.")
                OutlinedTextField(question, { question = it }, Modifier.fillMaxWidth(), label = { Text("Ask about this page") }, minLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { onAsk("Summarize with concise bullets and caveats.") }, label = { Text("Summarize") }, enabled = !state.aiLoading)
                    AssistChip(onClick = { onAsk("Explain this page in simple language, preserving important nuances.") }, label = { Text("Explain") }, enabled = !state.aiLoading)
                    AssistChip(onClick = { onAsk("Extract key facts, dates, numbers and unresolved questions.") }, label = { Text("Key facts") }, enabled = !state.aiLoading)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { onAsk("Translate the main content of this page into natural Bengali.") }, label = { Text("বাংলা") }, enabled = !state.aiLoading)
                    AssistChip(onClick = { onAsk("Create five thoughtful research questions and suggest search queries to verify this page.") }, label = { Text("Research") }, enabled = !state.aiLoading)
                }
                Button(onClick = { onAsk(question.ifBlank { "Summarize the key points of this page." }) }, enabled = !state.aiLoading) { Text("Review & send") }
                if (state.aiLoading) Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(); Text("  Waiting for your provider…") }
                if (state.aiResponse.isNotBlank()) {
                    HorizontalDivider()
                    SelectionContainer { Text(state.aiResponse) }
                    Row {
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Nebula AI response", state.aiResponse))
                        }) { Text("Copy") }
                        TextButton(onClick = onClear) { Text("Clear response") }
                    }
                }
            } else {
                Text("Safe browser automation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Open up to 12 HTTPS sources, go back, collect readable text, and export TXT/PDF. Every plan requires approval. No forms, payments, credentials, arbitrary code, or hidden actions.")
                OutlinedTextField(
                    task, { task = it }, Modifier.fillMaxWidth(), minLines = 3,
                    label = { Text("Research task") },
                    placeholder = { Text("Example: Research Android WebView desktop mode from official sources and export a PDF") },
                )
                Button(onClick = { onStartAgent(task) }, enabled = task.length >= 3 && !state.agentPlanning && !state.agentRunning) { Text(if (state.agentPlanning) "Planning…" else "Create safe plan") }
                if (state.agentPlanning) CircularProgressIndicator()
                if (state.agentPlan.isNotEmpty()) com.nebula.browser.ui.components.GlassCard(Modifier.fillMaxWidth(), active = true) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Review this plan before anything opens", fontWeight = FontWeight.Bold)
                        state.agentPlan.forEachIndexed { index, action ->
                            Text("${index + 1}. ${action.title} · ${action.action}")
                            if (action.urls.isNotEmpty()) action.urls.forEach { Text("  $it", style = MaterialTheme.typography.bodySmall) }
                        }
                        Text("Collected text stays on this device for export. Only the task above was sent to the AI planner.")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onConfirmAgent(task) }) { Text("Approve & run") }
                            OutlinedButton(onClick = onDismissAgentPlan) { Text("Reject") }
                        }
                    }
                }
                if (state.agentSteps.isNotEmpty()) {
                    androidx.compose.material3.LinearProgressIndicator(progress = { state.agentProgress }, modifier = Modifier.fillMaxWidth())
                    state.agentSteps.forEach { step ->
                        com.nebula.browser.ui.components.GlassCard(Modifier.fillMaxWidth(), active = step.status == com.nebula.browser.feature.ai.AgentStepStatus.RUNNING) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    when (step.status) {
                                        com.nebula.browser.feature.ai.AgentStepStatus.COMPLETE -> Icons.Default.ArrowForward
                                        com.nebula.browser.feature.ai.AgentStepStatus.RUNNING -> Icons.Default.Refresh
                                        else -> Icons.Default.SmartToy
                                    }, null, tint = MaterialTheme.colorScheme.primary,
                                )
                                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                    Text(step.title, fontWeight = FontWeight.SemiBold)
                                    Text(step.detail, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(step.status.name, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                if (state.agentRunning) OutlinedButton(onClick = onCancelAgent) { Text("Cancel task") }
                if (state.agentReport.isNotBlank()) {
                    Text("Live report preview", fontWeight = FontWeight.Bold)
                    SelectionContainer { Text(state.agentReport.take(6000), style = MaterialTheme.typography.bodySmall) }
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
