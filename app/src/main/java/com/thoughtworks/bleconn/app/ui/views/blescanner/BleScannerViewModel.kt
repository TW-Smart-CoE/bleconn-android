package com.thoughtworks.bleconn.app.ui.views.blescanner

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
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
import kotlinx.coroutines.launch


class BleScannerViewModel(
    dependency: Dependency,
    store: Store<BleScannerState, BleScannerEvent, BleScannerAction> =
        DefaultStore(
            initialState = BleScannerState(
                isScanStarted = false,
                scanResults = emptyList(),
            )
        ),
) : MVIViewModel<BleScannerState, BleScannerEvent, BleScannerAction>(store) {
    private val ioDispatcher = dependency.coroutineDispatchers.ioDispatcher
    private val navigator = dependency.navigator
    private val bleScanner = dependency.bleScanner

    override fun reduce(
        currentState: BleScannerState,
        action: BleScannerAction,
    ): BleScannerState {
        return when (action) {
            is BleScannerAction.StartScan -> {
                currentState.copy(
                    isScanStarted = true,
                )
            }

            is BleScannerAction.StopScan -> {
                currentState.copy(
                    isScanStarted = false,
                    scanResults = emptyList(),
                )
            }

            is BleScannerAction.OnFoundDevice -> {
                currentState.copy(
                    scanResults = currentState.scanResults.toMutableList().apply {
                        buildNewScanResults(action)
                    }
                )
            }

            else -> {
                currentState
            }
        }
    }

    private fun MutableList<ScanResult>.buildNewScanResults(action: BleScannerAction.OnFoundDevice) {
        val newManufacturerData =
            action.scanResult.scanRecord?.getManufacturerSpecificData(
                Manufacturer.ID
            )

        val index = indexOfFirst { scanResult ->
            val existingManufacturerData =
                scanResult.scanRecord?.getManufacturerSpecificData(
                    Manufacturer.ID
                )
            existingManufacturerData != null && existingManufacturerData.contentEquals(
                newManufacturerData
            )
        }

        if (index >= 0) {
            set(index, action.scanResult)
        } else {
            add(action.scanResult)
        }
    }

    override fun runSideEffect(
        action: BleScannerAction,
        currentState: BleScannerState,
    ) {
        when (action) {
            is BleScannerAction.NavigateBack -> {
                navigator.navigateBack()
            }

            is BleScannerAction.StartScan -> {
                bleScannerStartAsync()
            }

            is BleScannerAction.StopScan -> {
                bleScannerStop()
            }

            is BleScannerAction.ConnectToDevice -> {
                bleScannerStop()
                navigator.navigateToBleClientScreen(action.address)
            }

            else -> {}
        }
    }

    private fun bleScannerStartAsync() {
        viewModelScope.launch(ioDispatcher) {
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
                filters,
                settings,
                onFound = { result ->
                    Log.d(TAG, "Found device: ${result.device.address}")
                    sendAction(BleScannerAction.OnFoundDevice(result))
                },
            ) { errorCode ->
                Log.e(TAG, "Scan failed with error code: $errorCode")
                sendEvent(BleScannerEvent.ShowToast("Scan failed with error code: $errorCode"))

                sendAction(BleScannerAction.StopScan)
            }
        }
    }

    private fun bleScannerStop() {
        bleScanner.stop()
    }

    override fun onCleared() {
        bleScannerStop()
    }

    companion object {
        private const val TAG = "BleScannerViewModel"
    }
}

class BleScannerViewModelFactory(
    private val dependency: Dependency,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BleScannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BleScannerViewModel(dependency) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
