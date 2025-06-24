package com.sstk_g9.ecoswitch.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.sstk_g9.ecoswitch.logic.PowerManager
import kotlinx.coroutines.delay
import com.sstk_g9.ecoswitch.logic.TimerManager
import kotlinx.coroutines.launch

@Composable
fun DeviceControlPage(
    device: EcoSwitch,
    onToggle: (EcoSwitch) -> Unit,
    onDisconnect: () -> Unit,
    onStatusUpdate: (EcoSwitch?) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val hoursFocusRequester = remember { FocusRequester() }
    val minutesFocusRequester = remember { FocusRequester() }
    val secondsFocusRequester = remember { FocusRequester() }

    LaunchedEffect(device.ipAddress) {
        while (device.isConnected) {
            val updated = PowerManager.checkDeviceStatus(device)
            onStatusUpdate(updated)
            delay(3_000)
        }
    }
    LaunchedEffect(Unit) { // Führe dies nur einmal aus, wenn die Composable erstellt wird
        // Beispiel: Setze einen Standard-Timer von 1 Minute, wenn die Seite geladen wird
        // Dies wird nur gesetzt, wenn der Timer nicht bereits läuft oder editiert wird.
        TimerManager.setInitialTime(0, 1, 0)
    }

    LaunchedEffect(TimerManager.isEditingTime) {
        if (TimerManager.isEditingTime) {
            delay(50)
            hoursFocusRequester.requestFocus()
        }
    }

    // 2. cancelEditMode() verwenden - mit BackHandler und optional einem Button
    if (TimerManager.isEditingTime) {
        BackHandler { // Android-Zurücktaste fängt den Edit-Modus ab
            TimerManager.cancelEditMode()
            focusManager.clearFocus()
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
            modifier = Modifier
                .weight(1f) // Nimmt den verbleibenden Hauptplatz ein
                // Klick außerhalb der Timer-Eingabefelder, um Edit-Modus zu beenden (mit Speichern)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null // Kein visueller Effekt für diesen äußeren Klick
                ) {
                    if (TimerManager.isEditingTime) {
                        TimerManager.saveEditedTimeAndExitEditMode()
                        focusManager.clearFocus() // Tastatur ausblenden
                    }
                }
                .padding(bottom = 0.dp), // Kein extra Padding unten, da der Disconnect Button separat ist
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimerLabel("HRS"); TimerLabel("MINS"); TimerLabel("SECS")
            }
            Spacer(modifier = Modifier.height(1.dp))

            if (TimerManager.isEditingTime) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable( // Verhindere Klick-Propagation nach außen für die Eingabefeld-Reihe
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                ) {
                    TimerInputField(
                        value = TimerManager.editHoursInput,
                        onValueChange = { newValue ->
                            if (newValue.length <= 2) TimerManager.editHoursInput =
                                newValue.filter(Char::isDigit)
                            if (newValue.length == 2) minutesFocusRequester.requestFocus()
                        },
                        label = "HH",
                        modifier = Modifier.focusRequester(hoursFocusRequester),
                        keyboardActions = KeyboardActions(onNext = { minutesFocusRequester.requestFocus() })
                    )
                    Text(
                        ":",
                        fontSize = 36.sp,
                        color = colorResource(R.color.eco_text),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    TimerInputField(
                        value = TimerManager.editMinutesInput,
                        onValueChange = { newValue ->
                            if (newValue.length <= 2) TimerManager.editMinutesInput =
                                newValue.filter(Char::isDigit)
                            if (newValue.length == 2) secondsFocusRequester.requestFocus()
                        },
                        label = "MM",
                        modifier = Modifier.focusRequester(minutesFocusRequester),
                        keyboardActions = KeyboardActions(onNext = { secondsFocusRequester.requestFocus() })
                    )
                    Text(
                        ":",
                        fontSize = 36.sp,
                        color = colorResource(R.color.eco_text),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    TimerInputField(
                        value = TimerManager.editSecondsInput,
                        onValueChange = { newValue ->
                            if (newValue.length <= 2) TimerManager.editSecondsInput =
                                newValue.filter(Char::isDigit)
                        },
                        label = "SS",
                        modifier = Modifier.focusRequester(secondsFocusRequester),
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(onDone = {
                            TimerManager.saveEditedTimeAndExitEditMode()
                            focusManager.clearFocus()
                        })
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Expliziter "Abbrechen"-Button
                    IconButton(onClick = {
                        TimerManager.cancelEditMode()
                        focusManager.clearFocus()
                    }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Cancel Edit",
                            tint = colorResource(R.color.eco_accent_dark)
                        )
                    }
                    // Expliziter "Speichern"-Button
                    IconButton(onClick = {
                        TimerManager.saveEditedTimeAndExitEditMode()
                        focusManager.clearFocus()
                    }) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Save Time",
                            tint = colorResource(R.color.eco_green)
                        )
                    }
                }
            } else {
                Text( // Anzeige-Modus
                    text = "%02d:%02d:%02d".format(
                        TimerManager.displayHours,
                        TimerManager.displayMinutes,
                        TimerManager.displaySeconds
                    ),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.eco_text),
                    modifier = Modifier.clickable {
                        if (!TimerManager.isTimerRunning) {
                            TimerManager.enterEditMode()
                        } else {
                            Toast.makeText(context, "Stop the timer to edit.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (!TimerManager.isEditingTime) {
                TimerActionButton(
                    text = if (TimerManager.isTimerRunning) "Stop" else "Start",
                    onClick = {
                        scope.launch {
                            TimerManager.startOrStopTimer() // Der Aufruf
                        }
                    },
                    enabled = TimerManager.canStartOrStop(),
                    // Hier die Farben für den jeweiligen Zustand (Start vs. Stop) anpassen
                    // Die enabledBackgroundColor und enabledContentColor werden von TimerActionButton
                    // für den AKTIVIERTEN Zustand des KNOPFES verwendet.
                    // Wenn der Knopf disabled ist, greift die Logik in TimerActionButton.
                    enabledBackgroundColor = if (TimerManager.isTimerRunning) {
                        colorResource(R.color.eco_accent_dark) // Farbe für "Stop"-Button
                    } else {
                        colorResource(R.color.eco_green)       // Farbe für "Start"-Button
                    },
                    enabledContentColor = colorResource(R.color.white), // Bleibt für beide weiß
                    buttonSize = 100.dp
                )
            }
            Spacer(modifier = Modifier.height(24.dp)) // Dieser Spacer kann hier bleiben oder entfernt werden
        }
        // --- ENDE TIMER-BEREICH ---
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

@Composable
fun TimerLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = colorResource(R.color.eco_accent_dark),
        modifier = Modifier.width(60.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
fun TimerInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 10.sp, color = colorResource(R.color.eco_text)) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
        singleLine = true,
        modifier = modifier
            .width(75.dp)
            .height(75.dp),
        textStyle = MaterialTheme.typography.headlineLarge.copy(
            textAlign = TextAlign.Center,
            color = colorResource(R.color.eco_text)
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colorResource(R.color.eco_surface),
            unfocusedContainerColor = colorResource(R.color.eco_surface),
            disabledContainerColor = colorResource(R.color.eco_surface),
        ),
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
fun TimerActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 64.dp,
    // Diese Farben werden für den ENABLED-Zustand verwendet.
    // Material 3 leitet daraus die Farben für den DISABLED-Zustand ab.
    enabledBackgroundColor: Color = colorResource(R.color.eco_green),
    enabledContentColor: Color = colorResource(R.color.white),
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(buttonSize),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = enabledBackgroundColor, // Farbe für den Hintergrund, wenn enabled
            contentColor = enabledContentColor,     // Farbe für den Text/Icon, wenn enabled
            // Für disabledContainerColor und disabledContentColor:
            // Keine explizite Zuweisung hier!
            // Material 3 wird basierend auf enabledBackgroundColor und enabledContentColor
            // sowie dem aktuellen Theme (onSurface etc.) passende Disabled-Farben generieren.
            // Diese haben typischerweise einen geringeren Alpha-Wert, um Deaktivierung anzuzeigen,
            // sind aber so gestaltet, dass sie sichtbar bleiben.
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text.uppercase(),
            fontSize = if (buttonSize > 80.dp) 18.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = colorResource(R.color.eco_text)
            // Die Textfarbe wird automatisch durch `contentColor` bzw. `disabledContentColor` gesteuert.
        )
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