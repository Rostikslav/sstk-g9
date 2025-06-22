package com.sstk_g9.ecoswitch.logic

import com.sstk_g9.ecoswitch.model.EcoSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

object PowerManager {
    suspend fun toggleDevice(device: EcoSwitch): EcoSwitch? {
        return withContext(Dispatchers.IO) {
            try {
                val newState = if (device.isOn) 0 else 1
                val request = Request.Builder()
                    .url("http://${device.ipAddress}/toggle?token=${device.token}&state=$newState")
                    .get()
                    .build()

                val response = DeviceCore.client.newCall(request).execute()
                if (response.isSuccessful) {
                    checkDeviceStatus(device)
                } else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

     suspend fun checkDeviceStatus(device: EcoSwitch): EcoSwitch? {
        return withContext(Dispatchers.IO) {
            try {
                // Get device status
                val statusRequest = Request.Builder()
                    .url("http://${device.ipAddress}/status?token=${device.token}")
                    .get()
                    .build()

                val statusResponse = DeviceCore.client.newCall(statusRequest).execute()

                // Get temperature
                val tempRequest = Request.Builder()
                    .url("http://${device.ipAddress}/temperature?token=${device.token}")
                    .get()
                    .build()

                val tempResponse = DeviceCore.client.newCall(tempRequest).execute()

                if (statusResponse.isSuccessful) {
                    val statusBody = statusResponse.body?.string() ?: ""
                    val temperature = if (tempResponse.isSuccessful) {
                        tempResponse.body?.string() ?: "N/A"
                    } else "N/A"

                    val json = JSONObject(statusBody)
                    val isOn = json.optBoolean("state", false)

                    device.copy(
                        isConnected = true,
                        isOn = isOn,
                        temperature = temperature
                    )
                } else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}