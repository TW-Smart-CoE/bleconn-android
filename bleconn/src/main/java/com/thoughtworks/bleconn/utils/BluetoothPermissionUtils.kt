package com.thoughtworks.bleconn.utils

import android.os.Build

object BluetoothPermissionUtils {
    fun runtimePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        }
    }
}