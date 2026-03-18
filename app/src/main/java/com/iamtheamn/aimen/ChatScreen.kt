package com.iamtheamn.aimen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    currentIp: String,
    appTheme: ThemeMode,
    accentColor: Color,
    backgroundColor: Color,
    textColor: Color
) {
    var inputText by remember { mutableStateOf("") }
    val messages = viewModel.messages

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        LazyColumn(
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
            val fieldBorderColor = if (appTheme == ThemeMode.LIGHT) Color.LightGray else Color.DarkGray

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.type_message), color = Color.Gray) },
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
                    viewModel.sendMessage(inputText, currentIp)
                    inputText = ""
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
                Text(
                    text = message.text,
                    color = messageTextColor,
                    fontSize = 16.sp
                )
            }
        }
    }
}