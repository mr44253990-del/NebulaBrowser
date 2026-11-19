package com.nebula.browser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nebula.browser.AiChatMessage
import com.nebula.browser.AiChatRole
import com.nebula.browser.BrowserUiState
import com.nebula.browser.feature.ai.AgentStepStatus
import com.nebula.browser.ui.components.GlassCard

/**
 * Full-screen Nebula AI: a conversation about the current page plus the safe research
 * agent in a single, modern workspace. The quick sheet can expand into this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    state: BrowserUiState,
    onBack: () -> Unit,
    onAsk: (String) -> Unit,
    onClear: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartAgent: (String) -> Unit,
    onConfirmAgent: (String) -> Unit,
    onDismissAgentPlan: () -> Unit,
    onCancelAgent: () -> Unit,
    onExportChat: () -> Unit,
    onExportReport: () -> Unit,
) {
    var mode by remember { mutableStateOf(0) }
    var input by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                Column(Modifier.weight(1f)) {
                    Text("Nebula AI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(
                        if (state.settings.aiEnabled) "${state.settings.aiModel.ifBlank { "No model set" }} · consent required"
                        else "AI is off · no page data leaves this device",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onExportChat, enabled = state.aiChat.isNotEmpty()) {
                    Icon(Icons.Default.Download, "Export conversation")
                }
                IconButton(onClick = onClear, enabled = state.aiChat.isNotEmpty()) {
                    Icon(Icons.Default.DeleteSweep, "Clear conversation")
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
                Spacer(Modifier.width(7.dp))
                Text(
                    if (state.isHome) "No page context · open a page to ask about it" else state.currentTitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (state.currentUrl.isNotBlank()) {
                    Icon(
                        if (state.currentUrl.startsWith("https://")) Icons.Default.Lock else Icons.Default.ErrorOutline,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TabRow(selectedTabIndex = mode, containerColor = Color.Transparent) {
                Tab(selected = mode == 0, onClick = { mode = 0 }, text = { Text("Ask this page") })
                Tab(selected = mode == 1, onClick = { mode = 1 }, text = { Text("Research agent") })
            }
            if (!state.settings.aiEnabled) {
                AiDisabled(onOpenSettings)
            } else if (mode == 0) {
                ChatPane(
                    state = state,
                    input = input,
                    onInput = { input = it },
                    onSend = onAsk,
                    onCopy = { text -> clipboard.setText(AnnotatedString(text)) },
                    onOpenSettings = onOpenSettings,
                )
            } else {
                AgentPane(
                    state = state,
                    onStartAgent = onStartAgent,
                    onConfirmAgent = onConfirmAgent,
                    onDismissAgentPlan = onDismissAgentPlan,
                    onCancelAgent = onCancelAgent,
                    onExportReport = onExportReport,
                    onCopy = { text -> clipboard.setText(AnnotatedString(text)) },
                )
            }
        }
    }
}

@Composable
private fun ChatPane(
    state: BrowserUiState,
    input: String,
    onInput: (String) -> Unit,
    onSend: (String) -> Unit,
    onCopy: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.aiChat.isEmpty()) {
                item { StartCards(onPick = onSend) }
            }
            items(state.aiChat, key = { it.id }) { message -> ChatBubble(message, onCopy) }
            if (state.aiLoading) item { ThinkingRow() }
        }
        QuickPrompts(onSend, enabled = !state.aiLoading && !state.isHome)
        Composer(
            input = input,
            onInput = onInput,
            enabled = !state.aiLoading && !state.isHome,
            onSend = {
                val question = input.trim()
                if (question.isNotEmpty()) {
                    onInput("")
                    onSend(question)
                }
            },
        )
    }
}

@Composable
private fun StartCards(onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Start with", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        SuggestionCard("Summarize this page", "Concise bullets with caveats", Icons.Default.SmartToy) {
            onPick("Summarize this page with concise bullets and caveats.")
        }
        SuggestionCard("Explain simply", "Plain language without losing nuance", Icons.Default.SmartToy) {
            onPick("Explain this page in simple language, preserving important nuances.")
        }
        SuggestionCard("Extract key facts", "Dates, numbers and open questions", Icons.Default.SmartToy) {
            onPick("Extract key facts, dates, numbers and unresolved questions.")
        }
        SuggestionCard("বাংলায় অনুবাদ", "Translate the main content into Bengali", Icons.Default.SmartToy) {
            onPick("Translate the main content of this page into natural Bengali.")
        }
        Text(
            "A consent dialog appears before any page text is sent to your provider.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SuggestionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .14f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("→", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ChatBubble(message: AiChatMessage, onCopy: (String) -> Unit) {
    val user = message.role == AiChatRole.USER
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
        Column(Modifier.widthIn(max = 340.dp)) {
            Surface(
                shape = RoundedCornerShape(if (user) 18.dp else 18.dp),
                color = if (user) MaterialTheme.colorScheme.primary.copy(alpha = .16f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f),
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (!user) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SmartToy, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Nebula", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    SelectionContainer { Text(message.text, style = MaterialTheme.typography.bodyMedium) }
                    if (message.pending) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(3.dp))
                    }
                }
            }
            if (!user && !message.pending && message.text.isNotBlank()) {
                Row(Modifier.padding(start = 2.dp, top = 2.dp)) {
                    TextButton(onClick = { onCopy(message.text) }) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Copy", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingRow() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(8.dp))
        Text("Nebula is reading the page…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QuickPrompts(onSend: (String) -> Unit, enabled: Boolean) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistChip(onClick = { onSend("Summarize with concise bullets and caveats.") }, label = { Text("Summarize") }, enabled = enabled)
        AssistChip(onClick = { onSend("Explain this page in simple language.") }, label = { Text("Explain") }, enabled = enabled)
        AssistChip(onClick = { onSend("Extract key facts, dates and numbers.") }, label = { Text("Key facts") }, enabled = enabled)
        AssistChip(onClick = { onSend("List five verifiable claims and how to check them.") }, label = { Text("Fact-check") }, enabled = enabled)
        AssistChip(onClick = { onSend("Translate the main content of this page into natural Bengali.") }, label = { Text("বাংলা") }, enabled = enabled)
    }
}

@Composable
private fun Composer(input: String, onInput: (String) -> Unit, enabled: Boolean, onSend: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().imePadding().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInput,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about this page…") },
                maxLines = 4,
                enabled = enabled,
                shape = RoundedCornerShape(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = onSend, enabled = enabled && input.isNotBlank(), modifier = Modifier.size(50.dp)) {
                Icon(Icons.Default.Send, "Send")
            }
        }
    }
}

@Composable
private fun AgentPane(
    state: BrowserUiState,
    onStartAgent: (String) -> Unit,
    onConfirmAgent: (String) -> Unit,
    onDismissAgentPlan: () -> Unit,
    onCancelAgent: () -> Unit,
    onExportReport: () -> Unit,
    onCopy: (String) -> Unit,
) {
    var task by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Safe browser research", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Opens up to 12 HTTPS sources, collects readable text and exports a report. Every plan needs your approval. No forms, payments, credentials or hidden actions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = task,
            onValueChange = { task = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            label = { Text("What should Nebula research?") },
            placeholder = { Text("Example: Compare Android WebView desktop mode behavior across official documentation") },
            shape = RoundedCornerShape(18.dp),
        )
        Button(
            onClick = { onStartAgent(task) },
            enabled = task.length >= 3 && !state.agentPlanning && !state.agentRunning,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.agentPlanning) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Planning…")
            } else {
                Text("Create a safe plan")
            }
        }
        if (state.agentPlan.isNotEmpty()) {
            GlassCard(Modifier.fillMaxWidth(), active = true, radius = 18.dp) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Review the plan", fontWeight = FontWeight.Bold)
                    state.agentPlan.forEachIndexed { index, action ->
                        Row {
                            Text("${index + 1}.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(action.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(action.action, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                action.urls.forEach { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            }
                        }
                    }
                    Text("Only your task above is sent to the planner. Collected text stays on this device.", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onConfirmAgent(task) }) { Text("Approve & run") }
                        OutlinedButton(onClick = onDismissAgentPlan) { Text("Reject") }
                    }
                }
            }
        }
        if (state.agentSteps.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(progress = { state.agentProgress }, modifier = Modifier.weight(1f).height(6.dp), trackColor = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.width(10.dp))
                Text("${(state.agentProgress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            state.agentSteps.forEach { step ->
                val running = step.status == AgentStepStatus.RUNNING
                GlassCard(Modifier.fillMaxWidth(), active = running, radius = 16.dp) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            when (step.status) {
                                AgentStepStatus.COMPLETE -> Icons.Default.CheckCircle
                                AgentStepStatus.RUNNING -> Icons.Default.RadioButtonUnchecked
                                AgentStepStatus.FAILED, AgentStepStatus.CANCELLED -> Icons.Default.ErrorOutline
                                else -> Icons.Default.RadioButtonUnchecked
                            },
                            null,
                            tint = when (step.status) {
                                AgentStepStatus.COMPLETE -> MaterialTheme.colorScheme.primary
                                AgentStepStatus.FAILED, AgentStepStatus.CANCELLED -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.secondary
                            },
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(step.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(step.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Text(step.status.name.lowercase().replace('_', ' '), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (state.agentRunning) {
                OutlinedButton(onClick = onCancelAgent, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Stop, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cancel the task")
                }
            }
        }
        if (state.agentReport.isNotBlank()) {
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Live report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onExportReport) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Save")
                }
                TextButton(onClick = { onCopy(state.agentReport) }) { Text("Copy") }
            }
            SelectionContainer {
                Text(state.agentReport.take(8_000), style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun AiDisabled(onOpenSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Settings, null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("AI is off", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Add an OpenAI-compatible endpoint, model and key in Settings. Nothing is uploaded until you approve a specific request.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onOpenSettings) { Text("Open AI settings") }
    }
}
