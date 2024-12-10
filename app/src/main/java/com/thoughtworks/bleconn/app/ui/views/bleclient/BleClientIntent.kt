package com.thoughtworks.bleconn.app.ui.views.bleclient

import android.bluetooth.BluetoothGattService
import com.thoughtworks.bleconn.app.foundation.mvi.model.Action
import com.thoughtworks.bleconn.app.foundation.mvi.model.Event
import com.thoughtworks.bleconn.app.foundation.mvi.model.State


data class BleClientState(
    val isConnected: Boolean = false,
    val mtu: String = "default",
    val notification: String = "",
    val services: List<BluetoothGattService> = emptyList(),
) : State

sealed interface BleClientEvent : Event {
    data class ShowToast(val text: String, val isLong: Boolean = false) : BleClientEvent
}

sealed interface BleClientAction : Action {
    data object NavigateBack : BleClientAction
    data object ReadDeviceInfo : BleClientAction
    data object EnableNotification : BleClientAction
    data object DisableNotification : BleClientAction
    data class ConnectStatusChanged(val isConnected: Boolean) : BleClientAction
    data class OnServicesDiscovered(val services: List<BluetoothGattService>): BleClientAction
    data class RequestMtu(val mtu: Int) : BleClientAction
    data class WriteWiFiConfig(val ssid: String, val password: String) : BleClientAction
    data class OnMtuUpdated(val mtu: Int) : BleClientAction
    data class OnNotification(val notification: String) : BleClientAction
}