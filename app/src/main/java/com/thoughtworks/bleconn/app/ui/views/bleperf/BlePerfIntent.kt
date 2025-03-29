package com.thoughtworks.bleconn.app.ui.views.bleperf

import com.thoughtworks.bleconn.app.foundation.mvi.model.Action
import com.thoughtworks.bleconn.app.foundation.mvi.model.Event
import com.thoughtworks.bleconn.app.foundation.mvi.model.State

data class BlePerfState(
    val isScanning: Boolean = false,
    val isConnected: Boolean = false,
    val log: List<String> = emptyList(),
    val connectSuccessCount: Int = 0,
    val connectFailCount: Int = 0,
    val discoverSuccessCount: Int = 0,
    val discoverFailCount: Int = 0,
    val requestMtuSuccessCount: Int = 0,
    val requestMtuFailCount: Int = 0,
    val readSuccessCount: Int = 0,
    val readFailCount: Int = 0,
    val writeSuccessCount: Int = 0,
    val writeFailCount: Int = 0,
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
    data object IncreaseConnectSuccessCount : BlePerfAction
    data object IncreaseConnectFailCount : BlePerfAction
    data object IncreaseDiscoverSuccessCount : BlePerfAction
    data object IncreaseDiscoverFailCount : BlePerfAction
    data object IncreaseRequestMtuSuccessCount : BlePerfAction
    data object IncreaseRequestMtuFailCount : BlePerfAction
    data object IncreaseReadSuccessCount : BlePerfAction
    data object IncreaseReadFailCount : BlePerfAction
    data object IncreaseWriteSuccessCount : BlePerfAction
    data object IncreaseWriteFailCount : BlePerfAction
}
