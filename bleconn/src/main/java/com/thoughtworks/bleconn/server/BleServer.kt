package com.thoughtworks.bleconn.server

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.thoughtworks.bleconn.server.service.ServiceWrapper
import com.thoughtworks.bleconn.utils.GattUtils.notifyCharacteristicChangedCompact
import com.thoughtworks.bleconn.utils.TestUUID
import com.thoughtworks.bleconn.utils.logger.DefaultLogger
import com.thoughtworks.bleconn.utils.logger.Logger
import com.thoughtworks.bleconn.utils.logger.debug
import com.thoughtworks.bleconn.utils.logger.error
import java.util.UUID

class BleServer(
    private val context: Context,
    private val logger: Logger = DefaultLogger(),
) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var gattServer: BluetoothGattServer? = null
    private val serviceWrappers = mutableListOf<ServiceWrapper>()

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                logger.debug(TAG, "Device connected: ${device.address}")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                logger.debug(TAG, "Device disconnected: ${device.address}")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic,
        ) {
            logger.debug(TAG, "Read request received from ${device?.address}")
            handleCharacteristicReadWrite(offset, device, requestId)
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            logger.debug(TAG, "Write request received from ${device.address}")
            handleCharacteristicReadWrite(offset, device, requestId)
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                logger.debug(TAG, "Indication acknowledged by client")
            } else {
                logger.error(TAG, "Indication failed with status: $status")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleCharacteristicReadWrite(
        offset: Int,
        device: BluetoothDevice,
        requestId: Int,
    ) {
        serviceWrappers.forEach { serviceWrapper ->
            if (offset != 0) {
                gattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_INVALID_OFFSET,
                    offset,
                    null
                )
                return
            }

            serviceWrapper.characteristicsWrappers.forEach { characteristicWrapper ->
                gattServer?.getService(UUID.fromString(serviceWrapper.uuid))?.getCharacteristic(
                    UUID.fromString(characteristicWrapper.uuid)
                )?.let { characteristic ->
                    val result = characteristicWrapper.handleReadWrite(
                        device.address,
                        ByteArray(0)
                    )
                    gattServer?.sendResponse(device, requestId, result, offset, null)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun start(serviceWrappers: List<ServiceWrapper>): Boolean {
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
        gattServer?.apply {
            serviceWrappers.forEach { serviceWrapper ->
                if (!addService(serviceWrapper.service)) {
                    stop()
                    return false
                }
            }

            services.forEach {
                logger.debug(TAG, "Service: ${it.uuid}")
                it.characteristics.forEach {
                    logger.debug(TAG, "Characteristic: ${it.uuid}")
                }
            }
        }

        this.serviceWrappers.addAll(serviceWrappers)
        return true
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        gattServer?.close()
        gattServer = null
        serviceWrappers.clear()
    }

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)

        val service = BluetoothGattService(
            UUID.fromString(TestUUID.ServerUUID),
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        val wifiConfigCharacteristic = BluetoothGattCharacteristic(
            UUID.fromString(TestUUID.WifiConfigCharacteristicUUID),
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        val wifiResultCharacteristic = BluetoothGattCharacteristic(
            UUID.fromString(TestUUID.WifiConfigCharacteristicResultUUID),
            BluetoothGattCharacteristic.PROPERTY_INDICATE,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        val cccd = BluetoothGattDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
            BluetoothGattDescriptor.PERMISSION_WRITE or BluetoothGattDescriptor.PERMISSION_READ
        )
        wifiResultCharacteristic.addDescriptor(cccd)

        service.addCharacteristic(wifiConfigCharacteristic)
        service.addCharacteristic(wifiResultCharacteristic)

        gattServer?.addService(service)

        logger.debug(TAG, "Service UUID: ${service.uuid}")
        service.characteristics.forEach { char ->
            logger.debug(TAG, "Characteristic UUID: ${char.uuid}")
            logger.debug(TAG, "Properties: ${char.properties}")
            logger.debug(TAG, "Permissions: ${char.permissions}")
        }

        return true
    }

    @SuppressLint("MissingPermission")
    fun sendIndication(device: BluetoothDevice, success: Boolean) {
        gattServer?.getService(UUID.fromString(TestUUID.ServerUUID))?.getCharacteristic(
            UUID.fromString(TestUUID.WifiConfigCharacteristicResultUUID)
        )?.let { characteristic ->
            val result = if (success) "true" else "false"
            gattServer?.notifyCharacteristicChangedCompact(
                device,
                characteristic,
                true,
                result.toByteArray()
            )
        }
    }

    companion object {
        private const val TAG = "BleServer"
    }
}