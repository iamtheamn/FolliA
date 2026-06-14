package com.iamtheamn.aimen

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import kotlinx.coroutines.delay

@Composable
fun FolliVoiceIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier.size(24.dp)) {
        val cx = size.width / 2
        val cy = size.height / 2
        val outerRadius = size.width / 2
        val innerRadius = outerRadius * 0.45f
        val path = Path()
        val numPoints = 8
        for (i in 0 until numPoints * 2) {
            val angle = i * Math.PI / numPoints
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val x = cx + r * Math.cos(angle).toFloat()
            val y = cy + r * Math.sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path = path, color = color, style = Stroke(width = 1.5.dp.toPx()))
        val waveHeightMax = innerRadius * 0.9f
        val spacing = innerRadius * 0.6f
        drawLine(color, Offset(cx - spacing, cy - waveHeightMax * 0.5f), Offset(cx - spacing, cy + waveHeightMax * 0.5f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, Offset(cx, cy - waveHeightMax), Offset(cx, cy + waveHeightMax), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, Offset(cx + spacing, cy - waveHeightMax * 0.5f), Offset(cx + spacing, cy + waveHeightMax * 0.5f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
fun GeometricPulsingStar(modifier: Modifier = Modifier, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scaleFactor by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "pulseScale"
    )
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(20000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "pulseRotate"
    )
    Box(modifier = modifier.size(100.dp).scale(scaleFactor).rotate(rotationAngle), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val numPoints = 8
            val outerRadius = size.minDimension / 2
            val innerRadius = outerRadius * 0.4f
            val cx = size.width / 2
            val cy = size.height / 2
            val path = Path()
            for (i in 0 until numPoints * 2) {
                val angle = i * Math.PI / numPoints
                val r = if (i % 2 == 0) outerRadius else innerRadius
                val x = cx + r * Math.cos(angle).toFloat()
                val y = cy + r * Math.sin(angle).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color = color, alpha = 0.2f)
            val numLines = 8
            for (j in 0 until numLines) {
                val angleLine = j * 2 * Math.PI / numLines
                val endX = cx + outerRadius * Math.cos(angleLine).toFloat()
                val endY = cy + outerRadius * Math.sin(angleLine).toFloat()
                drawLine(color = color, start = Offset(cx, cy), end = Offset(endX, endY), strokeWidth = 1.5.dp.toPx(), alpha = 0.6f)
            }
        }
    }
}

@Composable
fun GeometricEnergyWave(modifier: Modifier = Modifier, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val wavePosition by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "wavePosition"
    )
    Box(modifier = modifier.fillMaxWidth().height(30.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val numSteps = 60
            val waveHeight = height * 0.5f
            val waveWidth = width / numSteps
            for (i in 0 until numSteps) {
                val x1 = (i + wavePosition * numSteps) * waveWidth % width
                val y1 = height / 2 + waveHeight * Math.sin((i + wavePosition * numSteps) * Math.PI / numSteps * 4).toFloat()
                val x2 = (i + 1 + wavePosition * numSteps) * waveWidth % width
                val y2 = height / 2 + waveHeight * Math.sin((i + 1 + wavePosition * numSteps) * Math.PI / numSteps * 4).toFloat()
                val alpha = if (x1 < width / 2) x1 / (width / 2) else (width - x1) / (width / 2)
                drawLine(color = color, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = 2.dp.toPx(), alpha = alpha.coerceIn(0.05f, 0.8f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val context = LocalContext.current
    val sttManager = remember { SttManager(context) }
    var isListening by remember { mutableStateOf(false) }
    var showVoiceMenu by remember { mutableStateOf(false) }

    val startMic = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            showVoiceMenu = true
            viewModel.isTtsEnabled.value = true
            sttManager.startListening(
                initialText = inputText,
                onResult = { inputText = it },
                onSilence = {
                    if (inputText.isNotBlank()) {
                        val textToSend = inputText
                        inputText = ""
                        viewModel.sendMessage(userText = textToSend, ipAddress = currentIp, port = currentPort)
                    }
                },
                onError = { /* Ignoré pour ne pas casser la boucle infinie */ },
                onStateChanged = { isListening = it }
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) startMic()
    }

    // --- LA MAGIE : LA BOUCLE INFINIE ---
    LaunchedEffect(viewModel.isAiResponding.value) {
        if (!viewModel.isAiResponding.value && showVoiceMenu) {
            delay(500)
            while (viewModel.isTtsSpeaking()) {
                delay(200)
            }
            if (showVoiceMenu && !isListening) {
                startMic()
            }
        }
    }

    DisposableEffect(Unit) { onDispose { sttManager.destroy() } }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }
    LaunchedEffect(currentIp, currentPort) { viewModel.fetchModels(currentIp, currentPort) }

    var isDropdownExpanded by remember { mutableStateOf(false) }

    // --- LE PANNEAU VOCAL FOLLIA ---
    if (showVoiceMenu) {
        ModalBottomSheet(
            onDismissRequest = {
                sttManager.stopListening()
                viewModel.stopAllAudio()
                viewModel.isTtsEnabled.value = false
                if (inputText.isNotBlank()) {
                    val textToSend = inputText
                    inputText = ""
                    viewModel.sendMessage(userText = textToSend, ipAddress = currentIp, port = currentPort)
                }
                showVoiceMenu = false
            },
            containerColor = Color.Transparent,
            scrimColor = Color.Black.copy(alpha = 0.6f),
            dragHandle = null
        ) {
            Surface(
                color = backgroundColor.copy(alpha = 0.98f),
                modifier = Modifier.fillMaxWidth().heightIn(min = 450.dp),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = " FolliA Voice ", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = {
                            sttManager.stopListening()
                            viewModel.stopAllAudio()
                            viewModel.isTtsEnabled.value = false
                            showVoiceMenu = false
                        }) { Icon(Icons.Default.Close, contentDescription = null, tint = textColor) }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                    GeometricPulsingStar(color = accentColor)

                    val statusText = if (isListening) "FolliA vous écoute..." else if (viewModel.isAiResponding.value || viewModel.isTtsSpeaking()) "FolliA répond..." else "Connexion..."
                    Text(text = statusText, color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 20.dp))

                    val displayText = if ((viewModel.isAiResponding.value || viewModel.isTtsSpeaking()) && messages.isNotEmpty() && !messages.last().isUser) {
                        messages.last().text
                    } else {
                        inputText.ifBlank { "..." }
                    }

                    Text(
                        text = displayText, color = textColor, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(vertical = 30.dp).verticalScroll(rememberScrollState())
                    )

                    GeometricEnergyWave(color = accentColor)
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // --- INTERFACE CHAT ---
    Column(modifier = Modifier.fillMaxSize().background(backgroundColor), horizontalAlignment = Alignment.CenterHorizontally) {
        LazyColumn(state = listState, modifier = Modifier.weight(1f).widthIn(max = 850.dp).fillMaxWidth().padding(horizontal = 8.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
            items(messages) { message -> MessageBubble(message = message, appTheme = appTheme, accentColor = accentColor) }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.widthIn(max = 850.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

                Box {
                    TextButton(onClick = { isDropdownExpanded = true }) {
                        Text(
                            text = viewModel.selectedModel.value.ifBlank { stringResource(R.string.model) },
                            color = accentColor,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 100.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.background(backgroundColor)
                    ) {
                        viewModel.availableModels.forEach { modelName ->
                            DropdownMenuItem(
                                text = { Text(modelName, color = textColor) },
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
                    placeholder = { Text(text = stringResource(R.string.type_message), color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, focusedTextColor = textColor, unfocusedTextColor = textColor, unfocusedBorderColor = fieldBorderColor),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))

                IconButton(onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startMic()
                    else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) { FolliVoiceIcon(color = accentColor) }

                IconButton(onClick = {
                    val t = inputText; inputText = ""
                    viewModel.isTtsEnabled.value = false
                    viewModel.stopAllAudio()
                    viewModel.sendMessage(t, currentIp, currentPort)
                }) { Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = accentColor) }
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
    val codeBackgroundColor = if (appTheme == ThemeMode.LIGHT) Color(0xFFF5F5F5) else Color(0xFF1A1A1A)
    val bubbleShape = if (isUser) RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 4.dp)
    else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 24.dp)

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 12.dp), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Box(modifier = Modifier.fillMaxWidth(0.85f), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
            Box(modifier = Modifier.background(color = bubbleBackgroundColor, shape = bubbleShape).padding(horizontal = 18.dp, vertical = 14.dp)) {
                SelectionContainer {
                    if (isUser) {
                        Text(text = message.text, color = messageTextColor, fontSize = 16.sp)
                    } else {
                        Markdown(
                            content = message.text,
                            colors = markdownColor(text = messageTextColor, codeText = messageTextColor, codeBackground = codeBackgroundColor, linkText = accentColor),
                            typography = markdownTypography(text = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}