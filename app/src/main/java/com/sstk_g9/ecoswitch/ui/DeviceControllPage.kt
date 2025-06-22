package com.sstk_g9.ecoswitch.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sstk_g9.ecoswitch.R
import com.sstk_g9.ecoswitch.model.EcoSwitch
import androidx.compose.runtime.LaunchedEffect
import com.sstk_g9.ecoswitch.logic.PowerManager
import kotlinx.coroutines.delay

@Composable
fun DeviceControlPage(
    device: EcoSwitch,
    onToggle: (EcoSwitch) -> Unit,
    onDisconnect: () -> Unit,
    onStatusUpdate: (EcoSwitch?) -> Unit
) {
    LaunchedEffect(device.ipAddress) {
        while (device.isConnected) {
            val updated = PowerManager.checkDeviceStatus(device)
            onStatusUpdate(updated)
            delay(3_000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Device status section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "EcoSwitch",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.eco_text)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Status indicator
            Text(
                text = if (device.isOn) "DEVICE IS ON" else "DEVICE IS OFF",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (device.isOn) {
                    colorResource(R.color.eco_green)
                } else {
                    colorResource(R.color.eco_text)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Temperature display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.eco_surface)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Temperature",
                        fontSize = 16.sp,
                        color = colorResource(R.color.eco_text)
                    )
                    Text(
                        text = "${device.temperature} °C",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.eco_text)
                    )
                }
            }
        }

        // Control buttons section
        Column {
            // Main toggle button
            Button(
                onClick = { onToggle(device) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (device.isOn) {
                        colorResource(R.color.eco_accent_dark)  // Orange for OFF
                    } else {
                        colorResource(R.color.eco_green)  // Green for ON
                    },
                    contentColor = colorResource(R.color.white)
                )
            ) {
                Text(
                    text = if (device.isOn) "TURN OFF" else "TURN ON",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDisconnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.eco_dark_green),
                    contentColor = colorResource(R.color.white)
                )
            ) {
                Text(
                    text = "Disconnect",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// PREVIEW //
@Preview(showBackground = true)
@Composable
fun DeviceControlPagePreview() {
    val testDevice = EcoSwitch(
        ipAddress = "192.168.0.1",
        token = "FAKE_TOKEN",
        isConnected = true,
        isOn = true,
        temperature = "23.4"
    )

    DeviceControlPage(
        device = testDevice,
        onToggle = {},
        onDisconnect = {},
        onStatusUpdate = {}
    )
}