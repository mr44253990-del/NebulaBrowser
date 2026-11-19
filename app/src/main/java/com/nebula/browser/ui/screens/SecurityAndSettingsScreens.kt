package com.nebula.browser.ui.screens

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.nebula.browser.BrowserUiState
import com.nebula.browser.core.BrowserSettings
import com.nebula.browser.core.PasswordCredential

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordsScreen(
    state: BrowserUiState,
    onBack: () -> Unit,
    onUnlocked: () -> Unit,
    onSave: (PasswordCredential) -> Unit,
    onDelete: (String) -> Unit,
    onGenerate: () -> String,
) {
    val activity = LocalContext.current as FragmentActivity
    var unlocked by remember { mutableStateOf(false) }
    var editorVisible by remember { mutableStateOf(false) }
    var visiblePasswordId by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }

    fun authenticate() {
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                unlocked = true
                onUnlocked()
            }
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Nebula Vault")
            .setSubtitle("Your credentials are protected by Android Keystore")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("Cancel")
            .build())
    }

    Scaffold(topBar = {
        LibraryTopBar("Password vault", onBack) {
            if (unlocked) IconButton(onClick = { editorVisible = true }) { Icon(Icons.Default.Add, "Add credential") }
        }
    }) { padding ->
        if (!unlocked) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Fingerprint, null, modifier = Modifier.height(72.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Your vault is locked", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Unlock with a strong biometric. Passwords are AES-GCM encrypted with a non-exportable Keystore key.")
                Spacer(Modifier.height(18.dp))
                Button(onClick = ::authenticate) { Text("Unlock vault") }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(12.dp), placeholder = { Text("Search credentials") }, singleLine = true)
                LazyColumn {
                    items(state.passwords.filter { query.isBlank() || it.title.contains(query, true) || it.username.contains(query, true) }, key = PasswordCredential::id) { credential ->
                        ListItem(
                            headlineContent = { Text(credential.title) },
                            supportingContent = {
                                Column {
                                    Text(credential.username)
                                    Text(if (visiblePasswordId == credential.id) credential.password else "••••••••••••")
                                    if (credential.url.isNotBlank()) Text(credential.url)
                                }
                            },
                            leadingContent = { Icon(Icons.Default.Key, null) },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { visiblePasswordId = if (visiblePasswordId == credential.id) null else credential.id }) { Icon(Icons.Default.Visibility, "Show password") }
                                    IconButton(onClick = { onDelete(credential.id) }) { Icon(Icons.Default.Delete, "Delete") }
                                }
                            },
                            modifier = Modifier.clickable {
                                val clipboard = activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Nebula credential", credential.password))
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (editorVisible) {
        var title by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf(onGenerate()) }
        var url by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { editorVisible = false },
            title = { Text("New credential") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("Website / title") }, singleLine = true)
                    OutlinedTextField(url, { url = it }, label = { Text("URL") }, singleLine = true)
                    OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true)
                    OutlinedTextField(password, { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())
                    TextButton(onClick = { password = onGenerate() }) { Text("Generate strong password") }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSave(PasswordCredential(title = title.ifBlank { url }, username = username, password = password, url = url))
                    editorVisible = false
                }, enabled = title.isNotBlank() && password.isNotBlank()) { Text("Encrypt & save") }
            },
            dismissButton = { TextButton(onClick = { editorVisible = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun PrivacyScreen(state: BrowserUiState, onBack: () -> Unit, onSettings: ((BrowserSettings) -> BrowserSettings) -> Unit, onClearData: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    Scaffold(topBar = { LibraryTopBar("Privacy control center", onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(22.dp)) {
                        Icon(Icons.Default.Security, null)
                        Text("Native protection is active", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("${state.blockedRequests}", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                        Text("network requests blocked this session")
                    }
                }
            }
            item { SettingSwitch("Ad & tracker host filtering", "Blocks known advertising and analytics host suffixes before requests load.", state.settings.adBlockEnabled) { enabled -> onSettings { it.copy(adBlockEnabled = enabled) } } }
            item { SettingSwitch("Block third-party cookies", "Applied to newly created tabs. Private tabs disable cookies and persistent storage.", state.settings.blockThirdPartyCookies) { enabled -> onSettings { it.copy(blockThirdPartyCookies = enabled) } } }
            item { SettingSwitch("HTTPS-first", "Typed domain names are upgraded to HTTPS. Certificate errors are never bypassed.", state.settings.httpsFirst) { enabled -> onSettings { it.copy(httpsFirst = enabled) } } }
            item { SettingSwitch("Wi-Fi only downloads", "New download requests will avoid metered and roaming connections.", state.settings.downloadWifiOnly) { enabled -> onSettings { it.copy(downloadWifiOnly = enabled) } } }
            item { OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) { Text("Clear history and all WebView sessions") } }
            item { Text("Private tabs: history is excluded, cookies and DOM storage are disabled, and cache is cleared when tabs close. Android WebView is process-shared; do not treat it as a separate security profile.", style = MaterialTheme.typography.bodySmall) }
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Clear browsing data?") },
        text = { Text("This deletes history and destroys every open WebView session. Bookmarks, downloads, and vault credentials remain.") },
        confirmButton = { Button(onClick = { onClearData(); confirmClear = false }) { Text("Clear") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: BrowserUiState,
    onBack: () -> Unit,
    onSettings: ((BrowserSettings) -> BrowserSettings) -> Unit,
    onAddEngine: (String, String, String) -> Unit,
    initialApiKey: String,
    onApiKey: (String) -> Unit,
) {
    var engineDialog by remember { mutableStateOf(false) }
    var apiKey by remember(initialApiKey) { mutableStateOf(initialApiKey) }
    var endpoint by remember(state.settings.aiEndpoint) { mutableStateOf(state.settings.aiEndpoint) }
    var model by remember(state.settings.aiModel) { mutableStateOf(state.settings.aiModel) }
    Scaffold(topBar = { LibraryTopBar("Settings", onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Experience", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item { SettingSwitch("Dark theme", "Use Nebula's AMOLED-friendly color palette.", state.settings.darkTheme) { enabled -> onSettings { it.copy(darkTheme = enabled) } } }
            item { SettingSwitch("Bottom address bar", "Reach navigation controls with one hand.", state.settings.bottomToolbar) { enabled -> onSettings { it.copy(bottomToolbar = enabled) } } }
            item { SettingSwitch("JavaScript", "Required by most modern sites. Changes apply to active and new sessions.", state.settings.javaScriptEnabled) { enabled -> onSettings { it.copy(javaScriptEnabled = enabled) } } }
            item {
                Text("Home grid columns: ${state.settings.homeColumns}")
                Slider(value = state.settings.homeColumns.toFloat(), onValueChange = { value -> onSettings { it.copy(homeColumns = value.toInt()) } }, valueRange = 3f..5f, steps = 1)
            }
            item { HorizontalDivider() }
            item { Text("Search", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.engines.forEach { engine ->
                        FilterChip(selected = state.settings.searchEngineId == engine.id, onClick = { onSettings { it.copy(searchEngineId = engine.id) } }, label = { Text("${engine.name}  ·  ${engine.keyword}") })
                    }
                    OutlinedButton(onClick = { engineDialog = true }) { Text("Add custom search engine") }
                }
            }
            item { HorizontalDivider() }
            item { Text("OpenAI-compatible AI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item { SettingSwitch("Enable page assistant", "Off by default. Page text is sent only after a separate confirmation.", state.settings.aiEnabled) { enabled -> onSettings { it.copy(aiEnabled = enabled) } } }
            item { OutlinedTextField(endpoint, { endpoint = it }, Modifier.fillMaxWidth(), label = { Text("API base URL (HTTPS)") }, singleLine = true) }
            item { OutlinedTextField(model, { model = it }, Modifier.fillMaxWidth(), label = { Text("Model") }, singleLine = true) }
            item { OutlinedTextField(apiKey, { apiKey = it }, Modifier.fillMaxWidth(), label = { Text("API key") }, visualTransformation = PasswordVisualTransformation(), singleLine = true) }
            item {
                Text("Response creativity: ${String.format("%.1f", state.settings.aiTemperature)}")
                Slider(value = state.settings.aiTemperature, onValueChange = { value -> onSettings { it.copy(aiTemperature = value) } }, valueRange = 0f..1f)
            }
            item { Button(onClick = {
                onApiKey(apiKey)
                onSettings { it.copy(aiEndpoint = endpoint.trim(), aiModel = model.trim()) }
            }, modifier = Modifier.fillMaxWidth()) { Text("Save AI configuration securely") } }
            item { Text("The API key is encrypted with Android Keystore. Nebula sends page content directly to your configured provider; it has no Nebula server.", style = MaterialTheme.typography.bodySmall) }
            item { HorizontalDivider() }
            item { Text("Nebula Browser 1.0.0\nChromium System WebView · Kotlin · Material 3\nNo account, analytics, or remote browser-data collection.") }
        }
    }

    if (engineDialog) {
        var name by remember { mutableStateOf("") }
        var keyword by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("https://example.com/search?q=%s") }
        AlertDialog(
            onDismissRequest = { engineDialog = false },
            title = { Text("Custom search engine") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") })
                OutlinedTextField(keyword, { keyword = it }, label = { Text("Keyword") })
                OutlinedTextField(url, { url = it }, label = { Text("Search URL with %s") })
            } },
            confirmButton = { Button(onClick = { onAddEngine(name, keyword, url); engineDialog = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { engineDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.width(12.dp))
            Switch(checked, onChange)
        }
    }
}
