package com.thoughtworks.bleconn.app.ui.views.bleserver

import com.thoughtworks.bleconn.app.foundation.mvi.model.Action
import com.thoughtworks.bleconn.app.foundation.mvi.model.Event
import com.thoughtworks.bleconn.app.foundation.mvi.model.State


data class BleServerState(
    val isStarted: Boolean = false,
) : State

sealed interface BleServerEvent : Event {
    data class ShowToast(val text: String, val isLong: Boolean = false) : BleServerEvent
}

sealed interface BleServerAction : Action {
    data object NavigateBack: BleServerAction
    data object Start : BleServerAction
    data object Stop : BleServerAction
}