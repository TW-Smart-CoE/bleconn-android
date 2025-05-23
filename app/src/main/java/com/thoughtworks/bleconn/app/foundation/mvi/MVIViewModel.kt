package com.thoughtworks.bleconn.app.foundation.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thoughtworks.bleconn.app.foundation.mvi.model.Action
import com.thoughtworks.bleconn.app.foundation.mvi.model.Event
import com.thoughtworks.bleconn.app.foundation.mvi.model.State
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

abstract class MVIViewModel<S : State, E : Event, A : Action>(
    private val store: Store<S, E, A>,
) : ViewModel(),
    Reducer<S, E, A>,
    Store<S, E, A> by store {
    init {
        store.scope = viewModelScope
        dispatch()
    }

    private fun dispatch() {
        store.action.onEach {
            val newState = reduce(uiState.value, it)

            sendState(newState)

            runSideEffect(it, newState)
        }.launchIn(viewModelScope)
    }

    fun runAsync(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        block: suspend () -> Unit,
    ) = viewModelScope.launch(dispatcher) {
        block()
    }
}