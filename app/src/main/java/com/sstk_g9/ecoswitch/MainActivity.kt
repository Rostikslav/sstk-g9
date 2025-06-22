package com.sstk_g9.ecoswitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sstk_g9.ecoswitch.logic.ConnectionManager


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EcoSwitchApp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ConnectionManager.stopDiscovery()
    }
}
