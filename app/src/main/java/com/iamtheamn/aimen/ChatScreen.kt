package com.iamtheamn.aimen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    currentIp: String,
    currentPort: String,
    appTheme: ThemeMode,
    accentColor: Color,
    backgroundColor: Color,
    textColor: Color
) {
    var inputText by remember { mutableStateOf("") }
    val messages = viewModel.messages
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(currentIp, currentPort) {
        viewModel.fetchModels(ipAddress = currentIp, port = currentPort)
    }

    var isDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message = message, appTheme = appTheme, accentColor = accentColor)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                TextButton(onClick = { isDropdownExpanded = true }) {
                    Text(
                        text = viewModel.selectedModel.value.ifBlank { "Modèle" },
                        color = accentColor,
                        fontSize = 12.sp
                    )
                }

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    viewModel.availableModels.forEach { modelName ->
                        DropdownMenuItem(
                            text = { Text(modelName) },
                            onClick = {
                                viewModel.selectedModel.value = modelName
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            val fieldBorderColor = if (appTheme == ThemeMode.LIGHT) Color.LightGray else Color.DarkGray

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    val placeholderText = if (viewModel.selectedModel.value.isNotBlank()) {
                        "Parler à ${viewModel.selectedModel.value}"
                    } else {
                        stringResource(R.string.type_message)
                    }
                    Text(text = placeholderText, color = Color.Gray)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = fieldBorderColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = accentColor
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    val textToSend = inputText
                    inputText = ""
                    viewModel.sendMessage(userText = textToSend, ipAddress = currentIp, port = currentPort)
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = accentColor
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, appTheme: ThemeMode, accentColor: Color) {
    val isUser = message.isUser
    val aiBubbleColor = when (appTheme) {
        ThemeMode.LIGHT -> Color(0xFFE0E0E0)
        ThemeMode.DARK -> Color(0xFF2C2C2C)
        ThemeMode.AMOLED -> Color(0xFF1E1E1E)
    }
    val aiTextColor = if (appTheme == ThemeMode.LIGHT) Color.Black else Color.White
    val bubbleBackgroundColor = if (isUser) accentColor else aiBubbleColor
    val messageTextColor = if (isUser) Color.Black else aiTextColor

    val codeBackgroundColor = when (appTheme) {
        ThemeMode.LIGHT -> Color(0xFFF5F5F5)
        ThemeMode.DARK -> Color(0xFF1A1A1A)
        ThemeMode.AMOLED -> Color(0xFF000000)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = bubbleBackgroundColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            SelectionContainer {
                if (isUser) {
                    Text(
                        text = message.text,
                        color = messageTextColor,
                        fontSize = 16.sp
                    )
                } else {
                    Markdown(
                        content = message.text,
                        colors = markdownColor(
                            text = messageTextColor,
                            codeText = messageTextColor,
                            codeBackground = codeBackgroundColor,
                            linkText = accentColor
                        ),
                        typography = markdownTypography(
                            h1 = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                            h2 = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                            h3 = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            h4 = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                            h5 = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                            h6 = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                            text = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}