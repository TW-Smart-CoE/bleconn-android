package com.thoughtworks.bleconn.app.ui.views.bleclient

import com.thoughtworks.bleconn.app.foundation.mvi.model.Action
import com.thoughtworks.bleconn.app.foundation.mvi.model.Event
import com.thoughtworks.bleconn.app.foundation.mvi.model.State


data class BleClientState(
    val isConnected: Boolean = false,
) : State

sealed interface BleClientEvent : Event {
    data class ShowToast(val text: String, val isLong: Boolean = false) : BleClientEvent
}

sealed interface BleClientAction : Action {
    data object NavigateBack : BleClientAction
    data class ConnectStatusChanged(val isConnected: Boolean) : BleClientAction
    data class WriteWiFiConfig(val ssid: String, val password: String) : BleClientAction
}