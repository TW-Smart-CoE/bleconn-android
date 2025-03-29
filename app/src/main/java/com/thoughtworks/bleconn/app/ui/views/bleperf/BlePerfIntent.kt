package com.thoughtworks.bleconn.app.ui.views.bleperf

import android.bluetooth.BluetoothGattService
import com.thoughtworks.bleconn.app.foundation.mvi.model.Action
import com.thoughtworks.bleconn.app.foundation.mvi.model.Event
import com.thoughtworks.bleconn.app.foundation.mvi.model.State

data class BlePerfState(
    val isScanning: Boolean = false,
    val isConnected: Boolean = false,
    val log: List<String> = emptyList(),
) : State

sealed interface BlePerfEvent : Event {
    data class ShowToast(val text: String, val isLong: Boolean = false) : BlePerfEvent
}

sealed interface BlePerfAction : Action {
    data object NavigateBack : BlePerfAction
    data object StartScan : BlePerfAction
    data class OnFoundDevice(val address: String) : BlePerfAction
    data class LogMessage(val message: String, val isError: Boolean = false) : BlePerfAction
    data object RestartProcess : BlePerfAction
}
