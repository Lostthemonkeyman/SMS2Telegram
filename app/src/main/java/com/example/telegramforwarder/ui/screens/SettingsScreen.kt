package com.example.telegramforwarder.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telegramforwarder.data.local.UserPreferences
import com.example.telegramforwarder.data.remote.TelegramRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { UserPreferences(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val botToken by preferences.botToken.collectAsState(initial = "")
    val chatId by preferences.chatId.collectAsState(initial = "")
    val isSmsEnabled by preferences.isSmsEnabled.collectAsState(initial = true)
    val isEmailEnabled by preferences.isEmailEnabled.collectAsState(initial = true)

    var tokenInput by remember { mutableStateOf("") }
    var chatInput by remember { mutableStateOf("") }
    var isTestingConnection by remember { mutableStateOf(false) }

    // Initialize inputs with stored values
    LaunchedEffect(botToken, chatId) {
        if (botToken != null) tokenInput = botToken!!
        if (chatId != null) chatInput = chatId!!
    }

    // Animation state
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = slideInHorizontally(animationSpec = tween(300)) + fadeIn()
                    ) {
                        SettingsSectionTitle("Forwarding Options")
                    }
                }

                item {
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = slideInHorizontally(animationSpec = tween(300, delayMillis = 100)) + fadeIn()
                    ) {
                        SettingsSwitchCard(
                            title = "Forward SMS",
                            subtitle = "Intercept and forward incoming SMS messages",
                            checked = isSmsEnabled,
                            onCheckedChange = { scope.launch { preferences.setSmsEnabled(it) } }
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = slideInHorizontally(animationSpec = tween(300, delayMillis = 200)) + fadeIn()
                    ) {
                        SettingsSwitchCard(
                            title = "Forward Emails",
                            subtitle = "Intercept and forward Gmail notifications",
                            checked = isEmailEnabled,
                            onCheckedChange = { scope.launch { preferences.setEmailEnabled(it) } }
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = slideInHorizontally(animationSpec = tween(300, delayMillis = 300)) + fadeIn()
                    ) {
                        SettingsSectionTitle("Telegram Configuration")
                    }
                }

                item {
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = slideInHorizontally(animationSpec = tween(300, delayMillis = 400)) + fadeIn()
                    ) {
                        BeautifulTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            label = "Bot Token"
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = slideInHorizontally(animationSpec = tween(300, delayMillis = 500)) + fadeIn()
                    ) {
                        BeautifulTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            label = "Chat ID"
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = slideInVertically(animationSpec = tween(300, delayMillis = 600)) + fadeIn()
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    // Save credentials
                                    preferences.saveBotToken(tokenInput)
                                    preferences.saveChatId(chatInput)

                                    // Verify connection
                                    isTestingConnection = true
                                    val response = TelegramRepository.sendMessage(
                                        botToken = tokenInput.trim(),
                                        chatId = chatInput.trim(),
                                        message = "DONE" // As requested by user
                                    )
                                    isTestingConnection = false

                                    if (response.success) {
                                        snackbarHostState.showSnackbar("Configuration saved & Connection verified!")
                                    } else {
                                        snackbarHostState.showSnackbar("Saved, but failed to connect: ${response.message}")
                                    }
                                }
                            },
                            enabled = !isTestingConnection,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verifying...", fontSize = 16.sp)
                            } else {
                                Text("Save & Verify", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                item {
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = slideInVertically(animationSpec = tween(300, delayMillis = 700)) + fadeIn()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            SettingsSectionTitle("Diagnostics")
                        }
                    }
                }

                item {
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = slideInVertically(animationSpec = tween(300, delayMillis = 800)) + fadeIn()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToLogs() },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "View System Logs",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        "Check for errors and connection issues",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Logs",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsSwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (checked) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = if (checked) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeautifulTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        singleLine = true
    )
}
