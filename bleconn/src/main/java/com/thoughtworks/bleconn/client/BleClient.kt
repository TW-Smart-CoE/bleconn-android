package com.thoughtworks.bleconn.client

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.content.Context
import com.thoughtworks.bleconn.utils.GattUtils.writeCharacteristicCompact
import com.thoughtworks.bleconn.utils.TestUUID
import com.thoughtworks.bleconn.utils.logger.DefaultLogger
import com.thoughtworks.bleconn.utils.logger.Logger
import com.thoughtworks.bleconn.utils.logger.debug
import com.thoughtworks.bleconn.utils.logger.error
import com.thoughtworks.bleconn.utils.silentResume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.coroutines.suspendCoroutine

class BleClient(
    private val context: Context,
    private val logger: Logger = DefaultLogger(),
) {
    data class ConnectResult(
        val connected: Boolean = false,
        val errorMessage: String = "",
    )

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
    private var bluetoothGatt: BluetoothGatt? = null

    private var connectCallback: ((ConnectResult) -> Unit)? = null

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                logger.debug(TAG, "Connected to GATT server.")
                connectCallback?.invoke(ConnectResult(connected = true))
                connectCallback = null
//                bluetoothGatt?.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                val errorMessage = "Disconnected from GATT server."
                logger.debug(TAG, errorMessage)
                connectCallback?.invoke(
                    ConnectResult(
                        connected = false,
                        errorMessage = errorMessage
                    )
                )
                connectCallback = null
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                logger.debug(TAG, "GATT services discovered.")
                gatt.services.forEach {
                    logger.debug(TAG, "Service: ${it.uuid}")
                    it.characteristics.forEach {
                        logger.debug(TAG, "Characteristic: ${it.uuid}")
                    }
                }
//                setupIndication(gatt)
//                gatt.requestMtu(128)
            } else {
                logger.debug(TAG, "onServicesDiscovered received: $status")
            }
        }

        @SuppressLint("MissingPermission")
        private fun setupIndication(gatt: BluetoothGatt) {
            val service = gatt.getService(UUID.fromString(TestUUID.ServerUUID))
            val characteristic =
                service.getCharacteristic(UUID.fromString(TestUUID.WifiConfigCharacteristicResultUUID))
            gatt.setCharacteristicNotification(characteristic, true)

            val descriptor =
                characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            if (descriptor == null) {
                logger.error(TAG, "Descriptor is null.")
                return
            }
            descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            val receivedData = String(characteristic.value)
            logger.debug(TAG, "Received Indication: $receivedData")
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                logger.debug(TAG, "Characteristic written successfully.")
            } else {
                logger.debug(TAG, "Characteristic write failed with status: $status")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                logger.debug("BLE", "MTU changed to $mtu")
                gatt.getService(UUID.fromString(TestUUID.ServerUUID))?.let { service ->
                    service.getCharacteristic(UUID.fromString(TestUUID.WifiConfigCharacteristicUUID))
                        ?.let { characteristic ->
                            logger.debug(TAG, "Writing characteristic.")
                            val bytes = "12345678901234____________sadfasdfasdfadsfas".toByteArray()
                            if (gatt.writeCharacteristicCompact(
                                    characteristic,
                                    bytes,
                                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                                )
                            ) {
                                logger.debug(TAG, "Characteristic written successfully.")
                            } else {
                                logger.debug(TAG, "Characteristic write failed.")
                            }
                        }
                }
            } else {
                logger.error(TAG, "MTU change failed with status $status")
            }
        }
    }

    suspend fun connect(deviceAddress: String): ConnectResult {
        return suspendCoroutine { continuation ->
            CoroutineScope(Dispatchers.IO).launch {
                connect(deviceAddress) { connectResult ->
                    continuation.silentResume(connectResult)
                }
            }
        }
    }

    fun connect(
        deviceAddress: String,
        callback: (connectResult: ConnectResult) -> Unit,
    ): Boolean {
        if (connectCallback != null) {
            val errorMessage = "Another connection is in progress."
            logger.error(TAG, errorMessage)
            callback(ConnectResult(errorMessage = errorMessage))
            return false
        }

        if (!BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
            val errorMessage = "Invalid device address. $deviceAddress"
            logger.error(TAG, errorMessage)
            callback(ConnectResult(errorMessage = errorMessage))
            return false
        }

        val device = try {
            bluetoothAdapter.getRemoteDevice(deviceAddress)
        } catch (e: IllegalArgumentException) {
            val errorMessage = "Invalid device address. ${e.message}"
            logger.error(TAG, errorMessage)
            callback(ConnectResult(errorMessage = errorMessage))
            return false
        }

        val result = connectDevice(device)
        if (!result) {
            val errorMessage = "Failed to connect to GATT server."
            logger.error(TAG, errorMessage)
            callback(ConnectResult(errorMessage = errorMessage))
            return false
        }

        this.connectCallback = callback
        return true
    }

    @SuppressLint("MissingPermission")
    private fun connectDevice(device: BluetoothDevice): Boolean {
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
        return bluetoothGatt != null
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.apply {
            disconnect()
            close()
        }
        bluetoothGatt = null
        logger.debug(TAG, "Disconnected from GATT server.")
    }

    companion object {
        private const val TAG = "BleClient"
    }
}