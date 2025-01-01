package com.thoughtworks.bleconn.app.ui.views.bleclient

import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thoughtworks.bleconn.app.definitions.BleUUID
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
                mtu = "default",
                services = emptyList(),
            )
        ),
) : MVIViewModel<BleClientState, BleClientEvent, BleClientAction>(store) {
    private val ioDispatcher = dependency.coroutineDispatchers.ioDispatcher
    private val navigator = dependency.navigator
    private val bleClient = dependency.bleClient

    init {
        viewModelScope.launch(ioDispatcher) {
            if (address.isNotEmpty()) {
                bleClient.connect(address) { isConnected ->
                    sendAction(BleClientAction.ConnectStatusChanged(isConnected))
                }
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

            is BleClientAction.OnServicesDiscovered -> {
                currentState.copy(
                    services = action.services,
                )
            }

            is BleClientAction.OnMtuUpdated -> {
                currentState.copy(
                    mtu = action.mtu.toString(),
                )
            }

            is BleClientAction.OnNotification -> {
                currentState.copy(
                    notification = action.notification,
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

            is BleClientAction.ConnectStatusChanged -> {
                discoverServicesAsync(action.isConnected)
            }

            is BleClientAction.DiscoverServices -> {
                discoverServicesAsync(currentState.isConnected)
            }

            is BleClientAction.RequestMtu -> {
                requestMtuAsync(action.mtu)
            }

            is BleClientAction.ReadDeviceInfo -> {
                readDeviceInfoAsync()
            }

            is BleClientAction.WriteWiFiConfig -> {
                writeWiFiConfigAsync(action.ssid, action.password)
            }

            is BleClientAction.EnableNotification -> {
                enableNotificationAsync()
            }

            is BleClientAction.DisableNotification -> {
                disableNotificationAsync()
            }

            else -> {
            }
        }
    }

    private fun disableNotificationAsync() {
        viewModelScope.launch(ioDispatcher) {
            val result = bleClient.disableCharacteristicNotification(
                BleUUID.SERVICE,
                BleUUID.CHARACTERISTIC_DEVICE_STATUS,
            )

            val message = if (result.isSuccess) {
                "Disable notification successfully"
            } else {
                result.errorMessage
            }

            if (result.isSuccess) {
                Log.d(TAG, message)
            } else {
                Log.e(TAG, message)
            }
            sendEvent(BleClientEvent.ShowToast(message))
        }
    }

    private fun enableNotificationAsync() {
        viewModelScope.launch(ioDispatcher) {
            val result = bleClient.enableCharacteristicNotification(
                BleUUID.SERVICE,
                BleUUID.CHARACTERISTIC_DEVICE_STATUS,
                true,
            ) {
                Log.d(TAG, "Notification data arrived: ${String(it.value)}")
                sendAction(BleClientAction.OnNotification(String(it.value)))
            }

            val message = if (result.isSuccess) {
                "Enable notification successfully"
            } else {
                result.errorMessage
            }

            if (result.isSuccess) {
                Log.d(TAG, message)
            } else {
                Log.e(TAG, message)
            }
            sendEvent(BleClientEvent.ShowToast(message))
        }
    }

    private fun writeWiFiConfigAsync(ssid: String, password: String) {
        viewModelScope.launch(ioDispatcher) {
            val result = bleClient.writeCharacteristic(
                BleUUID.SERVICE,
                BleUUID.CHARACTERISTIC_WIFI,
                "$ssid;$password".toByteArray(),
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
            val message = if (result.isSuccess) {
                "Write WiFi config successfully"
            } else {
                result.errorMessage
            }

            if (result.isSuccess) {
                Log.d(TAG, message)
            } else {
                Log.e(TAG, message)
            }

            sendEvent(BleClientEvent.ShowToast(message))
        }
    }

    private fun readDeviceInfoAsync() {
        viewModelScope.launch(ioDispatcher) {
            val result =
                bleClient.readCharacteristic(BleUUID.SERVICE, BleUUID.CHARACTERISTIC_DEVICE_INFO)
            if (result.isSuccess) {
                val message = "Device info: ${String(result.value)}"
                Log.d(TAG, message)
                sendEvent(BleClientEvent.ShowToast(message))
            } else {
                Log.e(TAG, result.errorMessage)
                sendEvent(BleClientEvent.ShowToast("Failed to read device info"))
            }
        }
    }

    private fun requestMtuAsync(mtu: Int) {
        viewModelScope.launch(ioDispatcher) {
            val result = bleClient.requestMtu(mtu)
            if (result.isSuccess) {
                val message = "Requested MTU: ${result.mtu} successfully"
                Log.d(TAG, message)
                sendAction(BleClientAction.OnMtuUpdated(result.mtu))
                sendEvent(BleClientEvent.ShowToast(message))
            } else {
                Log.e(TAG, result.errorMessage)
                sendEvent(BleClientEvent.ShowToast(result.errorMessage))
            }
        }
    }

    private fun discoverServicesAsync(isConnected: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            if (isConnected) {
                val result = bleClient.discoverServices()
                if (!result.isSuccess) {
                    Log.e(TAG, result.errorMessage)
                }
                sendAction(BleClientAction.OnServicesDiscovered(result.services.filter {
                    it.uuid == BleUUID.SERVICE
                }))
            } else {
                sendAction(BleClientAction.OnServicesDiscovered(emptyList()))
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