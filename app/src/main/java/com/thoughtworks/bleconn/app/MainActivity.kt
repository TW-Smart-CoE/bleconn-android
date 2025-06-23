package com.thoughtworks.bleconn.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.thoughtworks.bleconn.app.di.Dependency
import com.thoughtworks.bleconn.app.ui.navigation.Navigation
import com.thoughtworks.bleconn.app.ui.theme.BleconnandroidTheme

class MainActivity : ComponentActivity() {
    private lateinit var dependency: Dependency

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize()
        enableEdgeToEdge()
        setContent {
            BleconnandroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Navigation(dependency, innerPadding)
                }
            }
        }
    }

    private fun initialize() {
        dependency = (application as BleConnApp).getDependency()
    }
}