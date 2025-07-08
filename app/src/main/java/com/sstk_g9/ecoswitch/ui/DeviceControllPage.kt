package com.sstk_g9.ecoswitch.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
    onToggle: suspend (EcoSwitch) -> Unit,
    onDisconnect: () -> Unit,
    onStatusUpdate: (EcoSwitch?) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isStartMode by remember { mutableStateOf(true) }
    var isToggling by remember { mutableStateOf(false) }

    val hoursFocusRequester = remember { FocusRequester() }
    val minutesFocusRequester = remember { FocusRequester() }
    val secondsFocusRequester = remember { FocusRequester() }

    // Status update (temperature, toggle and timer)
    LaunchedEffect(device.ipAddress) {
        while (device.isConnected && !isToggling) {
            val updated = PowerManager.checkDeviceStatus(device)
            onStatusUpdate(updated)
            delay(3_000)
        }
    }

    // Initial setup when page loads
    LaunchedEffect(device.ipAddress) {
        TimerManager.setDevice(device)
        val updatedDevice = PowerManager.checkDeviceStatus(device)
        updatedDevice.let { freshDevice ->
            onStatusUpdate(freshDevice)
            TimerManager.sync(freshDevice)
        }
        TimerManager.setOnTimerFinishedCallback { deviceWithTimerOff ->
            scope.launch {
                Toast.makeText(context, "Timer finished!", Toast.LENGTH_LONG).show()
                deviceWithTimerOff?.let { device ->
                    onStatusUpdate(device)
                }
            }
        }

        if (!device.isTimerActive)
            TimerManager.setInitialTime(0, 15, 0)
    }

    LaunchedEffect(TimerManager.isEditingTime) {
        if (TimerManager.isEditingTime) {
            delay(50)
            hoursFocusRequester.requestFocus()
        }
    }

    // cancel edit mode with android back button
    if (TimerManager.isEditingTime) {
        BackHandler {
            TimerManager.cancelEditMode()
            focusManager.clearFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            // stop edit timer mode if clicked outside timer input fields
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // no visuals
            ) {
                if (TimerManager.isEditingTime) {
                    scope.launch {
                        TimerManager.saveEditedTimeAndExitEditMode()
                        focusManager.clearFocus() // hide keyboard
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Device status section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
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
        // Timer
        Column(
            modifier = Modifier.weight(1f),
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
                    modifier = Modifier.clickable( // do nothing if clicked outside input fields to prevent outward propagation
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
                        modifier = Modifier.focusRequester(secondsFocusRequester),
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(onDone = {
                            scope.launch {
                                TimerManager.saveEditedTimeAndExitEditMode()
                                focusManager.clearFocus()
                            }
                        })
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Cancel button
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
                    // Save button
                    IconButton(onClick = {
                        scope.launch {
                            TimerManager.saveEditedTimeAndExitEditMode()
                            focusManager.clearFocus()
                        }
                    }) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Save Time",
                            tint = colorResource(R.color.eco_green)
                        )
                    }
                }
            } else {
                Text( // Countdown mode
                    text = "%02d:%02d:%02d".format(
                        TimerManager.displayHours,
                        TimerManager.displayMinutes,
                        TimerManager.displaySeconds
                    ),
                    fontSize = 60.sp,
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
                Row {
                    if (!TimerManager.isTimerRunning) {
                        TimerActionButton(
                            text = "Start",
                            onClick = {
                                scope.launch {
                                    TimerManager.startDeviceTimer(isStartMode)
                                }
                            },
                            backgroundColor = colorResource(R.color.eco_green),
                            buttonSize = 90.dp
                        )
                    } else {
                        TimerActionButton(
                            text = "Cancel",
                            onClick = {
                                scope.launch {
                                    TimerManager.cancelTimer()
                                }
                            },
                            backgroundColor = colorResource(R.color.eco_accent_dark),
                            buttonSize = 90.dp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color.Transparent,
                                    shape = CircleShape
                                )
                                .padding(0.dp)
                        ) {
                            Switch(
                                enabled = !TimerManager.isTimerRunning,
                                checked = isStartMode,
                                onCheckedChange = { isStartMode = it },
                                modifier = Modifier.rotate(270f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colorResource(R.color.white),
                                    uncheckedThumbColor = colorResource(R.color.white),
                                    checkedTrackColor = colorResource(R.color.eco_green),
                                    uncheckedTrackColor = colorResource(R.color.eco_accent),
                                    // these help neutralize extra effects
                                    checkedBorderColor = Color.Transparent,
                                    uncheckedBorderColor = Color.Transparent
                                )
                            )
                        }
                        Text(if (isStartMode) "On" else "Off")
                    }
                }
            }
            Spacer(modifier = Modifier.height(120.dp))
        }

        // Control buttons section
        Column {
            // Main toggle button
            Button(
                onClick = {
                    if (!isToggling) {
                        isToggling = true
                        scope.launch {
                            try {
                                onToggle(device)
                            } finally {
                                isToggling = false
                            }
                        }
                    }
                },
                enabled = !isToggling,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (device.isOn) {
                        colorResource(R.color.eco_accent_dark)  // Orange for OFF
                    } else {
                        colorResource(R.color.eco_green)  // Green for ON
                    },
                    contentColor = colorResource(R.color.white),
                    disabledContainerColor = if (device.isOn) {
                        colorResource(R.color.eco_accent_dark).copy(alpha = 0.7f)  // Slightly faded orange
                    } else {
                        colorResource(R.color.eco_green).copy(alpha = 0.7f)  // Slightly faded green
                    },
                    disabledContentColor = colorResource(R.color.white).copy(alpha = 0.8f)  // Slightly faded white
                )
            ) {
                Text(
                    text = if (isToggling) "PROCESSING..." else if (device.isOn) "TURN OFF" else "TURN ON",
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
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var isFocused by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = if (isFocused && value == "00") "" else value,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
        singleLine = true,
        modifier = modifier
            .width(75.dp)
            .height(75.dp)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            },
        textStyle = MaterialTheme.typography.headlineLarge.copy(
            textAlign = TextAlign.Center,
            color = colorResource(R.color.eco_text)
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colorResource(R.color.eco_surface),
            unfocusedContainerColor = colorResource(R.color.eco_surface),
            focusedIndicatorColor = colorResource(R.color.eco_accent_dark),
            unfocusedIndicatorColor = colorResource(R.color.eco_green),
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
    backgroundColor: Color = colorResource(R.color.eco_green),
    contentColor: Color = colorResource(R.color.white),
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .width(120.dp)
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text.uppercase(),
            fontSize = if (buttonSize > 80.dp) 18.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = contentColor
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