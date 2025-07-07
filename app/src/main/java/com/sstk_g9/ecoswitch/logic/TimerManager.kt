package com.sstk_g9.ecoswitch.logic

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sstk_g9.ecoswitch.model.EcoSwitch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

object TimerManager {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var displayHours by mutableIntStateOf(0)
        private set
    var displayMinutes by mutableIntStateOf(0)
        private set
    var displaySeconds by mutableIntStateOf(0)
        private set

    // Input fields
    var editHoursInput by mutableStateOf("00")
    var editMinutesInput by mutableStateOf("00")
    var editSecondsInput by mutableStateOf("00")

    var isTimerRunning by mutableStateOf(false)
        private set

    private var remainingTimeInMillis by mutableLongStateOf(0L)

    var isEditingTime by mutableStateOf(false)
        private set

    private var timerJob: Job? = null
    private var onTimerFinishedCallback: ((EcoSwitch?) -> Unit)? = null
    private var currentDevice: EcoSwitch? = null

    private fun timeToMillis(h: Int, m: Int, s: Int): Long {
        return (h * 3600L + m * 60L + s) * 1000L
    }

    private fun millisToHMS(millis: Long, updateDisplay: Boolean = true) {
        val totalSeconds = millis / 1000
        val h = (totalSeconds / 3600).toInt()
        val m = ((totalSeconds % 3600) / 60).toInt()
        val s = (totalSeconds % 60).toInt()
        if (updateDisplay) {
            displayHours = h
            displayMinutes = m
            displaySeconds = s
        }
    }

    fun setDevice(device: EcoSwitch) {
        currentDevice = device
    }

    suspend fun sync(device: EcoSwitch) {
        if (device.isTimerActive && device.remainingSeconds > 0) {
            remainingTimeInMillis = device.remainingSeconds * 1000L
            millisToHMS(remainingTimeInMillis)

            if (!isTimerRunning) {
                startLocalTimer()
            }
        } else {
            if (isTimerRunning) {
                cancelTimer()
            }
        }
    }

    private suspend fun sendTimerCommand(seconds: Int, action: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val device = currentDevice ?: return@withContext false

                val url =
                    "http://${device.ipAddress}/timer?token=${device.token}&seconds=$seconds&action=$action"
                val request = Request.Builder().url(url).get().build()
                val response = DeviceCore.client.newCall(request).execute()

                val success = response.isSuccessful
                response.close()

                Log.d("TimerManager", "Timer command $action sent, success: $success")
                success
            } catch (e: Exception) {
                Log.e("TimerManager", "Failed to send timer command", e)
                false
            }
        }
    }

    suspend fun startDeviceTimer(isStartMode: Boolean) {
        if (isEditingTime) return

        // Use the display values if remainingTimeInMillis is 0 (e.g. after initial input)
        if (remainingTimeInMillis == 0L && (displayHours > 0 || displayMinutes > 0 || displaySeconds > 0)) {
            remainingTimeInMillis = timeToMillis(displayHours, displayMinutes, displaySeconds)
        } else if (remainingTimeInMillis == 0L) {
            return
        }

        if (remainingTimeInMillis > 0 && !isTimerRunning) {
            val totalSeconds = (remainingTimeInMillis / 1000).toInt()
            val success: Boolean = if(isStartMode) {
                sendTimerCommand(totalSeconds, 1)
            } else {
                sendTimerCommand(totalSeconds, 0)
            }
            if (success) {
                currentDevice?.let { device ->
                    device.isTimerActive = true
                    device.remainingSeconds = totalSeconds
                }
            }
            startLocalTimer()
        } else {
            Log.e("TimerManager", "Failed to start timer on device")
        }
    }

    private fun startLocalTimer() {
        isTimerRunning = true
        timerJob?.cancel()
        timerJob = scope.launch {
            var currentTime = remainingTimeInMillis
            while (currentTime > 0 && isTimerRunning) {
                millisToHMS(currentTime)
                delay(1000)
                currentTime -= 1000
                if (isTimerRunning) {
                    remainingTimeInMillis = currentTime
                }
            }
            if (currentTime <= 0 && isTimerRunning) {
                handleTimerFinished()
            }
        }
    }

     suspend fun cancelTimer() {
        if (isTimerRunning) {
            val success = sendTimerCommand(0, 0)
            if (success) {
                currentDevice?.let { device ->
                    device.isTimerActive = false
                    device.remainingSeconds = 0
                }
            }
            // cancel local timer regardless of device response
            isTimerRunning = false
            timerJob?.cancel()

            Log.d("TimerManager", "Timer canceled, device command success: $success")
        } else {
            Log.e("TimerManager", "Unable to cancel device timer")
        }
    }

    private suspend fun handleTimerFinished() {
        // Update device state
        currentDevice?.let { device ->
            var updatedDevice = PowerManager.checkDeviceStatus(device)
            // in case ui timer finishes faster than hardware timer
            if (updatedDevice!!.isTimerActive) {
                timerJob?.cancel()
                timerJob = scope.launch {
                    var currentTime = updatedDevice!!.remainingSeconds
                    while (currentTime > 0) {
                        remainingTimeInMillis = currentTime * 1000L
                        millisToHMS(remainingTimeInMillis)
                        delay(1000)
                        currentTime -= 1
                    }
                    updatedDevice = PowerManager.checkDeviceStatus(device)
                }
            }
            device.isOn = updatedDevice!!.isOn
            device.isTimerActive = updatedDevice!!.isTimerActive
            device.remainingSeconds = updatedDevice!!.remainingSeconds
        }

        millisToHMS(0)
        isTimerRunning = false
        remainingTimeInMillis = 0L
        onTimerFinishedCallback?.invoke(currentDevice)
    }

    fun enterEditMode() {
        if (isTimerRunning) return

        editHoursInput = "%02d".format(displayHours)
        editMinutesInput = "%02d".format(displayMinutes)
        editSecondsInput = "%02d".format(displaySeconds)
        isEditingTime = true
    }

    suspend fun saveEditedTimeAndExitEditMode() {
        if (!isEditingTime) return

        val h = editHoursInput.toIntOrNull() ?: displayHours
        val m = editMinutesInput.toIntOrNull() ?: displayMinutes
        val s = editSecondsInput.toIntOrNull() ?: displaySeconds

        if (isTimerRunning) cancelTimer()

        displayHours = h.coerceIn(0, 99)
        displayMinutes = m.coerceIn(0, 59)
        displaySeconds = s.coerceIn(0, 59)
        remainingTimeInMillis = timeToMillis(displayHours, displayMinutes, displaySeconds)
        cancelEditMode()
    }

    fun cancelEditMode() {
        isEditingTime = false
    }

    fun setInitialTime(hours: Int, minutes: Int, seconds: Int) {
        if (!isTimerRunning && !isEditingTime) {
            displayHours = hours.coerceIn(0, 99)
            displayMinutes = minutes.coerceIn(0, 59)
            displaySeconds = seconds.coerceIn(0, 59)
            remainingTimeInMillis = timeToMillis(displayHours, displayMinutes, displaySeconds)
            millisToHMS(remainingTimeInMillis) // ensures that the consistent display
        }
    }

    fun setOnTimerFinishedCallback(callback: (EcoSwitch?) -> Unit) {
        onTimerFinishedCallback = callback
    }
}