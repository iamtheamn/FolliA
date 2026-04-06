package com.iamtheamn.aimen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    currentIp: String,
    currentPort: String,
    appTheme: ThemeMode,
    currentLanguage: String,
    accentColor: Color,
    backgroundColor: Color,
    textColor: Color,
    onIpSaved: (String) -> Unit,
    onPortSaved: (String) -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    onLanguageChanged: (String) -> Unit,
    onColorChanged: (Color) -> Unit
) {
    var ipInput by remember { mutableStateOf(currentIp) }
    var portInput by remember { mutableStateOf(currentPort) }
    var languageMenuExpanded by remember { mutableStateOf(false) }

    val colorPalette = listOf(
        Color(0xFF64B5F6),
        Color(0xFF81C784),
        Color(0xFFBA68C8),
        Color(0xFFFFB74D),
        Color(0xFFE57373),
        Color(0xFF4DB6AC)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.settings),
            color = textColor,
            fontSize = 28.sp,
            modifier = Modifier.padding(bottom = 32.dp, top = 32.dp)
        )

        Text(stringResource(R.string.language), color = accentColor, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            val currentLangLabel = when (currentLanguage) {
                "fr" -> "🇫🇷 Français"
                "en" -> "🇬🇧 English"
                else -> "⚙️ " + stringResource(R.string.system)
            }

            OutlinedButton(
                onClick = { languageMenuExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
                border = BorderStroke(1.dp, if (appTheme == ThemeMode.LIGHT) Color.LightGray else Color.DarkGray)
            ) {
                Text(currentLangLabel, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
            }

            DropdownMenu(
                expanded = languageMenuExpanded,
                onDismissRequest = { languageMenuExpanded = false },
                modifier = Modifier.background(backgroundColor).fillMaxWidth(0.85f)
            ) {
                DropdownMenuItem(
                    text = { Text("⚙️ " + stringResource(R.string.system), color = textColor) },
                    onClick = {
                        languageMenuExpanded = false
                        onLanguageChanged("system")
                    }
                )
                DropdownMenuItem(
                    text = { Text("🇫🇷 Français", color = textColor) },
                    onClick = {
                        languageMenuExpanded = false
                        onLanguageChanged("fr")
                    }
                )
                DropdownMenuItem(
                    text = { Text("🇬🇧 English", color = textColor) },
                    onClick = {
                        languageMenuExpanded = false
                        onLanguageChanged("en")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(stringResource(R.string.appearance), color = accentColor, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ThemeButton(stringResource(R.string.theme_light), appTheme == ThemeMode.LIGHT, accentColor, textColor) { onThemeChanged(ThemeMode.LIGHT) }
            ThemeButton(stringResource(R.string.theme_dark), appTheme == ThemeMode.DARK, accentColor, textColor) { onThemeChanged(ThemeMode.DARK) }
            ThemeButton(stringResource(R.string.theme_amoled), appTheme == ThemeMode.AMOLED, accentColor, textColor) { onThemeChanged(ThemeMode.AMOLED) }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(stringResource(R.string.accent_color), color = accentColor, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            colorPalette.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (accentColor == color) 3.dp else 0.dp,
                            color = if (accentColor == color) textColor else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onColorChanged(color) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(stringResource(R.string.server), color = accentColor, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))

        val fieldBorderColor = if (appTheme == ThemeMode.LIGHT) Color.LightGray else Color.DarkGray

        OutlinedTextField(
            value = ipInput,
            onValueChange = { ipInput = it },
            label = { Text(stringResource(R.string.server_ip), color = Color.Gray) },
            placeholder = { Text("Ex: 192.168.1.1", color = Color.Gray.copy(alpha = 0.6f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = fieldBorderColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedLabelColor = accentColor
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = portInput,
            onValueChange = { portInput = it },
            label = { Text("Port (Défaut: 11434)", color = Color.Gray) },
            placeholder = { Text("11434", color = Color.Gray.copy(alpha = 0.6f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = fieldBorderColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedLabelColor = accentColor
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onIpSaved(ipInput)
                onPortSaved(portInput)
            },
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.save), color = Color.Black, fontSize = 16.sp)
        }
    }
}

@Composable
fun ThemeButton(text: String, isSelected: Boolean, accentColor: Color, textColor: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) accentColor else Color.Transparent
        ),
        border = if (isSelected) null else BorderStroke(1.dp, textColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Text(text, color = if (isSelected) Color.Black else textColor, fontSize = 12.sp)
    }
}