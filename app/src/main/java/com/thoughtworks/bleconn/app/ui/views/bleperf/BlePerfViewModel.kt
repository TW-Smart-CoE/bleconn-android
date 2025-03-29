package com.thoughtworks.bleconn.app.ui.views.bleperf

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thoughtworks.bleconn.app.definitions.BleUUID
import com.thoughtworks.bleconn.app.definitions.Manufacturer
import com.thoughtworks.bleconn.app.di.Dependency
import com.thoughtworks.bleconn.app.foundation.mvi.DefaultStore
import com.thoughtworks.bleconn.app.foundation.mvi.MVIViewModel
import com.thoughtworks.bleconn.app.foundation.mvi.Store
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BlePerfViewModel(
    dependency: Dependency,
    store: Store<BlePerfState, BlePerfEvent, BlePerfAction> = DefaultStore(
        initialState = BlePerfState()
    )
) : MVIViewModel<BlePerfState, BlePerfEvent, BlePerfAction>(store) {
    private val ioDispatcher = dependency.coroutineDispatchers.ioDispatcher
    private val navigator = dependency.navigator
    private val bleClient = dependency.bleClient
    private val bleScanner = dependency.bleScanner

    init {
        startScan()
    }

    private fun startScan() {
        runAsync {
            val filters = listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(BleUUID.SERVICE))
                    .setManufacturerData(Manufacturer.ID, Manufacturer.data, Manufacturer.dataMask)
                    .build()
            )
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build()

            bleScanner.start(
                filters = filters,
                settings = settings,
                onFound = { result ->
                    bleScanner.stop()
                    sendAction(BlePerfAction.OnFoundDevice(result.device.address))
                },
                onError = { errorCode ->
                    sendEvent(BlePerfEvent.ShowToast("Scan failed with error code: $errorCode"))
                }
            )
        }
    }

    override fun reduce(currentState: BlePerfState, action: BlePerfAction): BlePerfState {
        return when (action) {
            is BlePerfAction.ConnectStatusChanged -> {
                currentState.copy(isConnected = action.isConnected)
            }

            is BlePerfAction.OnMtuUpdated -> {
                currentState.copy(mtu = action.mtu)
            }

            is BlePerfAction.LogMessage -> {
                currentState.copy(log = currentState.log + action.message)
            }

            else -> currentState
        }
    }

    override fun runSideEffect(action: BlePerfAction, currentState: BlePerfState) {
        when (action) {
            is BlePerfAction.NavigateBack -> {
                navigator.navigateBack()
            }

            is BlePerfAction.OnFoundDevice -> {
                connectToDevice(action.address)
            }

            is BlePerfAction.ConnectStatusChanged -> {
                if (action.isConnected) discoverServices()
            }

            is BlePerfAction.OnServicesDiscovered -> {
                requestMtu()
            }

            is BlePerfAction.OnMtuUpdated -> {
                if (action.mtu >= 0) {
                    startReadWriteTest()
                }
            }

            is BlePerfAction.StartReadWriteTest -> {
                performReadWriteTest()
            }

            else -> {}
        }
    }

    private fun connectToDevice(address: String) {
        runAsync {
            bleClient.connect(address) { isConnected ->
                sendAction(BlePerfAction.ConnectStatusChanged(isConnected))
            }
        }
    }

    private fun discoverServices() {
        viewModelScope.launch(ioDispatcher) {
            val result = bleClient.discoverServices()
            if (result.isSuccess) {
                sendAction(BlePerfAction.OnServicesDiscovered(result.services))
            } else {
                logMessage("Failed to discover services: ${result.errorMessage}")
            }
        }
    }

    private fun requestMtu() {
        viewModelScope.launch(ioDispatcher) {
            val result = bleClient.requestMtu(PERF_TEST_MTU)
            if (result.isSuccess) {
                sendAction(BlePerfAction.OnMtuUpdated(result.mtu))
            } else {
                logMessage("Failed to request MTU: ${result.errorMessage}")
            }
        }
    }

    private fun startReadWriteTest() {
        sendAction(BlePerfAction.StartReadWriteTest)
    }

    private fun performReadWriteTest() {
        viewModelScope.launch(ioDispatcher) {
            while (true) {
                val readResult = bleClient.readCharacteristic(
                    BleUUID.SERVICE,
                    BleUUID.CHARACTERISTIC_PERF_TEST_READ
                )
                if (readResult.isSuccess) {
                    logMessage("Read data: ${readResult.value.size} bytes")
                } else {
                    logMessage("Read failed: ${readResult.errorMessage}")
                }

                delay(3000)

                val writeResult = bleClient.writeCharacteristic(
                    BleUUID.SERVICE,
                    BleUUID.CHARACTERISTIC_PERF_TEST_WRITE,
                    ByteArray(PERF_TEST_WRITE_DATA_SIZE) { 0 },
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
                if (writeResult.isSuccess) {
                    logMessage("Write data successful")
                } else {
                    logMessage("Write failed: ${writeResult.errorMessage}")
                }

                delay(3000)
            }
        }
    }

    private fun logMessage(message: String) {
        sendAction(BlePerfAction.LogMessage(message))
        Log.d(TAG, message)
    }

    companion object {
        private const val TAG = "BlePerfViewModel"
        private const val PERF_TEST_MTU = 480
        private const val PERF_TEST_WRITE_DATA_SIZE = 200
    }
}

class BlePerfViewModelFactory(
    private val dependency: Dependency
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BlePerfViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BlePerfViewModel(dependency) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
