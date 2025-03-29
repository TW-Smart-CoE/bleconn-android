package com.thoughtworks.bleconn.app.ui.views.bleperf

import android.bluetooth.BluetoothGattService
import com.thoughtworks.bleconn.app.foundation.mvi.model.Action
import com.thoughtworks.bleconn.app.foundation.mvi.model.Event
import com.thoughtworks.bleconn.app.foundation.mvi.model.State

data class BlePerfState(
    val isScanning: Boolean = false,
    val isConnected: Boolean = false,
    val log: List<String> = emptyList(),
    val mtu: Int = 0,
) : State

sealed interface BlePerfEvent : Event {
    data class ShowToast(val text: String, val isLong: Boolean = false) : BlePerfEvent
}

sealed interface BlePerfAction : Action {
    data object NavigateBack : BlePerfAction
    data object StartScan : BlePerfAction
    data class OnFoundDevice(val address: String) : BlePerfAction
    data class ConnectStatusChanged(val isConnected: Boolean) : BlePerfAction
    data class OnServicesDiscovered(val services: List<BluetoothGattService>) : BlePerfAction
    data class OnMtuUpdated(val mtu: Int) : BlePerfAction
    data class LogMessage(val message: String) : BlePerfAction
    data object StartReadWriteTest : BlePerfAction
}
