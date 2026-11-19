package com.nebula.browser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.nebula.browser.BrowserUiState
import com.nebula.browser.ui.components.GlassCard
import com.nebula.browser.ui.components.SectionTitle
import java.io.File

@Composable
fun DeveloperScreen(
    state: BrowserUiState,
    onBack: () -> Unit,
    onRunScript: (String) -> Unit,
    onViewSource: () -> Unit,
) {
    val context = LocalContext.current
    var script by remember { mutableStateOf("({title: document.title, url: location.href, language: navigator.language, cookiesEnabled: navigator.cookieEnabled})") }
    var showCrash by remember { mutableStateOf(false) }
    val crashLog = remember(showCrash) { runCatching { File(context.filesDir, "crash-report.log").takeIf { it.exists() }?.readText() ?: "No crashes recorded on this device." }.getOrElse { it.message ?: "Unavailable" } }
    Scaffold(containerColor = Color.Transparent, topBar = { LibraryTopBar("Developer console", onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Chromium remote debugging", "WebContentsDebugging is enabled while Developer Mode is on.")
                    Text("Connect USB, open chrome://inspect on a desktop, and inspect this app's WebViews. Disable Developer Mode on untrusted devices.", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    Text("Active: ${state.currentUrl}", maxLines = 2)
                    OutlinedButton(onClick = onViewSource) { Text("View page source") }
                }
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("JavaScript evaluator", "Runs only in the active tab after you press Run.")
                    OutlinedTextField(script, { script = it }, Modifier.fillMaxWidth().heightIn(min = 110.dp), textStyle = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                    Button(onClick = { onRunScript(script) }, enabled = script.isNotBlank()) { Text("Run in active page") }
                    SelectionContainer { Text(state.developerOutput.ifBlank { "Result will appear here" }, fontFamily = FontFamily.Monospace) }
                }
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("Page console", "Latest 100 messages while Developer Mode is enabled")
                    if (state.consoleMessages.isEmpty()) Text("No console messages captured")
                    state.consoleMessages.takeLast(20).forEach { Text(it, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
                }
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth()) { SectionTitle("Local crash report"); TextButton(onClick = { showCrash = !showCrash }) { Text(if (showCrash) "Hide" else "Show") } }
                    if (showCrash) SelectionContainer { Text(crashLog, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
                    Text("Crash logs stay on this device and are never uploaded automatically.", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
