package com.sstk_g9.ecoswitch.logic

import android.util.Log
import com.sstk_g9.ecoswitch.model.EcoSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

object PowerManager {

    suspend fun toggleDevice(device: EcoSwitch): EcoSwitch {
        return withContext(Dispatchers.IO) {
            // Retry logic
            for (attempt in 0 until 2) {
                try {
                    val newState = if (device.isOn) 0 else 1
                    val request = Request.Builder()
                        .url("http://${device.ipAddress}/toggle?token=${device.token}&state=$newState")
                        .get()
                        .build()

                    val response = DeviceCore.client.newCall(request).execute()
                    response.use { // Ensures response is always closed
                        if (response.isSuccessful) {
                            // Try to read response body, but handle if it's malformed
                            try {
                                response.body?.string()
                            } catch (e: java.io.EOFException) {
                                Log.w("PowerManager", "Incomplete response from device (attempt ${attempt + 1}), but toggle may have succeeded")
                            }

                            // Longer delay to let device settle after toggle
                            kotlinx.coroutines.delay(500)
                            return@withContext checkDeviceStatus(device)
                        } else {
                            Log.e("PowerManager", "Toggle failed: ${response.code} ${response.message}")
                        }
                    }
                } catch (e: java.io.EOFException) {
                    Log.w("PowerManager", "EOF error on toggle attempt ${attempt + 1}: ${e.message}")
                    if (attempt < 1) {
                        // Wait before retrying
                        kotlinx.coroutines.delay(300)
                        continue
                    }
                } catch (e: Exception) {
                    Log.e("PowerManager", "Toggle device error (attempt ${attempt + 1})", e)
                    if (attempt < 1) {
                        kotlinx.coroutines.delay(300)
                        continue
                    }
                }
            }

            // If all retries failed, still try to check device status (in case of malformed response)
            Log.w("PowerManager", "All toggle attempts failed, checking device status anyway")
            //kotlinx.coroutines.delay(500)
            checkDeviceStatus(device)
        }
    }

    /**
     * Check device status including timer, toggle status, temperature
     */
    suspend fun checkDeviceStatus(device: EcoSwitch): EcoSwitch {
        return withContext(Dispatchers.IO) {
            for (attempt in 0 until 2) {
                try {
                    // Get device status
                    val statusRequest = Request.Builder()
                        .url("http://${device.ipAddress}/status?token=${device.token}")
                        .get()
                        .build()

                    val statusResponse = DeviceCore.client.newCall(statusRequest).execute()

                    statusResponse.use { response ->
                        if (response.isSuccessful) {
                            val responseBody = try {
                                response.body?.string()
                            } catch (e: java.io.EOFException) {
                                Log.w("PowerManager", "EOF while reading status response")
                                null
                            }

                            val temperature = checkTemperature(device)

                            responseBody?.let { body ->
                                try {
                                    val json = JSONObject(body)

                                    val isOn = json.optBoolean("state", false)

                                    val remainingSeconds = json.optInt("timerEnd", 0)
                                    val isTimerActive = remainingSeconds > 0

                                    return@withContext device.copy(
                                        isConnected = true,
                                        isOn = isOn,
                                        temperature = temperature,
                                        remainingSeconds = remainingSeconds,
                                        isTimerActive = isTimerActive,
                                    )
                                } catch (e: Exception) {
                                    Log.e("PowerManager", "Failed to parse device status JSON", e)
                                }
                            }
                        } else {
                            Log.e("PowerManager", "Status check failed: ${response.code} ${response.message}")
                        }
                    }
                } catch (e: java.io.EOFException) {
                    Log.w("PowerManager", "EOF error on status check attempt ${attempt + 1}: ${e.message}")
                    if (attempt < 1) {
                        kotlinx.coroutines.delay(200)
                        continue
                    }
                } catch (e: Exception) {
                    Log.e("PowerManager", "Check device status error (attempt ${attempt + 1})", e)
                    if (attempt < 1) {
                        kotlinx.coroutines.delay(200)
                        continue
                    }
                }
            }

            // If status check failed, return disconnected state
            Log.w("PowerManager", "All status check attempts failed")
            device.copy(isConnected = false)
        }
    }

    private suspend fun checkTemperature(device: EcoSwitch): String {
        return withContext(Dispatchers.IO) {
            try {
                val tempRequest = Request.Builder()
                    .url("http://${device.ipAddress}/temperature?token=${device.token}")
                    .get()
                    .build()

                val tempResponse = DeviceCore.client.newCall(tempRequest).execute()
                tempResponse.use { response ->
                    if (response.isSuccessful) {
                        try {
                            response.body?.string() ?: "0.0"
                        } catch (e: java.io.EOFException) {
                            Log.w("PowerManager", "EOF while reading temperature response")
                            "0.0"
                        }
                    } else {
                        Log.e("PowerManager", "Temperature check failed: ${response.code}")
                        "0.0"
                    }
                }
            } catch (e: java.io.EOFException) {
                Log.w("PowerManager", "EOF error on temperature check: ${e.message}")
                "0.0"
            } catch (e: Exception) {
                Log.e("PowerManager", "Check temperature error", e)
                "0.0"
            }
        }
    }
}
