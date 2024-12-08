package com.thoughtworks.bleconn.app.ui.views.bleclient

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thoughtworks.bleconn.app.di.Dependency
import com.thoughtworks.bleconn.app.foundation.mvi.DefaultStore
import com.thoughtworks.bleconn.app.foundation.mvi.MVIViewModel
import com.thoughtworks.bleconn.app.foundation.mvi.Store
import kotlinx.coroutines.launch

class BleClientViewModel(
    dependency: Dependency,
    private val address: String,
    store: Store<BleClientState, BleClientEvent, BleClientAction> =
        DefaultStore(
            initialState = BleClientState(
                isConnected = false,
            )
        ),
) : MVIViewModel<BleClientState, BleClientEvent, BleClientAction>(store) {
    private val ioDispatcher = dependency.coroutineDispatchers.ioDispatcher
    private val navigator = dependency.navigator
    private val bleClient = dependency.bleClient

    init {
        viewModelScope.launch(ioDispatcher) {
            if (address.isNotEmpty()) {
                val result = bleClient.connect(address)
                sendAction(BleClientAction.ConnectStatusChanged(result.connected))
            } else {
                Log.w(TAG, "Address is empty")
            }
        }
    }

    override fun reduce(
        currentState: BleClientState,
        action: BleClientAction,
    ): BleClientState {
        return when (action) {
            is BleClientAction.NavigateBack -> {
                currentState
            }

            is BleClientAction.ConnectStatusChanged -> {
                currentState.copy(
                    isConnected = action.isConnected,
                )
            }

            else -> {
                currentState
            }
        }
    }

    override fun runSideEffect(
        action: BleClientAction,
        currentState: BleClientState,
    ) {
        when (action) {
            is BleClientAction.NavigateBack -> {
                navigator.navigateBack()
            }

            is BleClientAction.WriteWiFiConfig -> {
            }

            else -> {
            }
        }
    }

    override fun onCleared() {
        bleClient.disconnect()
    }

    companion object {
        private const val TAG = "BleClientViewModel"
    }
}

class BleClientViewModelFactory(
    private val dependency: Dependency,
    private val address: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BleClientViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BleClientViewModel(dependency, address) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}