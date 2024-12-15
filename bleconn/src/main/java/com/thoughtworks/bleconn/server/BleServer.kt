package com.thoughtworks.bleconn.server

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.thoughtworks.bleconn.definitions.DescriptorUUID
import com.thoughtworks.bleconn.server.service.ServiceHolder
import com.thoughtworks.bleconn.utils.GattUtils.notifyCharacteristicChangedCompact
import com.thoughtworks.bleconn.utils.logger.DefaultLogger
import com.thoughtworks.bleconn.utils.logger.Logger
import com.thoughtworks.bleconn.utils.logger.debug
import com.thoughtworks.bleconn.utils.logger.error
import java.util.Timer
import java.util.TimerTask

class BleServer(
    private val context: Context,
    private val logger: Logger = DefaultLogger(),
) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var notificationTimer: Timer? = null
    private var gattServer: BluetoothGattServer? = null
    private val serviceHolders = mutableListOf<ServiceHolder>()
    private val subscribedDevicesLock = Any()

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                logger.debug(TAG, "Device connected: ${device.address}")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                logger.debug(TAG, "Device disconnected: ${device.address}")
                removeDisconnectDeviceNotification(device)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic,
        ) {
            logger.debug(TAG, "Read request received from ${device.address}")
            handleCharacteristicReadWrite(
                characteristic,
                device,
                requestId,
                true,
                offset,
                byteArrayOf()
            )
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
            handleCharacteristicReadWrite(
                characteristic,
                device,
                requestId,
                responseNeeded,
                offset,
                value
            )
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor,
        ) {
            logger.debug(TAG, "Descriptor read request received from ${device.address}")
            handleDescriptorReadWrite(descriptor, device, requestId, true, offset, byteArrayOf())
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            logger.debug(TAG, "Descriptor write request received from ${device.address}")
            handleDescriptorReadWrite(descriptor, device, requestId, responseNeeded, offset, value)
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                logger.debug(TAG, "onNotificationSent success")
            } else {
                logger.error(TAG, "onNotificationSent failed with status: $status")
            }
        }
    }

    private fun removeDisconnectDeviceNotification(device: BluetoothDevice) {
        synchronized(subscribedDevicesLock) {
            serviceHolders
                .flatMap { it.characteristicsHolders }
                .forEach { characteristicHolder ->
                    if (characteristicHolder.notificationHolder?.subscribedDevices?.remove(device) == true) {
                        logger.debug(TAG, "Remove device(${device.address}) from notification list")
                    }
                }
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleCharacteristicReadWrite(
        characteristic: BluetoothGattCharacteristic,
        device: BluetoothDevice,
        requestId: Int,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray,
    ) {
        val characteristicHolder = serviceHolders
            .flatMap { it.characteristicsHolders }
            .find { it.uuid == characteristic.uuid }
        characteristicHolder?.let {
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

            val result = characteristicHolder.handleReadWrite(
                device.address,
                value,
            )

            if (responseNeeded) {
                gattServer?.sendResponse(
                    device,
                    requestId,
                    result.status,
                    offset,
                    result.value,
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleDescriptorReadWrite(
        descriptor: BluetoothGattDescriptor,
        device: BluetoothDevice,
        requestId: Int,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray,
    ) {
        val descriptorHolders = serviceHolders
            .flatMap { it.characteristicsHolders }
            .flatMap { it.descriptorHolders }
            .find { it.uuid == descriptor.uuid }
        descriptorHolders?.let {
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

            val result = descriptorHolders.handleReadWrite(
                device.address,
                value,
            )

            if (responseNeeded) {
                gattServer?.sendResponse(
                    device,
                    requestId,
                    result.status,
                    offset,
                    result.value,
                )
            }

            // write descriptor value and success
            if (value.isNotEmpty() && result.status == BluetoothGatt.GATT_SUCCESS) {
                arrangeSubscribedDevices(device, descriptor, value)
            }
        }
    }

    private fun arrangeSubscribedDevices(
        device: BluetoothDevice,
        descriptor: BluetoothGattDescriptor,
        value: ByteArray,
    ) {
        if (descriptor.uuid == DescriptorUUID.CLIENT_CHARACTERISTIC_CONFIG) {
            val characteristic = descriptor.characteristic
            val characteristicHolder = serviceHolders
                .flatMap { it.characteristicsHolders }
                .find { it.uuid == characteristic.uuid }
            characteristicHolder?.let { holder ->
                synchronized(subscribedDevicesLock) {
                    when {
                        value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) -> {
                            logger.debug(
                                TAG,
                                "Notifications enabled for ${characteristic.uuid}"
                            )
                            if (holder.notificationHolder?.subscribedDevices?.find { it.address == device.address } == null) {
                                holder.notificationHolder?.subscribedDevices?.add(device)
                            }
                        }

                        value.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE) -> {
                            logger.debug(TAG, "Indications enabled for ${characteristic.uuid}")
                            if (holder.notificationHolder?.subscribedDevices?.find { it.address == device.address } == null) {
                                holder.notificationHolder?.subscribedDevices?.add(device)
                            }
                        }

                        value.contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) -> {
                            logger.debug(
                                TAG,
                                "Notifications/Indications disabled for ${characteristic.uuid}"
                            )
                            holder.notificationHolder?.subscribedDevices?.remove(device)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun start(serviceHolders: List<ServiceHolder>): Boolean {
        if (!bluetoothManager.adapter.isEnabled) {
            logger.error(TAG, "Bluetooth is not enabled")
            return false
        }

        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
        gattServer?.apply {
            serviceHolders.forEach { serviceWrapper ->
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

        this.serviceHolders.addAll(serviceHolders)
        startNotificationLoop()
        return true
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        stopNotificationLoop()
        gattServer?.close()
        gattServer = null
        serviceHolders.clear()
    }

    private fun startNotificationLoop() {
        notificationTimer = Timer()
        notificationTimer?.apply {
            schedule(object : TimerTask() {
                override fun run() {
                    serviceHolders
                        .flatMap { it.characteristicsHolders }
                        .forEach { characteristicHolder ->
                            characteristicHolder.notificationHolder?.let { notificationHolder ->
                                synchronized(subscribedDevicesLock) {
                                    if (notificationHolder.subscribedDevices.isNotEmpty()) {
                                        val currentTime = System.currentTimeMillis() / 1000
                                        if (notificationHolder.intervalSeconds > 0 &&
                                            currentTime % notificationHolder.intervalSeconds == 0L
                                        ) {
                                            val tobeRemoved = mutableListOf<BluetoothDevice>()
                                            notificationHolder.subscribedDevices.forEach { device ->
                                                val data =
                                                    notificationHolder.handleNotification(device.address)
                                                try {
                                                    sendNotification(
                                                        device,
                                                        characteristicHolder.characteristic,
                                                        data
                                                    )
                                                } catch (t: Throwable) {
                                                    logger.error(
                                                        TAG,
                                                        "Failed to send notification. ${t.message}"
                                                    )
                                                    tobeRemoved.add(device)
                                                }
                                            }
                                            if (tobeRemoved.isNotEmpty()) {
                                                logger.debug(
                                                    TAG,
                                                    "Remove devices: ${tobeRemoved.joinToString { it.address }}"
                                                )
                                                notificationHolder.subscribedDevices.removeAll(tobeRemoved)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                }
            }, 0, 1000)
        }
    }

    private fun stopNotificationLoop() {
        notificationTimer?.cancel()
        notificationTimer = null
    }


    @SuppressLint("MissingPermission")
    fun sendNotification(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ) {
        if (gattServer == null) {
            logger.error(TAG, "Gatt server is not started")
            return
        }

        val confirm =
            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
        if (!gattServer!!.notifyCharacteristicChangedCompact(
                device,
                characteristic,
                confirm,
                value
            )
        ) {
            logger.error(TAG, "Failed to send notification to ${device.address}")
        } else {
            logger.debug(TAG, "Send notification. (confirm = $confirm)")
        }
    }

    companion object {
        private const val TAG = "BleServer"
    }
}