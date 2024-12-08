package com.thoughtworks.bleconn.app.ui.views.bleserver

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thoughtworks.bleconn.app.definitions.BleUUID
import com.thoughtworks.bleconn.app.definitions.Manufacturer
import com.thoughtworks.bleconn.app.di.Dependency
import com.thoughtworks.bleconn.app.foundation.mvi.DefaultStore
import com.thoughtworks.bleconn.app.foundation.mvi.MVIViewModel
import com.thoughtworks.bleconn.app.foundation.mvi.Store
import com.thoughtworks.bleconn.server.characteristic.CharacteristicWrapper
import com.thoughtworks.bleconn.server.service.ServiceWrapper
import kotlinx.coroutines.launch


class BleServerViewModel(
    dependency: Dependency,
    store: Store<BleServerState, BleServerEvent, BleServerAction> =
        DefaultStore(
            initialState = BleServerState(
                isStarted = false,
            )
        ),
) : MVIViewModel<BleServerState, BleServerEvent, BleServerAction>(store) {
    private val navigator = dependency.navigator
    private val bleServer = dependency.bleServer
    private val bleAdvertiser = dependency.bleAdvertiser

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
        viewModelScope.launch {
            if (startServer()) {
                startAdvertiser()
            } else {
                Log.e(TAG, "Failed to start server")
            }
        }
    }

    private fun startServer(): Boolean {
        return bleServer.start(buildServices())
    }

    private fun buildServices(): List<ServiceWrapper> {
        return listOf(
            ServiceWrapper(
                uuid = BleUUID.SERVICE,
                serviceType = SERVICE_TYPE_PRIMARY,
                characteristicsWrappers = buildServiceCharacteristics(),
            )
        )
    }

    private fun buildServiceCharacteristics(): List<CharacteristicWrapper> {
        return listOf(
            CharacteristicWrapper(
                uuid = BleUUID.CHARACTERISTIC_WIFI,
                properties = BluetoothGattCharacteristic.PROPERTY_WRITE,
                permissions = BluetoothGattCharacteristic.PERMISSION_WRITE,
                handleReadWrite = { deviceAddress, value ->
                    Log.d(TAG, "Received value: ${value.contentToString()}")
                    GATT_SUCCESS
                }
            )
        )
    }

    private suspend fun startAdvertiser() {
        val advertiseSettings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW)
            .setConnectable(true)
            .build()


        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid.fromString(BleUUID.SERVICE))
            .addManufacturerData(Manufacturer.ID, Manufacturer.data)
            .build()

        val result = bleAdvertiser.start(advertiseSettings, advertiseData)
        if (result == ADVERTISE_SUCCESS) {
            Log.d(TAG, "Advertising started successfully")
        } else {
            Log.e(
                TAG,
                "Advertising failed with error code: $result, message: ${
                    advertiseErrorMessage(result)
                }"
            )
        }
    }

    override fun onCleared() {
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
