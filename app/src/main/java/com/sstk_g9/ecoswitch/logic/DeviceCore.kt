package com.sstk_g9.ecoswitch.logic

import kotlinx.coroutines.Job
import okhttp3.OkHttpClient
import java.net.DatagramSocket

object DeviceCore {
    internal val client = OkHttpClient()
    internal var udpSocket: DatagramSocket? = null
    internal var discoveryJob: Job? = null
}