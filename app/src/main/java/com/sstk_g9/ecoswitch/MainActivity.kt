package com.sstk_g9.ecoswitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import com.sstk_g9.ecoswitch.logic.ConnectionManager


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
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
