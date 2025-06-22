package com.sstk_g9.ecoswitch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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

@Composable
fun ConnectionPage(
    device: EcoSwitch?,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    onStopConnecting: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.eco_surface)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Welcome to EcoSwitch",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.eco_text),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        when {
            device?.isConnected == null -> {
                // No device connected
                Button(
                    onClick = if (isConnecting) {
                        onStopConnecting
                    } else {
                        onConnect
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnecting) {
                            colorResource(R.color.eco_accent_dark)  // stop
                        } else {
                            colorResource(R.color.eco_green)        // connect
                        },
                        contentColor = colorResource(R.color.white)
                    ),
                ) {
                    Text(
                        if (isConnecting) "Stop connecting" else "Find & Connect Device"
                    )
                }

                if (isConnecting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        color = colorResource(R.color.eco_green)
                    )
                    Text(
                        text = "Searching for EcoSwitch devices...",
                        modifier = Modifier.padding(top = 8.dp),
                        color = colorResource(R.color.eco_text)
                    )
                }
            }
        }
    }
}

// PREVIEW //
@Preview(showBackground = true)
@Composable
fun PreviewConnectionPage() {
    val fakeDevice = EcoSwitch(
        ipAddress = "192.168.0.200",
        token = "FAKE",
        isConnected = false,
        isOn = false,
        temperature = "N/A"
    )

    ConnectionPage(
        device = null,
        isConnecting = false,
        onConnect = {},
        onStopConnecting = {},
    )
}