package com.muh.vollimit

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import com.muh.vollimit.ui.theme.VolLimitTheme
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VolLimitTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    VolumeControlScreen()
                }
            }
        }
    }

    @Composable
    fun VolumeControlScreen() {
        val context = LocalContext.current
        var sliderPosition by remember { mutableStateOf(70f) }
        var showLanguageDialog by remember { mutableStateOf(false) }
        var isServiceRunning by remember { mutableStateOf(false) }
        var showBatteryDialog by remember { mutableStateOf(false) }

        // Poll for service status
        LaunchedEffect(Unit) {
            while (true) {
                isServiceRunning = isServiceRunning(context)
                delay(1000)
            }
        }

        // Check for battery optimization
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                    showBatteryDialog = true
                }
            }
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { }
        )

        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        Scaffold(
            floatingActionButton = {
                SmallFloatingActionButton(
                    onClick = { showLanguageDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                }
            },
            floatingActionButtonPosition = FabPosition.End
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Only show status when active
                    if (isServiceRunning) {
                        StatusIndicator()
                    }

                    // Main Control Card
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.volume_limit, sliderPosition.toInt()),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Slider(
                                value = sliderPosition,
                                onValueChange = { sliderPosition = it },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                )
                            )
                        }
                    }

                    // Action Buttons with scale feedback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AnimatedButton(
                            onClick = {
                                val intent = Intent(context, VolumeControlService::class.java).apply {
                                    putExtra("volumePercentage", sliderPosition.toInt())
                                }
                                ContextCompat.startForegroundService(context, intent)
                            },
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.start))
                        }

                        AnimatedButton(
                            onClick = {
                                context.stopService(Intent(context, VolumeControlService::class.java))
                            },
                            modifier = Modifier.weight(1f),
                            isOutlined = true
                        ) {
                            Text(stringResource(R.string.stop))
                        }
                    }
                }
            }
        }

        if (showLanguageDialog) {
            LanguageDialog(onDismiss = { showLanguageDialog = false })
        }

        if (showBatteryDialog) {
            BatteryOptimizationDialog(
                onDismiss = { showBatteryDialog = false },
                onAllow = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                    showBatteryDialog = false
                }
            )
        }
    }

    @Composable
    fun StatusIndicator() {
        val color = Color(0xFF4CAF50)
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = color,
                    modifier = Modifier.size(6.dp)
                ) {}
                Text(
                    text = stringResource(R.string.status_active),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }

    @Composable
    fun AnimatedButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        isOutlined: Boolean = false,
        containerColor: Color = MaterialTheme.colorScheme.surface,
        contentColor: Color = MaterialTheme.colorScheme.primary,
        content: @Composable RowScope.() -> Unit
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "ScaleAnimation")

        if (isOutlined) {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.scale(scale),
                interactionSource = interactionSource,
                shape = RoundedCornerShape(16.dp),
                content = content
            )
        } else {
            Button(
                onClick = onClick,
                modifier = modifier.scale(scale),
                interactionSource = interactionSource,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
                content = content
            )
        }
    }

    @Composable
    fun BatteryOptimizationDialog(onDismiss: () -> Unit, onAllow: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.battery_optimization_title)) },
            text = { Text(stringResource(R.string.battery_optimization_message)) },
            confirmButton = {
                TextButton(onClick = onAllow) {
                    Text(stringResource(R.string.allow))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.ignore))
                }
            }
        )
    }

    @Composable
    fun LanguageDialog(onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.change_language)) },
            text = {
                Column {
                    LanguageOption(stringResource(R.string.language_english), "en") {
                        changeLanguage("en")
                        onDismiss()
                    }
                    LanguageOption(stringResource(R.string.language_arabic), "ar") {
                        changeLanguage("ar")
                        onDismiss()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            }
        )
    }

    @Composable
    fun LanguageOption(label: String, code: String, onClick: () -> Unit) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label, modifier = Modifier.padding(8.dp))
        }
    }

    private fun changeLanguage(langCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(langCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    private fun isServiceRunning(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (VolumeControlService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
