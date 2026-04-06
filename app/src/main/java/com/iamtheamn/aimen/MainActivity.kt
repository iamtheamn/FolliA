package com.iamtheamn.aimen

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("AIMenPrefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("app_language", "system") ?: "system"
        val context = if (lang != "system") {
            val locale = Locale.forLanguageTag(lang)
            Locale.setDefault(locale)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            newBase.createConfigurationContext(config)
        } else {
            newBase
        }
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("AIMenPrefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("app_language", "system") ?: "system"
        if (lang != "system") {
            val locale = Locale.forLanguageTag(lang)
            Locale.setDefault(locale)
            val config = Configuration(resources.configuration)
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIMenApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIMenApp() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val database = remember { AppDatabase.getDatabase(context) }

    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(database.chatDao())
    )

    var currentScreen by remember { mutableStateOf("chat") }
    var savedIpAddress by remember { mutableStateOf(prefs.getIpAddress()) }
    var savedPort by remember { mutableStateOf(prefs.getPort()) }
    var appTheme by remember { mutableStateOf(prefs.getTheme()) }
    var appLanguage by remember { mutableStateOf(prefs.getLanguage()) }
    var accentColor by remember { mutableStateOf(prefs.getColor()) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val backgroundColor = when (appTheme) {
        ThemeMode.LIGHT -> Color(0xFFFFFFFF)
        ThemeMode.DARK -> Color(0xFF121212)
        ThemeMode.AMOLED -> Color(0xFF000000)
    }

    val navBarColor = when (appTheme) {
        ThemeMode.LIGHT -> Color(0xFFF5F5F5)
        ThemeMode.DARK -> Color(0xFF1E1E1E)
        ThemeMode.AMOLED -> Color(0xFF0A0A0A)
    }

    val textColor = if (appTheme == ThemeMode.LIGHT) Color.Black else Color.White
    val unselectedIconColor = if (appTheme == ThemeMode.LIGHT) Color.Gray else Color.DarkGray

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = navBarColor,
                drawerContentColor = textColor
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    color = accentColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                HorizontalDivider(color = backgroundColor)

                Spacer(modifier = Modifier.height(16.dp))

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.new_chat)) },
                    selected = false,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = {
                        chatViewModel.createNewConversation()
                        scope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        unselectedTextColor = textColor,
                        unselectedIconColor = accentColor
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.chat_history),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    items(chatViewModel.conversations) { conv ->
                        val isSelected = chatViewModel.currentConversationId.value == conv.id
                        NavigationDrawerItem(
                            label = { Text(conv.title, maxLines = 1) },
                            selected = isSelected,
                            onClick = {
                                chatViewModel.selectConversation(conv.id)
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                selectedTextColor = accentColor,
                                unselectedContainerColor = Color.Transparent,
                                unselectedTextColor = textColor
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = backgroundColor,
            topBar = {
                if (currentScreen == "chat") {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name), color = textColor, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = null, tint = textColor)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = backgroundColor
                        )
                    )
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = navBarColor,
                    contentColor = textColor
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Chat") },
                        selected = currentScreen == "chat",
                        onClick = { currentScreen = "chat" },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accentColor,
                            selectedTextColor = accentColor,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = unselectedIconColor,
                            unselectedTextColor = unselectedIconColor
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.settings)) },
                        selected = currentScreen == "settings",
                        onClick = { currentScreen = "settings" },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accentColor,
                            selectedTextColor = accentColor,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = unselectedIconColor,
                            unselectedTextColor = unselectedIconColor
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentScreen) {
                    "chat" -> ChatScreen(
                        viewModel = chatViewModel,
                        currentIp = savedIpAddress,
                        currentPort = savedPort,
                        appTheme = appTheme,
                        accentColor = accentColor,
                        backgroundColor = backgroundColor,
                        textColor = textColor
                    )
                    "settings" -> SettingsScreen(
                        currentIp = savedIpAddress,
                        currentPort = savedPort,
                        appTheme = appTheme,
                        currentLanguage = appLanguage,
                        accentColor = accentColor,
                        backgroundColor = backgroundColor,
                        textColor = textColor,
                        onIpSaved = { newIp ->
                            savedIpAddress = newIp
                            prefs.saveIpAddress(newIp)
                            Toast.makeText(context, context.getString(R.string.save), Toast.LENGTH_SHORT).show()
                        },
                        onPortSaved = { newPort ->
                            savedPort = newPort
                            prefs.savePort(newPort)
                        },
                        onThemeChanged = { newTheme ->
                            appTheme = newTheme
                            prefs.saveTheme(newTheme)
                        },
                        onLanguageChanged = { newLang ->
                            appLanguage = newLang
                            prefs.saveLanguage(newLang)
                            (context as Activity).recreate()
                        },
                        onColorChanged = { newColor ->
                            accentColor = newColor
                            prefs.saveColor(newColor)
                        }
                    )
                }
            }
        }
    }
}