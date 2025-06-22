package com.sstk_g9.ecoswitch.logic

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import com.sstk_g9.ecoswitch.model.EcoSwitch

object ConnectionManager {

    fun loadSavedDevice(prefs: SharedPreferences): EcoSwitch? {
        val ip = prefs.getString("device_ip", "") ?: ""
        val token = prefs.getString("device_token", "") ?: ""

        return if (ip.isNotEmpty() && token.isNotEmpty()) {
            EcoSwitch(ip, token)
        } else null
    }

    suspend fun findAndConnectDevice(onResult: (Boolean, EcoSwitch?) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                // Close any existing socket first
                DeviceCore.udpSocket?.close()

                DeviceCore.udpSocket = DatagramSocket(4210)
                DeviceCore.udpSocket?.soTimeout = 1000

                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)

                DeviceCore.discoveryJob = launch {
                    val startTime = System.currentTimeMillis()
                    val timeout = 15000 // 15 seconds timeout

                    while (isActive && (System.currentTimeMillis() - startTime) < timeout) {
                        try {
                            DeviceCore.udpSocket?.receive(packet)
                            val message = String(packet.data, 0, packet.length)
                            if (message.startsWith("ecoswitch:")) {
                                val ipAddress = message.substring(10)

                                // Found device, now try to connect
                                val newDevice = EcoSwitch(ipAddress = ipAddress)
                                val connectedDevice = connectToDevice(newDevice)

                                if (connectedDevice != null) {
                                    // Success -> stop discovery and return result
                                    withContext(Dispatchers.Main) {
                                        onResult(true, connectedDevice)
                                    }
                                    stopDiscovery()
                                    return@launch
                                }
                            }
                        } catch (e: SocketTimeoutException) {
                            // Continue listening
                        } catch (e: Exception) {
                            break
                        }
                    }
                    // Timeout reached, no device found or connection failed
                    withContext(Dispatchers.Main) {
                        onResult(false, null)
                    }
                    stopDiscovery()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false, null)
                }
            }
        }
    }

    fun stopDiscovery() {
        DeviceCore.discoveryJob?.cancel()
        DeviceCore.udpSocket?.close()
        DeviceCore.udpSocket = null
    }

    private suspend fun connectToDevice(device: EcoSwitch): EcoSwitch? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("http://${device.ipAddress}/setup")
                    .get()
                    .build()

                val response = DeviceCore.client.newCall(request).execute()
                if (response.isSuccessful) {
                    val token = response.body?.string() ?: ""
                    if (token.isNotEmpty()) {
                        device.copy(token = token, isConnected = true)
                    } else null
                } else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun saveDevice(prefs: SharedPreferences, device: EcoSwitch) {
        prefs.edit {
            putString("device_ip", device.ipAddress)
            putString("device_token", device.token)
        }
    }

    private fun clearSavedDevice(prefs: SharedPreferences) {
        prefs.edit {
            remove("device_ip")
            remove("device_token")
        }
    }

    suspend fun disconnect(
        device: EcoSwitch,
        prefs: SharedPreferences
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("http://${device.ipAddress}/disconnect?token=${device.token}")
                .get()
                .build()

            val response = DeviceCore.client.newCall(request).execute()
            if (response.isSuccessful) {
                clearSavedDevice(prefs)
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
