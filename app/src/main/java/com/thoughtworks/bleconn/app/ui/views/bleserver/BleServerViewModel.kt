package com.thoughtworks.bleconn.app.ui.views.bleserver

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY
import android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED
import android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE
import android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED
import android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR
import android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSetCallback.ADVERTISE_SUCCESS
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thoughtworks.bleconn.app.definitions.BleUUID
import com.thoughtworks.bleconn.app.definitions.Manufacturer
import com.thoughtworks.bleconn.app.di.Dependency
import com.thoughtworks.bleconn.app.foundation.mvi.DefaultStore
import com.thoughtworks.bleconn.app.foundation.mvi.MVIViewModel
import com.thoughtworks.bleconn.app.foundation.mvi.Store
import com.thoughtworks.bleconn.definitions.DescriptorUUID
import com.thoughtworks.bleconn.server.BleServer
import com.thoughtworks.bleconn.server.characteristic.CharacteristicHolder
import com.thoughtworks.bleconn.server.characteristic.CharacteristicHolder.ReadWriteResult
import com.thoughtworks.bleconn.server.descriptor.DescriptorHolder
import com.thoughtworks.bleconn.server.service.ServiceHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class BleServerViewModel(
    dependency: Dependency,
    store: Store<BleServerState, BleServerEvent, BleServerAction> =
        DefaultStore(
            initialState = BleServerState(
                isStarted = false,
            )
        ),
) : MVIViewModel<BleServerState, BleServerEvent, BleServerAction>(store) {
    private val ioDispatcher = dependency.coroutineDispatchers.ioDispatcher
    private val navigator = dependency.navigator
    private val bleServer = dependency.bleServer
    private val bleAdvertiser = dependency.bleAdvertiser
    private val bluetoothStateMonitor = dependency.bluetoothStateMonitor

    init {
        bluetoothStateMonitor.start {
            onBluetoothStateChanged(it)
        }

        bleServer.setBleServerListener(object : BleServer.BleServerListener {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onDeviceConnected(device: BluetoothDevice) {
                Log.d(TAG, "Device connected: ${device.address}, connected devices: ${bleServer.getConnectedDevices()}")
                if (bleServer.getConnectedDevices().size > 1) {
                    Log.d(TAG, "Disconnecting device: ${device.address}")
                    bleServer.cancelConnection(device)
                }
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onDeviceDisconnected(device: BluetoothDevice) {
                Log.d(TAG, "Device disconnected: ${device.address}, connected devices: ${bleServer.getConnectedDevices()}")
            }
        })
    }

    private fun onBluetoothStateChanged(state: Int) {
        if (state == BluetoothAdapter.STATE_ON) {
            if (bleServer.isStarted() || bleAdvertiser.isStarted()) {
                viewModelScope.launch(ioDispatcher) {
                    Log.d(TAG, "Restarting BLE server and advertiser")
                    delay(500)
                    sendAction(BleServerAction.Stop)
                    delay(500)
                    sendAction(BleServerAction.Start)
                }
            }
        }
    }

    override fun reduce(
        currentState: BleServerState,
        action: BleServerAction,
    ): BleServerState {
        return when (action) {
            BleServerAction.Start -> {
                currentState.copy(
                    isStarted = true,
                )
            }

            BleServerAction.Stop -> {
                currentState.copy(
                    isStarted = false,
                )
            }

            else -> {
                currentState
            }
        }
    }

    override fun runSideEffect(
        action: BleServerAction,
        currentState: BleServerState,
    ) {
        when (action) {
            BleServerAction.NavigateBack -> {
                navigator.navigateBack()
            }

            BleServerAction.Start -> {
                bleServerStartAsync()
            }

            BleServerAction.Stop -> {
                bleServerStop()
            }
        }
    }

    private fun bleServerStop() {
        bleAdvertiser.stop()
        bleServer.stop()
    }

    private fun bleServerStartAsync() {
        viewModelScope.launch(ioDispatcher) {
            if (startServer()) {
                startAdvertiser()
            } else {
                val errorMessage = "Failed to start server"
                Log.e(TAG, errorMessage)
                sendEvent(BleServerEvent.ShowToast(errorMessage))

                sendAction(BleServerAction.Stop)
            }
        }
    }

    private fun startServer(): Boolean {
        return bleServer.start(buildServices())
    }

    private fun buildServices(): List<ServiceHolder> {
        return listOf(
            ServiceHolder(
                uuid = BleUUID.SERVICE,
                serviceType = SERVICE_TYPE_PRIMARY,
                characteristicsHolders = buildServiceCharacteristics(),
            )
        )
    }

    private fun buildServiceCharacteristics(): List<CharacteristicHolder> {
        return listOf(
            CharacteristicHolder(
                uuid = BleUUID.CHARACTERISTIC_DEVICE_INFO,
                properties = BluetoothGattCharacteristic.PROPERTY_READ,
                permissions = BluetoothGattCharacteristic.PERMISSION_READ,
                handleReadWrite = { address, value ->
                    Log.d(TAG, "Read value")
                    ReadWriteResult(
                        status = GATT_SUCCESS,
                        value = "ble device 001".toByteArray(),
                    )
                }
            ),
            CharacteristicHolder(
                uuid = BleUUID.CHARACTERISTIC_WIFI,
                properties = BluetoothGattCharacteristic.PROPERTY_WRITE,
                permissions = BluetoothGattCharacteristic.PERMISSION_WRITE,
                handleReadWrite = { address, value ->
                    Log.d(TAG, "Write value: ${value.contentToString()}")
                    ReadWriteResult(
                        status = GATT_SUCCESS,
                    )
                }
            ),
            CharacteristicHolder(
                uuid = BleUUID.CHARACTERISTIC_DEVICE_STATUS,
                properties = BluetoothGattCharacteristic.PROPERTY_INDICATE,
                permissions = BluetoothGattCharacteristic.PERMISSION_READ,
                descriptorHolders = buildDeviceStatusDescriptors(),
                notificationHolder = CharacteristicHolder.NotificationHolder(
                    handleNotification = { address ->
                        Log.d(TAG, "Notification value")
                        val dateFormat =
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val date = Date()
                        val formattedDate = dateFormat.format(date)
                        formattedDate.toByteArray()
                    },
                    intervalSeconds = 1,
                )
            )
        )
    }

    private fun buildDeviceStatusDescriptors(): List<DescriptorHolder> {
        return listOf(
            DescriptorHolder(
                uuid = DescriptorUUID.CLIENT_CHARACTERISTIC_CONFIG,
                permissions = BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE,
                handleReadWrite = { address, value ->
                    Log.d(TAG, "Descriptor read/write value: ${value.contentToString()}")
                    DescriptorHolder.ReadWriteResult(
                        status = GATT_SUCCESS,
                    )
                }
            )
        )
    }

    private suspend fun startAdvertiser() {
        val advertiseSettings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .build()

        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(BleUUID.SERVICE))
            .addManufacturerData(Manufacturer.ID, Manufacturer.data)
            .build()

        val result = bleAdvertiser.start(advertiseSettings, advertiseData)
        if (result == ADVERTISE_SUCCESS) {
            Log.d(TAG, "Advertising started successfully")
        } else {
            val errorMessage = "Advertising failed with error code: $result, message: ${
                advertiseErrorMessage(result)
            }"
            Log.e(TAG, errorMessage)
            sendEvent(BleServerEvent.ShowToast(errorMessage))

            sendAction(BleServerAction.Stop)
        }
    }

    override fun onCleared() {
        bluetoothStateMonitor.stop()
        bleServerStop()
    }

    private fun advertiseErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            ADVERTISE_FAILED_DATA_TOO_LARGE -> "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes."
            ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Failed to start advertising because no advertising instance is available."
            ADVERTISE_FAILED_ALREADY_STARTED -> "Failed to start advertising as the advertising is already started."
            ADVERTISE_FAILED_INTERNAL_ERROR -> "Failed to start advertising due to an internal error."
            ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "This feature is not supported on this platform."
            else -> "Unknown error code: $errorCode"
        }
    }

    companion object {
        private const val TAG = "BleServerViewModel"
    }
}

class BleServerViewModelFactory(
    private val dependency: Dependency,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BleServerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BleServerViewModel(dependency) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
