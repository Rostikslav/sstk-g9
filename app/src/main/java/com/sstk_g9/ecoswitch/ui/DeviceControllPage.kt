package com.sstk_g9.ecoswitch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sstk_g9.ecoswitch.R
import com.sstk_g9.ecoswitch.model.EcoSwitch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.sstk_g9.ecoswitch.logic.PowerManager
import kotlinx.coroutines.delay

@Composable
fun DeviceControlPage(
    device: EcoSwitch,
    onToggle: (EcoSwitch) -> Unit,
    onDisconnect: () -> Unit,
    onStatusUpdate: (EcoSwitch?) -> Unit
) {
    // Zustand für den Timer
    var timerHours by remember { mutableStateOf(0) }
    var timerMinutes by remember { mutableStateOf(0) }
    var timerSeconds by remember { mutableStateOf(0) }

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
        //Timer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Timer-Anzeige Labels
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimerLabel("HRS")
                TimerLabel("MINS")
                TimerLabel("SECS")
            }

            Spacer(modifier = Modifier.height(1.dp))

            // Timer-Zeitanzeige
            Text(
                // Formatiert die Zeit, sodass immer zwei Ziffern angezeigt werden (z.B. 05 statt 5)
                text = "%02d:%02d:%02d".format(timerHours, timerMinutes, timerSeconds),
                fontSize = 48.sp, // Größere Schrift für die Zeit
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.eco_text)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Timer-Aktionsknöpfe
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp), // Abstand zwischen den Knöpfen
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimerActionButton(
                    text = "Start",
                    onClick = { /* TODO: Start Timer Logik */ }
                )
                TimerActionButton(
                    text = "Edit",
                    onClick = { /* TODO: Edit Timer Logik */ }
                )
                TimerActionButton(
                    text = "Cancel",
                    onClick = { /* TODO: Cancel Timer Logik */ }
                )
            }
            Spacer(modifier = Modifier.height(100.dp))

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
}
    @Composable
    fun TimerActionButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        buttonSize: Dp = 64.dp, // Größe der runden Buttons
        textColor: Color = colorResource(R.color.eco_text), // Farbe für den Text
        backgroundColor: Color = colorResource(R.color.eco_green), // Farbe für den Hintergrund
    ) {
        Box( // Box verwenden, um den Text im Kreis perfekt zu zentrieren
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(buttonSize)
                .clip(CircleShape) // Macht den Button rund
                .background(backgroundColor)
                .clickable(onClick = onClick)
                .padding(8.dp) // Inneres Padding, damit der Text nicht am Rand klebt
        ) {
            Text(
                text = text.uppercase(), // Text in Großbuchstaben für besseren Button-Look
                color = colorResource(R.color.white),
                fontSize = 12.sp, // Anpassen nach Bedarf
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
@Composable
fun TimerLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = colorResource(R.color.eco_accent_dark),//Eine subtiler Farbe
        modifier = Modifier.width(50.dp), // Gib jedem Label eine feste Breite für bessere Ausrichtung
        textAlign = TextAlign.Center
    )
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