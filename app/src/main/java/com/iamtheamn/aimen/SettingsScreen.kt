package com.iamtheamn.aimen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

enum class SettingsTab {
    APPEARANCE, VOICE, SERVER, SYNC, ABOUT
}

@Composable
fun SettingsScreen(
    currentIp: String,
    currentPort: String,
    appTheme: ThemeMode,
    currentLanguage: String,
    accentColor: Color,
    backgroundColor: Color,
    textColor: Color,
    isMaleVoice: Boolean,
    onIpSaved: (String) -> Unit,
    onPortSaved: (String) -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    onLanguageChanged: (String) -> Unit,
    onColorChanged: (Color) -> Unit,
    onVoiceGenderChanged: (Boolean) -> Unit
) {
    var ipInput by remember { mutableStateOf(currentIp) }
    var portInput by remember { mutableStateOf(currentPort) }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(SettingsTab.APPEARANCE) }

    val scrollState = rememberScrollState()
    val navColor = if (appTheme == ThemeMode.LIGHT) Color(0xFFF5F5F5) else Color(0xFF1E1E1E)

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val prefs = remember { PreferencesManager(context) }
    val chatDao = remember { AppDatabase.getDatabase(context).chatDao() }
    val syncManager = remember { BackupSyncManager(chatDao, prefs) }

    var syncModeExpanded by remember { mutableStateOf(false) }
    var syncMode by remember { mutableStateOf("local") }

    var ncUrl by remember { mutableStateOf(prefs.getNextcloudUrl()) }
    var ncUser by remember { mutableStateOf(prefs.getNextcloudUser()) }
    var ncPass by remember { mutableStateOf(prefs.getNextcloudPassword()) }
    var syncStatus by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }

    val strBackupLocalProgress = stringResource(R.string.backup_in_progress)
    val strBackupLocalSuccess = stringResource(R.string.backup_local_success)
    val strBackupLocalError = stringResource(R.string.backup_local_error)
    val strRestoreLocalProgress = stringResource(R.string.restore_in_progress)
    val strRestoreSuccess = stringResource(R.string.restore_success)
    val strRestoreInvalid = stringResource(R.string.restore_invalid_file)
    val strRestoreReadError = stringResource(R.string.restore_read_error)
    val strRestoreError = stringResource(R.string.restore_error)
    val strBackupCloudSuccess = stringResource(R.string.backup_cloud_success)
    val strBackupCloudError = stringResource(R.string.backup_cloud_error)
    val strRestoreCloudError = stringResource(R.string.restore_cloud_error)
    val strCredentialsSaved = stringResource(R.string.credentials_saved)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            isSyncing = true
            syncStatus = strBackupLocalProgress
            coroutineScope.launch {
                try {
                    val json = syncManager.getBackupJsonString()
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        out.write(json.toByteArray())
                    }
                    syncStatus = strBackupLocalSuccess
                } catch (e: Exception) {
                    syncStatus = strBackupLocalError
                } finally {
                    isSyncing = false
                }
            }
        } ?: run { isSyncing = false }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            isSyncing = true
            syncStatus = strRestoreLocalProgress
            coroutineScope.launch {
                try {
                    val json = context.contentResolver.openInputStream(it)?.bufferedReader().use { reader -> reader?.readText() }
                    if (json != null) {
                        val success = syncManager.restoreFromJsonString(json)
                        syncStatus = if (success) strRestoreSuccess else strRestoreInvalid
                    } else {
                        syncStatus = strRestoreReadError
                    }
                } catch (e: Exception) {
                    syncStatus = strRestoreError
                } finally {
                    isSyncing = false
                }
            }
        } ?: run { isSyncing = false }
    }

    val settingsContent: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth()) {
            when (selectedTab) {
                SettingsTab.APPEARANCE -> {
                    Text(
                        text = stringResource(R.string.appearance),
                        color = textColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            SettingsRow(label = stringResource(R.string.language), textColor = textColor) {
                                Box {
                                    val currentLangLabel = when (currentLanguage) {
                                        "fr" -> "🇫🇷 Français"
                                        "en" -> "🇬🇧 English"
                                        else -> "⚙️ " + stringResource(R.string.system)
                                    }
                                    TextButton(onClick = { languageMenuExpanded = true }) {
                                        Text(currentLangLabel, color = accentColor)
                                    }
                                    DropdownMenu(
                                        expanded = languageMenuExpanded,
                                        onDismissRequest = { languageMenuExpanded = false },
                                        modifier = Modifier.background(backgroundColor)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("⚙️ " + stringResource(R.string.system), color = textColor) },
                                            onClick = { onLanguageChanged("system"); languageMenuExpanded = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("🇫🇷 Français", color = textColor) },
                                            onClick = { onLanguageChanged("fr"); languageMenuExpanded = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("🇬🇧 English", color = textColor) },
                                            onClick = { onLanguageChanged("en"); languageMenuExpanded = false }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            SettingsRow(label = stringResource(R.string.theme), textColor = textColor) {
                                Row {
                                    ThemeOption(
                                        text = "L",
                                        isSelected = appTheme == ThemeMode.LIGHT,
                                        accentColor = accentColor,
                                        textColor = textColor
                                    ) { onThemeChanged(ThemeMode.LIGHT) }
                                    ThemeOption(
                                        text = "D",
                                        isSelected = appTheme == ThemeMode.DARK,
                                        accentColor = accentColor,
                                        textColor = textColor
                                    ) { onThemeChanged(ThemeMode.DARK) }
                                    ThemeOption(
                                        text = "A",
                                        isSelected = appTheme == ThemeMode.AMOLED,
                                        accentColor = accentColor,
                                        textColor = textColor
                                    ) { onThemeChanged(ThemeMode.AMOLED) }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            SettingsRow(label = stringResource(R.string.color), textColor = textColor) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val colors = listOf(
                                        Color(0xFF64B5F6), Color(0xFF81C784),
                                        Color(0xFFBA68C8), Color(0xFFFFB74D),
                                        Color(0xFFE57373), Color(0xFF4DB6AC)
                                    )
                                    colors.forEach { color ->
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .clickable { onColorChanged(color) }
                                                .border(
                                                    width = if (accentColor == color) 2.dp else 0.dp,
                                                    color = if (accentColor == color) textColor else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                SettingsTab.VOICE -> {
                    Text(
                        text = stringResource(R.string.audio_voice_title),
                        color = textColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = stringResource(R.string.voice_gender),
                                color = textColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                GenderOption(
                                    text = stringResource(R.string.male_voice),
                                    isSelected = isMaleVoice,
                                    accentColor = accentColor,
                                    textColor = textColor,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    onVoiceGenderChanged(true)
                                }

                                GenderOption(
                                    text = stringResource(R.string.female_voice),
                                    isSelected = !isMaleVoice,
                                    accentColor = accentColor,
                                    textColor = textColor,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    onVoiceGenderChanged(false)
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent("com.android.settings.TTS_SETTINGS")
                                        context.startActivity(intent)
                                    } catch (e: Exception) {}
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.manage_system_voices),
                                    color = accentColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                SettingsTab.SERVER -> {
                    Text(
                        text = stringResource(R.string.server),
                        color = textColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            OutlinedTextField(
                                value = ipInput,
                                onValueChange = { ipInput = it },
                                label = { Text(stringResource(R.string.server_ip)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = portInput,
                                onValueChange = { portInput = it },
                                label = { Text(stringResource(R.string.server_port)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor
                                )
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { onIpSaved(ipInput); onPortSaved(portInput) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.apply),
                                    color = Color.Black,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                SettingsTab.SYNC -> {
                    Text(
                        text = stringResource(R.string.sync),
                        color = textColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {

                            SettingsRow(label = stringResource(R.string.sync_method), textColor = textColor) {
                                Box {
                                    TextButton(onClick = { syncModeExpanded = true }) {
                                        Text(
                                            text = if (syncMode == "local") stringResource(R.string.local_storage) else stringResource(R.string.nextcloud),
                                            color = accentColor
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = syncModeExpanded,
                                        onDismissRequest = { syncModeExpanded = false },
                                        modifier = Modifier.background(backgroundColor)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.local_storage), color = textColor) },
                                            onClick = { syncMode = "local"; syncModeExpanded = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.nextcloud), color = textColor) },
                                            onClick = { syncMode = "nextcloud"; syncModeExpanded = false }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (syncMode == "nextcloud") {
                                OutlinedTextField(
                                    value = ncUrl,
                                    onValueChange = { ncUrl = it },
                                    label = { Text(stringResource(R.string.nextcloud_url)) },
                                    supportingText = { Text(stringResource(R.string.nextcloud_url_hint)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = ncUser,
                                    onValueChange = { ncUser = it },
                                    label = { Text(stringResource(R.string.username)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = ncPass,
                                    onValueChange = { ncPass = it },
                                    label = { Text(stringResource(R.string.app_password)) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor
                                    )
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        prefs.saveNextcloudUrl(ncUrl)
                                        prefs.saveNextcloudUser(ncUser)
                                        prefs.saveNextcloudPassword(ncPass)
                                        Toast.makeText(context, strCredentialsSaved, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(stringResource(R.string.save_credentials), color = textColor)
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.local_sync_desc),
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    lineHeight = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(color = textColor.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(24.dp))

                            if (syncStatus.isNotEmpty()) {
                                Text(
                                    text = syncStatus,
                                    color = if (syncStatus.contains("❌")) Color.Red else Color(0xFF81C784),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (syncMode == "nextcloud") {
                                            isSyncing = true
                                            syncStatus = strBackupLocalProgress
                                            coroutineScope.launch {
                                                val success = syncManager.backupToNextcloud()
                                                syncStatus = if (success) strBackupCloudSuccess else strBackupCloudError
                                                isSyncing = false
                                            }
                                        } else {
                                            isSyncing = true
                                            exportLauncher.launch("follia_backup.json")
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isSyncing
                                ) {
                                    Text(stringResource(R.string.backup_btn), color = Color.Black, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (syncMode == "nextcloud") {
                                            isSyncing = true
                                            syncStatus = strRestoreLocalProgress
                                            coroutineScope.launch {
                                                val success = syncManager.restoreFromNextcloud()
                                                syncStatus = if (success) strRestoreSuccess else strRestoreCloudError
                                                isSyncing = false
                                            }
                                        } else {
                                            isSyncing = true
                                            importLauncher.launch(arrayOf("application/json", "*/*"))
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                                    enabled = !isSyncing
                                ) {
                                    Text(stringResource(R.string.restore_btn), color = accentColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                SettingsTab.ABOUT -> {
                    Text(
                        text = stringResource(R.string.about),
                        color = textColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            SettingsRow(label = stringResource(R.string.version), textColor = textColor) {
                                Text("1.0.0", color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            SettingsRow(label = stringResource(R.string.developer), textColor = textColor) {
                                Text("iamtheamn", color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = stringResource(R.string.about_description),
                                fontSize = 14.sp,
                                color = Color.Gray,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/iamtheamn404"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.support_kofi),
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        if (isTablet) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .background(navColor)
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings),
                        color = textColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 32.dp, start = 8.dp, top = 16.dp)
                    )

                    SectionMenuItem(
                        title = stringResource(R.string.appearance),
                        icon = Icons.Default.Face,
                        isSelected = selectedTab == SettingsTab.APPEARANCE,
                        accentColor = accentColor,
                        textColor = textColor
                    ) { selectedTab = SettingsTab.APPEARANCE }

                    Spacer(modifier = Modifier.height(8.dp))

                    SectionMenuItem(
                        title = stringResource(R.string.voice),
                        icon = Icons.Default.RecordVoiceOver,
                        isSelected = selectedTab == SettingsTab.VOICE,
                        accentColor = accentColor,
                        textColor = textColor
                    ) { selectedTab = SettingsTab.VOICE }

                    Spacer(modifier = Modifier.height(8.dp))

                    SectionMenuItem(
                        title = stringResource(R.string.server),
                        icon = Icons.Default.Settings,
                        isSelected = selectedTab == SettingsTab.SERVER,
                        accentColor = accentColor,
                        textColor = textColor
                    ) { selectedTab = SettingsTab.SERVER }

                    Spacer(modifier = Modifier.height(8.dp))

                    SectionMenuItem(
                        title = stringResource(R.string.sync),
                        icon = Icons.Default.CloudSync,
                        isSelected = selectedTab == SettingsTab.SYNC,
                        accentColor = accentColor,
                        textColor = textColor
                    ) { selectedTab = SettingsTab.SYNC }

                    Spacer(modifier = Modifier.height(8.dp))

                    SectionMenuItem(
                        title = stringResource(R.string.about),
                        icon = Icons.Default.Info,
                        isSelected = selectedTab == SettingsTab.ABOUT,
                        accentColor = accentColor,
                        textColor = textColor
                    ) { selectedTab = SettingsTab.ABOUT }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(scrollState)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(modifier = Modifier.widthIn(max = 800.dp)) {
                        settingsContent()
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = backgroundColor,
                    contentColor = accentColor,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = accentColor
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == SettingsTab.APPEARANCE,
                        onClick = { selectedTab = SettingsTab.APPEARANCE },
                        text = { Text(stringResource(R.string.appearance), color = if (selectedTab == SettingsTab.APPEARANCE) accentColor else textColor) }
                    )
                    Tab(
                        selected = selectedTab == SettingsTab.VOICE,
                        onClick = { selectedTab = SettingsTab.VOICE },
                        text = { Text(stringResource(R.string.voice), color = if (selectedTab == SettingsTab.VOICE) accentColor else textColor) }
                    )
                    Tab(
                        selected = selectedTab == SettingsTab.SERVER,
                        onClick = { selectedTab = SettingsTab.SERVER },
                        text = { Text(stringResource(R.string.server), color = if (selectedTab == SettingsTab.SERVER) accentColor else textColor) }
                    )
                    Tab(
                        selected = selectedTab == SettingsTab.SYNC,
                        onClick = { selectedTab = SettingsTab.SYNC },
                        text = { Text(stringResource(R.string.sync), color = if (selectedTab == SettingsTab.SYNC) accentColor else textColor) }
                    )
                    Tab(
                        selected = selectedTab == SettingsTab.ABOUT,
                        onClick = { selectedTab = SettingsTab.ABOUT },
                        text = { Text(stringResource(R.string.about), color = if (selectedTab == SettingsTab.ABOUT) accentColor else textColor) }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    settingsContent()
                }
            }
        }
    }
}

@Composable
fun GenderOption(text: String, isSelected: Boolean, accentColor: Color, textColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) accentColor else Color.Transparent)
            .clickable { onClick() }
            .border(
                width = if (!isSelected) 1.dp else 0.dp,
                color = if (!isSelected) textColor.copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSelected) Color.Black else textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionMenuItem(title: String, icon: ImageVector, isSelected: Boolean, accentColor: Color, textColor: Color, onClick: () -> Unit) {
    val bgColor = if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent
    val contentColor = if (isSelected) accentColor else textColor.copy(alpha = 0.7f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, color = contentColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
fun SettingsRow(label: String, textColor: Color, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        content()
    }
}

@Composable
fun ThemeOption(text: String, isSelected: Boolean, accentColor: Color, textColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(start = 12.dp)
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) accentColor else Color.Transparent)
            .clickable { onClick() }
            .border(
                width = if (!isSelected) 1.dp else 0.dp,
                color = if (!isSelected) textColor.copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSelected) Color.Black else textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}