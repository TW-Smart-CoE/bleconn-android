package com.thoughtworks.bleconn.app.ui.views.blescanner

import android.bluetooth.le.ScanResult
import com.thoughtworks.bleconn.app.foundation.mvi.model.Action
import com.thoughtworks.bleconn.app.foundation.mvi.model.Event
import com.thoughtworks.bleconn.app.foundation.mvi.model.State


data class BleScannerState(
    val isScanStarted: Boolean = false,
    val scanResults: List<ScanResult> = emptyList()
) : State

sealed interface BleScannerEvent : Event {
    data class ShowToast(val text: String, val isLong: Boolean = false) : BleScannerEvent
}

sealed interface BleScannerAction : Action {
    data object NavigateBack: BleScannerAction
    data object StartScan : BleScannerAction
    data object StopScan : BleScannerAction
    data class OnFoundDevice(val scanResult: ScanResult) : BleScannerAction
    data class ConnectToDevice(val address: String) : BleScannerAction
}