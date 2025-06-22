package com.sstk_g9.ecoswitch
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.sstk_g9.ecoswitch.ui.DeviceControlPage
import com.sstk_g9.ecoswitch.ui.ConnectionPage
import com.sstk_g9.ecoswitch.logic.ConnectionManager
import com.sstk_g9.ecoswitch.logic.PowerManager
import com.sstk_g9.ecoswitch.model.EcoSwitch


@Composable
fun EcoSwitchApp() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("ecoswitch", Context.MODE_PRIVATE)

    var device by remember { mutableStateOf<EcoSwitch?>(null) }
    var isConnecting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Load saved device on startup
    LaunchedEffect(Unit) {
        ConnectionManager.loadSavedDevice(prefs)?.let { savedDevice ->
            device = savedDevice
            // Try to reconnect
            scope.launch {
                PowerManager.checkDeviceStatus(savedDevice)?.let { updatedDevice ->
                    device = updatedDevice
                }
            }
        }
    }

    Crossfade(
        targetState = device,
        modifier = Modifier.fillMaxSize(),
        animationSpec = tween(300, easing = EaseInOutCubic),
        label = "PageTransition"
    ) { currentDevice -> // targetState
        if (currentDevice?.isConnected == true) {
            // Connected device - show control page
            DeviceControlPage(
                device = currentDevice,
                onToggle = { selectedDevice ->
                    scope.launch {
                        PowerManager.toggleDevice(selectedDevice)?.let { updatedDevice ->
                            device = updatedDevice
                        } ?: run {
                            Toast.makeText(
                                context,
                                "Failed to toggle device",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                onDisconnect = {
                    scope.launch {
                        if(ConnectionManager.disconnect(currentDevice, prefs))  device = null
                        else Toast.makeText(context, "Disconnect failed", Toast.LENGTH_SHORT).show()
                    }
                },
                onStatusUpdate = { updated ->
                    device = updated
                }
            )
        } else {
            // No device -> show connection page
            ConnectionPage(
                device = device,
                isConnecting = isConnecting,
                onConnect = {
                    scope.launch {
                        isConnecting = true
                        ConnectionManager.findAndConnectDevice { success, connectedDevice ->
                            isConnecting = false
                            if (success && connectedDevice != null) {
                                device = connectedDevice
                                ConnectionManager.saveDevice(prefs, connectedDevice)
                            } else {
                                Toast.makeText(
                                    context,
                                    "No device found or connection failed",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                },
                onStopConnecting = {
                    isConnecting = false
                    ConnectionManager.stopDiscovery()
                }
            )
        }
    }
}