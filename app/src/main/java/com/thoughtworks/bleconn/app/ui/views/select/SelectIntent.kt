package com.thoughtworks.bleconn.app.ui.views.select

import com.thoughtworks.bleconn.app.foundation.mvi.model.Action
import com.thoughtworks.bleconn.app.foundation.mvi.model.Event
import com.thoughtworks.bleconn.app.foundation.mvi.model.State

data class SelectState(
    val placeholder: String = "",
) : State

sealed interface SelectEvent : Event

sealed interface SelectAction : Action {
    data object ClickBleServer : SelectAction
    data object ClickBleScanner : SelectAction
    data object ClickBlePerf : SelectAction
}