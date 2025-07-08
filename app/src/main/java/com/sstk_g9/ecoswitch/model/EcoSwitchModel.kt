package com.sstk_g9.ecoswitch.model

data class EcoSwitch(
    val ipAddress: String,
    var token: String = "",
    var isConnected: Boolean = false,
    var isOn: Boolean = false,
    var temperature: String = "0.0",
    var remainingSeconds: Int = 0,
    var isTimerActive: Boolean = false
)