package com.thoughtworks.bleconn.app.ui.views.bleperf

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.thoughtworks.bleconn.app.definitions.BleUUID
import com.thoughtworks.bleconn.app.definitions.Manufacturer
import com.thoughtworks.bleconn.app.di.Dependency
import com.thoughtworks.bleconn.app.foundation.mvi.DefaultStore
import com.thoughtworks.bleconn.app.foundation.mvi.MVIViewModel
import com.thoughtworks.bleconn.app.foundation.mvi.Store
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.LinkedList

class BlePerfViewModel(
    dependency: Dependency,
    store: Store<BlePerfState, BlePerfEvent, BlePerfAction> = DefaultStore(
        initialState = BlePerfState()
    ),
) : MVIViewModel<BlePerfState, BlePerfEvent, BlePerfAction>(store) {
    private val navigator = dependency.navigator
    private val bleClient = dependency.bleClient
    private val bleScanner = dependency.bleScanner
    private var mtu = 20

    init {
        startScan()
    }

    private fun startScan() {
        runAsync {
            logMessage("Starting scan...")
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
                    logMessage("Device found: ${result.device.address}")
                    sendAction(BlePerfAction.OnFoundDevice(result.device.address))
                    sendAction(BlePerfAction.IncreaseScanSuccessCount) // New action
                },
                onError = { errorCode ->
                    bleScanner.stop()
                    logMessage("Scan failed with error code: $errorCode", true)
                    sendAction(BlePerfAction.IncreaseScanFailCount) // New action
                    disconnectAndRestart()
                }
            )

            delay(SCAN_TIMEOUT)
            if (bleScanner.isStarted()) {
                logMessage("Scan timeout, no device found", true)
                bleScanner.stop()
                sendAction(BlePerfAction.IncreaseScanFailCount) // New action
                disconnectAndRestart()
            }
        }
    }

    override fun reduce(currentState: BlePerfState, action: BlePerfAction): BlePerfState {
        return when (action) {
            is BlePerfAction.LogMessage -> {
                val newLog = LinkedList(currentState.log)
                newLog.add(action.message)
                if (newLog.size > MAX_LOG_SIZE) {
                    newLog.poll()
                }
                currentState.copy(log = newLog)
            }

            is BlePerfAction.IncreaseConnectSuccessCount -> {
                currentState.copy(connectSuccessCount = currentState.connectSuccessCount + 1)
            }

            is BlePerfAction.IncreaseConnectFailCount -> {
                currentState.copy(connectFailCount = currentState.connectFailCount + 1)
            }

            is BlePerfAction.IncreaseDiscoverSuccessCount -> {
                currentState.copy(discoverSuccessCount = currentState.discoverSuccessCount + 1)
            }

            is BlePerfAction.IncreaseDiscoverFailCount -> {
                currentState.copy(discoverFailCount = currentState.discoverFailCount + 1)
            }

            is BlePerfAction.IncreaseRequestMtuSuccessCount -> {
                currentState.copy(requestMtuSuccessCount = currentState.requestMtuSuccessCount + 1)
            }

            is BlePerfAction.IncreaseRequestMtuFailCount -> {
                currentState.copy(requestMtuFailCount = currentState.requestMtuFailCount + 1)
            }

            is BlePerfAction.IncreaseReadSuccessCount -> {
                currentState.copy(readSuccessCount = currentState.readSuccessCount + 1)
            }

            is BlePerfAction.IncreaseReadFailCount -> {
                currentState.copy(readFailCount = currentState.readFailCount + 1)
            }

            is BlePerfAction.IncreaseWriteSuccessCount -> {
                currentState.copy(writeSuccessCount = currentState.writeSuccessCount + 1)
            }

            is BlePerfAction.IncreaseWriteFailCount -> {
                currentState.copy(writeFailCount = currentState.writeFailCount + 1)
            }

            is BlePerfAction.IncreaseScanSuccessCount -> {
                currentState.copy(scanSuccessCount = currentState.scanSuccessCount + 1)
            }

            is BlePerfAction.IncreaseScanFailCount -> {
                currentState.copy(scanFailCount = currentState.scanFailCount + 1)
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
                startTest(action.address)
            }

            is BlePerfAction.RestartProcess -> {
                startScan()
            }

            else -> {}
        }
    }

    private fun startTest(address: String) {
        runAsync {
            if (!connectToDevice(address)) return@runAsync
            if (!discoverServices()) return@runAsync
            if (!requestMtu()) return@runAsync
            performReadWriteTest()
            disconnectAndRestart()
        }
    }

    private suspend fun connectToDevice(address: String): Boolean {
        logMessage("Connecting to device: $address")
        val connResult = bleClient.connect(address) {}
        return if (!connResult.isSuccess) {
            sendAction(BlePerfAction.IncreaseConnectFailCount)
            disconnectAndRestart()
            false
        } else {
            sendAction(BlePerfAction.IncreaseConnectSuccessCount)
            logMessage("Connected to device: $address")
            true
        }
    }

    private suspend fun discoverServices(): Boolean {
        logMessage("Discovering services...")
        val discoverResult = bleClient.discoverServices()
        return if (!discoverResult.isSuccess) {
            sendAction(BlePerfAction.IncreaseDiscoverFailCount)
            logMessage("Failed to discover services: ${discoverResult.errorMessage}", true)
            disconnectAndRestart()
            false
        } else {
            sendAction(BlePerfAction.IncreaseDiscoverSuccessCount)
            logMessage("Services discovered successfully")
            true
        }
    }

    private suspend fun requestMtu(): Boolean {
        logMessage("Requesting MTU $PERF_TEST_MTU ...")
        val mtuResult = bleClient.requestMtu(PERF_TEST_MTU)
        return if (!mtuResult.isSuccess) {
            sendAction(BlePerfAction.IncreaseRequestMtuFailCount)
            logMessage("Failed to request MTU ${PERF_TEST_MTU}: ${mtuResult.errorMessage}", true)
            disconnectAndRestart()
            false
        } else {
            sendAction(BlePerfAction.IncreaseRequestMtuSuccessCount)
            logMessage("MTU updated to ${mtuResult.mtu}")
            mtu = mtuResult.mtu
            true
        }
    }

    private suspend fun performReadWriteTest() {
        logMessage("Starting read/write test...")
        repeat(READ_WRITE_TIMES) {
            val readResult = bleClient.readCharacteristic(
                BleUUID.SERVICE,
                BleUUID.CHARACTERISTIC_PERF_TEST_READ
            )
            if (readResult.isSuccess) {
                sendAction(BlePerfAction.IncreaseReadSuccessCount)
                logMessage("Read data: ${readResult.value.size} bytes")
            } else {
                sendAction(BlePerfAction.IncreaseReadFailCount)
                logMessage("Read failed: ${readResult.errorMessage}", true)
                disconnectAndRestart()
                return
            }

            delay(READ_WRITE_DELAY)

            val writeResult = bleClient.writeCharacteristic(
                BleUUID.SERVICE,
                BleUUID.CHARACTERISTIC_PERF_TEST_WRITE,
                ByteArray(mtu - 10) { 0 },
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
            if (writeResult.isSuccess) {
                sendAction(BlePerfAction.IncreaseWriteSuccessCount)
                logMessage("Write data ${mtu - 10} bytes")
            } else {
                sendAction(BlePerfAction.IncreaseWriteFailCount)
                logMessage("Write failed: ${writeResult.errorMessage}", true)
                disconnectAndRestart()
                return
            }

            delay(READ_WRITE_DELAY)
        }

        logMessage("Read/Write test completed successfully")
    }

    private fun disconnectAndRestart() {
        bleClient.disconnect()
        sendAction(BlePerfAction.RestartProcess)
    }

    private fun logMessage(message: String, isError: Boolean = false) {
        val timestamp = SimpleDateFormat("yyyy.MM.dd-HH:mm:ss", Locale.getDefault()).format(Date())
        val logMessage = if (isError) "[ERROR] $message" else message
        sendAction(BlePerfAction.LogMessage("[$timestamp] $logMessage"))
        Log.d(TAG, message)
    }

    override fun onCleared() {
        bleScanner.stop()
        bleClient.disconnect()
    }

    companion object {
        private const val TAG = "BlePerfViewModel"
        private const val PERF_TEST_MTU = 480
        private const val READ_WRITE_TIMES = 5
        private const val READ_WRITE_DELAY = 2000L
        private const val MAX_LOG_SIZE = 1000
        private const val SCAN_TIMEOUT = 30000L // 30 seconds
    }
}

class BlePerfViewModelFactory(
    private val dependency: Dependency,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BlePerfViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BlePerfViewModel(dependency) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
