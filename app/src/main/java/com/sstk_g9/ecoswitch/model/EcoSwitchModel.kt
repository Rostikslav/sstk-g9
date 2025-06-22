package com.sstk_g9.ecoswitch.model

data class EcoSwitch(
    val ipAddress: String,
    var token: String = "",
    var isConnected: Boolean = false,
    var isOn: Boolean = false,
    var temperature: String = "N/A"
)