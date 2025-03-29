package com.thoughtworks.bleconn.app.ui.views.select

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.thoughtworks.bleconn.app.di.Dependency
import com.thoughtworks.bleconn.app.foundation.mvi.DefaultStore
import com.thoughtworks.bleconn.app.foundation.mvi.MVIViewModel
import com.thoughtworks.bleconn.app.foundation.mvi.Store

class SelectViewModel(
    dependency: Dependency,
    store: Store<SelectState, SelectEvent, SelectAction> =
        DefaultStore(
            initialState = SelectState(
                placeholder = "Select",
            )
        ),
) : MVIViewModel<SelectState, SelectEvent, SelectAction>(store) {
    private val navigator = dependency.navigator

    override fun reduce(
        currentState: SelectState,
        action: SelectAction,
    ): SelectState {
        return when (action) {
            else -> {
                currentState
            }
        }
    }

    override fun runSideEffect(
        action: SelectAction,
        currentState: SelectState,
    ) {
        when (action) {
            SelectAction.ClickBleServer -> {
                navigator.navigateToBleServerScreen()
            }

            SelectAction.ClickBleScanner -> {
                navigator.navigateToBleScannerScreen()
            }

            SelectAction.ClickBlePerf -> {
                navigator.navigateToBlePerfScreen()
            }
        }
    }
}

class SelectViewModelFactory(
    private val dependency: Dependency,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SelectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SelectViewModel(dependency) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
