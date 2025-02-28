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
import com.thoughtworks.bleconn.definitions.DescriptorUUID
import com.thoughtworks.bleconn.utils.CallbackHolder
import com.thoughtworks.bleconn.utils.EnableNotificationCallbackHolder
import com.thoughtworks.bleconn.utils.GattUtils.writeCharacteristicCompact
import com.thoughtworks.bleconn.utils.GattUtils.writeDescriptorCompact
import com.thoughtworks.bleconn.utils.KeyCallbackHolder
import com.thoughtworks.bleconn.utils.NotificationHolder
import com.thoughtworks.bleconn.utils.logger.DefaultLogger
import com.thoughtworks.bleconn.utils.logger.Logger
import com.thoughtworks.bleconn.utils.logger.debug
import com.thoughtworks.bleconn.utils.logger.error
import com.thoughtworks.bleconn.utils.silentResume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import kotlin.coroutines.suspendCoroutine

class BleClient(
    private val context: Context,
    private val logger: Logger = DefaultLogger(),
    connectTimeout: Int = 5000,
    requestTimeout: Int = 3000,
) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
    private var bluetoothGatt: BluetoothGatt? = null

    private var onConnectStateChanged: ((Boolean) -> Unit)? = null
    private val connectCallback = CallbackHolder<Result>(connectTimeout)
    private val discoverServicesCallback = CallbackHolder<DiscoverServicesResult>(requestTimeout)
    private val requestMtuCallback = CallbackHolder<MtuResult>(requestTimeout)
    private val readCallback = KeyCallbackHolder<UUID, ReadResult>(requestTimeout)
    private val writeCallback = KeyCallbackHolder<UUID, Result>(requestTimeout)
    private val enableNotificationCallback =
        EnableNotificationCallbackHolder<UUID, Result, NotificationData>(requestTimeout)
    private val disableNotificationCallback = KeyCallbackHolder<UUID, Result>(requestTimeout)
    private val notificationManager = NotificationHolder<UUID, NotificationData>()
    private var callbackCheckTimer: Timer? = null

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            onConnectStateChanged?.invoke(newState == BluetoothGatt.STATE_CONNECTED)
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                logger.debug(TAG, "Connected to GATT server.")
                connectCallback.resolve(Result(isSuccess = true))
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                val errorMessage = "Disconnected from GATT server."
                logger.debug(TAG, errorMessage)
                stopCallbackCheckLoop()
                connectCallback.resolve(Result(errorMessage = errorMessage))
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                logger.debug(TAG, "GATT services discovered.")
                discoverServicesCallback.resolve(
                    DiscoverServicesResult(
                        isSuccess = true, services = gatt.services
                    )
                )

                gatt.services.forEach {
                    logger.debug(TAG, "Service: ${it.uuid}")
                    it.characteristics.forEach {
                        logger.debug(TAG, "Characteristic: ${it.uuid}")
                    }
                }
            } else {
                val errorMessage = "Failed to discover services. (status: $status)"
                logger.error(TAG, errorMessage)
                discoverServicesCallback.resolve(
                    DiscoverServicesResult(
                        isSuccess = false, errorMessage = errorMessage
                    )
                )
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleOnCharacteristicChanged(characteristic, value)
        }

        // deprecated callback method, for lower sdk
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            handleOnCharacteristicChanged(characteristic, characteristic.value)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            handleOnCharacteristicRead(status, characteristic, value)
        }

        // deprecated callback method, for lower sdk
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            handleOnCharacteristicRead(status, characteristic, characteristic.value)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                logger.debug(TAG, "Characteristic written successfully.")
                writeCallback.getKey()?.let { key ->
                    if (key == characteristic.uuid) {
                        writeCallback.resolve(Result(isSuccess = true))
                    }
                }
            } else {
                val errorMessage = "Characteristic write failed with status: $status"
                logger.debug(TAG, errorMessage)
                writeCallback.getKey()?.let { key ->
                    if (key == characteristic.uuid) {
                        writeCallback.resolve(
                            Result(
                                isSuccess = false,
                                errorMessage = errorMessage
                            )
                        )
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                logger.debug(TAG, "MTU changed to $mtu")
                requestMtuCallback.resolve(MtuResult(isSuccess = true, mtu = mtu))
            } else {
                val errorMessage = "MTU change failed with status $status"
                logger.error(TAG, errorMessage)
                requestMtuCallback.resolve(
                    MtuResult(
                        isSuccess = false,
                        mtu = mtu,
                        errorMessage = errorMessage,
                    )
                )
            }
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
            value: ByteArray,
        ) {
            handleOnDescriptorRead(status)
        }

        // deprecated callback method, for lower sdk
        override fun onDescriptorRead(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int,
        ) {
            handleOnDescriptorRead(status)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                logger.debug(TAG, "Descriptor written successfully.")
                if (descriptor.uuid == DescriptorUUID.CLIENT_CHARACTERISTIC_CONFIG) {
                    handleNotificationSetSuccess(descriptor)
                }
            } else {
                val errorMessage = "Descriptor write failed with status: $status"
                logger.error(TAG, errorMessage)
                if (descriptor.uuid == DescriptorUUID.CLIENT_CHARACTERISTIC_CONFIG) {
                    handleNotificationSetFailed(descriptor, errorMessage)
                }
            }
        }
    }

    private fun handleOnCharacteristicChanged(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ) {
        logger.debug(TAG, "onCharacteristicChanged: ${characteristic.uuid}")
        notificationManager.notify(characteristic.uuid, NotificationData(value))
    }

    private fun handleOnDescriptorRead(status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            logger.debug(TAG, "Descriptor read successfully.")
        } else {
            logger.error(TAG, "Descriptor read failed with status: $status")
        }
    }

    private fun handleOnCharacteristicRead(
        status: Int,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            logger.debug(TAG, "Characteristic read successfully.")
            readCallback.getKey()?.let { key ->
                if (key == characteristic.uuid) {
                    readCallback.resolve(
                        ReadResult(isSuccess = true, value = value)
                    )
                }
            }
        } else {
            val errorMessage = "Characteristic read failed with status: $status"
            logger.debug(TAG, errorMessage)
            readCallback.getKey()?.let { key ->
                if (key == characteristic.uuid) {
                    readCallback.resolve(
                        ReadResult(
                            isSuccess = false,
                            errorMessage = errorMessage
                        )
                    )
                }
            }
        }
    }

    private fun handleNotificationSetFailed(
        descriptor: BluetoothGattDescriptor,
        errorMessage: String,
    ) {
        if (enableNotificationCallback.isSet()) {
            if (enableNotificationCallback.getKey() == descriptor.characteristic.uuid) {
                enableNotificationCallback.getOnNotificationDataHandler()?.let {
                    enableNotificationCallback.resolve(
                        Result(
                            isSuccess = false,
                            errorMessage = errorMessage,
                        )
                    )
                }
            }
        }

        if (disableNotificationCallback.isSet()) {
            if (disableNotificationCallback.getKey() == descriptor.characteristic.uuid) {
                disableNotificationCallback.resolve(
                    Result(
                        isSuccess = false,
                        errorMessage = errorMessage,
                    )
                )
            }
        }
    }

    private fun handleNotificationSetSuccess(descriptor: BluetoothGattDescriptor) {
        if (enableNotificationCallback.isSet()) {
            if (enableNotificationCallback.getKey() == descriptor.characteristic.uuid) {
                enableNotificationCallback.getOnNotificationDataHandler()?.let {
                    notificationManager.add(descriptor.characteristic.uuid, it)
                    enableNotificationCallback.resolve(
                        Result(
                            isSuccess = true,
                        )
                    )
                }
            }
        }

        if (disableNotificationCallback.isSet()) {
            if (disableNotificationCallback.getKey() == descriptor.characteristic.uuid) {
                notificationManager.remove(descriptor.characteristic.uuid)
                disableNotificationCallback.resolve(
                    Result(
                        isSuccess = true,
                    )
                )
            }
        }
    }

    fun isConnected(): Boolean {
        return bluetoothGatt != null
    }

    suspend fun connect(
        deviceAddress: String,
        onConnectStateChanged: (Boolean) -> Unit,
    ): Result {
        return suspendCoroutine { continuation ->
            CoroutineScope(Dispatchers.IO).launch {
                connect(deviceAddress, onConnectStateChanged) { connectResult ->
                    continuation.silentResume(connectResult)
                }
            }
        }
    }

    fun connect(
        deviceAddress: String,
        onConnectStateChanged: (Boolean) -> Unit,
        callback: (Result) -> Unit,
    ): Boolean {
        if (!bluetoothAdapter.isEnabled) {
            val errorMessage = "Bluetooth is not enabled."
            logger.error(TAG, errorMessage)
            callback(Result(errorMessage = errorMessage))
            return false
        }

        if (connectCallback.isSet()) {
            val errorMessage = "Another connection is in progress."
            logger.error(TAG, errorMessage)
            callback(Result(errorMessage = errorMessage))
            return false
        }

        if (!BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
            val errorMessage = "Invalid device address. $deviceAddress"
            logger.error(TAG, errorMessage)
            callback(Result(errorMessage = errorMessage))
            return false
        }

        val device = try {
            bluetoothAdapter.getRemoteDevice(deviceAddress)
        } catch (e: IllegalArgumentException) {
            val errorMessage = "getRemoteDevice failed. ${e.message}"
            logger.error(TAG, errorMessage)
            callback(Result(errorMessage = errorMessage))
            return false
        }

        this.onConnectStateChanged = onConnectStateChanged

        connectCallback.set(callback)
        startCallbackLoop()
        val result = connectDevice(device)
        if (!result) {
            val errorMessage = "Failed to connect to GATT server."
            logger.error(TAG, errorMessage)
            connectCallback.resolve(Result(errorMessage = errorMessage))
            return false
        }

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
            clearCallbacks()
        }
        bluetoothGatt = null
        onConnectStateChanged = null
        logger.debug(TAG, "Disconnected from GATT server.")
        stopCallbackCheckLoop()
    }

    private fun clearCallbacks() {
        val errorMessage = "Manually disconnected GATT"

        readCallback.resolve(
            ReadResult(
                isSuccess = false,
                errorMessage = errorMessage,
            )
        )

        writeCallback.resolve(
            Result(
                isSuccess = false,
                errorMessage = errorMessage,
            )
        )

        requestMtuCallback.resolve(
            MtuResult(
                isSuccess = false,
                errorMessage = errorMessage,
            )
        )

        discoverServicesCallback.resolve(
            DiscoverServicesResult(
                isSuccess = false, errorMessage = errorMessage
            )
        )

        connectCallback.resolve(
            Result(
                isSuccess = false,
                errorMessage = errorMessage,
            )
        )

        notificationManager.clear()
    }

    suspend fun discoverServices(): DiscoverServicesResult {
        return suspendCoroutine { continuation ->
            CoroutineScope(Dispatchers.IO).launch {
                discoverServices { discoverServicesResult ->
                    continuation.silentResume(discoverServicesResult)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun discoverServices(callback: (DiscoverServicesResult) -> Unit): Boolean {
        if (bluetoothGatt == null) {
            val errorMessage = "BluetoothGatt is null."
            logger.error(TAG, errorMessage)
            callback(DiscoverServicesResult(errorMessage = errorMessage))
            return false
        }

        if (discoverServicesCallback.isSet()) {
            val errorMessage = "Another discover services is in progress."
            logger.error(TAG, errorMessage)
            callback(DiscoverServicesResult(errorMessage = errorMessage))
            return false
        }

        bluetoothGatt!!.let { gatt ->
            discoverServicesCallback.set(callback)
            val result = gatt.discoverServices()
            if (!result) {
                val errorMessage = "Failed to discover services."
                logger.error(TAG, errorMessage)
                discoverServicesCallback.resolve(DiscoverServicesResult(errorMessage = errorMessage))
                return false
            }

            return true
        }
    }

    suspend fun requestMtu(mtu: Int): MtuResult {
        return suspendCoroutine { continuation ->
            CoroutineScope(Dispatchers.IO).launch {
                requestMtu(mtu) { result ->
                    continuation.silentResume(result)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun requestMtu(mtu: Int, callback: (MtuResult) -> Unit): Boolean {
        if (bluetoothGatt == null) {
            val errorMessage = "BluetoothGatt is null."
            logger.error(TAG, errorMessage)
            callback(MtuResult(errorMessage = errorMessage))
            return false
        }

        if (requestMtuCallback.isSet()) {
            val errorMessage = "Another request MTU is in progress."
            logger.error(TAG, errorMessage)
            callback(MtuResult(errorMessage = errorMessage))
            return false
        }

        bluetoothGatt!!.let { gatt ->
            requestMtuCallback.set(callback)
            val result = gatt.requestMtu(mtu)
            if (!result) {
                val errorMessage = "Failed to request MTU."
                logger.error(TAG, errorMessage)
                requestMtuCallback.resolve(MtuResult(errorMessage = errorMessage))
                return false
            }

            return true
        }
    }

    suspend fun readCharacteristic(
        serviceUUID: UUID,
        characteristicUUID: UUID,
    ): ReadResult {
        return suspendCoroutine { continuation ->
            CoroutineScope(Dispatchers.IO).launch {
                readCharacteristic(serviceUUID, characteristicUUID) { result ->
                    continuation.silentResume(result)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun readCharacteristic(
        serviceUUID: UUID,
        characteristicUUID: UUID,
        callback: (ReadResult) -> Unit,
    ): Boolean {
        if (bluetoothGatt == null) {
            val errorMessage = "BluetoothGatt is null."
            logger.error(TAG, errorMessage)
            callback(ReadResult(errorMessage = errorMessage))
            return false
        }

        if (readCallback.isSet()) {
            val errorMessage = "Another read characteristic is in progress."
            logger.error(TAG, errorMessage)
            callback(ReadResult(errorMessage = errorMessage))
            return false
        }

        bluetoothGatt!!.let { gatt ->
            val service = gatt.getService(serviceUUID)
            if (service == null) {
                val errorMessage = "Service is null."
                logger.error(TAG, errorMessage)
                callback(ReadResult(errorMessage = errorMessage))
                return false
            }

            val characteristic = service.getCharacteristic(characteristicUUID)
            if (characteristic == null) {
                val errorMessage = "Characteristic is null."
                logger.error(TAG, errorMessage)
                callback(ReadResult(errorMessage = errorMessage))
                return false
            }

            readCallback.set(characteristicUUID, callback)
            val result = gatt.readCharacteristic(characteristic)
            logger.debug(TAG, "gatt.readCharacteristic: $characteristicUUID $result")
            if (!result) {
                val errorMessage = "Failed to read characteristic."
                logger.error(TAG, errorMessage)
                readCallback.resolve(ReadResult(errorMessage = errorMessage))
                return false
            }

            return true
        }
    }

    suspend fun writeCharacteristic(
        serviceUUID: UUID,
        characteristicUUID: UUID,
        value: ByteArray,
        writeType: Int,
    ): Result {
        return suspendCoroutine { continuation ->
            CoroutineScope(Dispatchers.IO).launch {
                writeCharacteristic(serviceUUID, characteristicUUID, value, writeType) { result ->
                    continuation.silentResume(result)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun writeCharacteristic(
        serviceUUID: UUID,
        characteristicUUID: UUID,
        value: ByteArray,
        writeType: Int,
        callback: (Result) -> Unit,
    ): Boolean {
        if (bluetoothGatt == null) {
            val errorMessage = "BluetoothGatt is null."
            logger.error(TAG, errorMessage)
            callback(Result(errorMessage = errorMessage))
            return false
        }

        if (writeCallback.isSet()) {
            val errorMessage = "Another write characteristic is in progress."
            logger.error(TAG, errorMessage)
            callback(Result(errorMessage = errorMessage))
            return false
        }

        bluetoothGatt!!.let { gatt ->
            val service = gatt.getService(serviceUUID)
            if (service == null) {
                val errorMessage = "Service is null."
                logger.error(TAG, errorMessage)
                callback(Result(errorMessage = errorMessage))
                return false
            }

            val characteristic = service.getCharacteristic(characteristicUUID)
            if (characteristic == null) {
                val errorMessage = "Characteristic is null."
                logger.error(TAG, errorMessage)
                callback(Result(errorMessage = errorMessage))
                return false
            }

            writeCallback.set(characteristicUUID, callback)
            val result = gatt.writeCharacteristicCompact(
                characteristic,
                value,
                writeType,
            )
            logger.debug(TAG, "gatt.writeCharacteristicCompact: $characteristicUUID $result")
            if (!result) {
                val errorMessage = "Failed to write characteristic."
                logger.error(TAG, errorMessage)
                writeCallback.resolve(Result(errorMessage = errorMessage))
                return false
            }

            if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
                writeCallback.resolve(Result(isSuccess = true))
            }

            return true
        }
    }

    suspend fun enableCharacteristicNotification(
        serviceUUID: UUID,
        characteristicUUID: UUID,
        confirm: Boolean,
        onNotificationData: (NotificationData) -> Unit,
    ): Result {
        return suspendCoroutine { continuation ->
            CoroutineScope(Dispatchers.IO).launch {
                enableCharacteristicNotification(
                    serviceUUID,
                    characteristicUUID,
                    confirm,
                    onNotificationData
                ) { result ->
                    continuation.silentResume(result)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun enableCharacteristicNotification(
        serviceUUID: UUID,
        characteristicUUID: UUID,
        confirm: Boolean,
        onNotificationData: (NotificationData) -> Unit,
        callback: (Result) -> Unit,
    ): Boolean {
        if (bluetoothGatt == null) {
            val errorMessage = "BluetoothGatt is null."
            logger.error(TAG, errorMessage)
            callback(Result(errorMessage = errorMessage))
            return false
        }

        if (enableNotificationCallback.isSet() || disableNotificationCallback.isSet()) {
            val errorMessage = "Another enable/disable characteristic notification is in progress."
            logger.error(TAG, errorMessage)
            callback(Result(errorMessage = errorMessage))
            return false
        }

        if (notificationManager.contains(characteristicUUID)) {
            val errorMessage =
                "characteristic $characteristicUUID} notification is already enabled."
            logger.error(TAG, errorMessage)
            callback(Result(errorMessage = errorMessage))
            return false
        }

        bluetoothGatt!!.let { gatt ->
            val service = gatt.getService(serviceUUID)
            if (service == null) {
                val errorMessage = "Service is null."
                logger.error(TAG, errorMessage)
                callback(Result(errorMessage = errorMessage))
                return false
            }

            val characteristic = service.getCharacteristic(characteristicUUID)
            if (characteristic == null) {
                val errorMessage = "Characteristic is null."
                logger.error(TAG, errorMessage)
                callback(Result(errorMessage = errorMessage))
                return false
            }

            if (!gatt.setCharacteristicNotification(characteristic, true)) {
                val errorMessage = "Failed to set characteristic notification."
                logger.error(TAG, errorMessage)
                callback(Result(errorMessage = errorMessage))
                return false
            }

            enableNotificationCallback.set(characteristicUUID, callback, onNotificationData)
            if (!gatt.writeDescriptorCompact(
                    characteristic.getDescriptor(DescriptorUUID.CLIENT_CHARACTERISTIC_CONFIG),
                    if (confirm) BluetoothGattDescriptor.ENABLE_INDICATION_VALUE else BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                )
            ) {
                val errorMessage =
                    "Failed to write descriptor ${DescriptorUUID.CLIENT_CHARACTERISTIC_CONFIG}."
                logger.error(TAG, errorMessage)
                enableNotificationCallback.resolve(Result(errorMessage = errorMessage))
                return false
            }

            return true
        }
    }

    suspend fun disableCharacteristicNotification(
        serviceUUID: UUID,
        characteristicUUID: UUID,
    ): Result {
        return suspendCoroutine { continuation ->
            CoroutineScope(Dispatchers.IO).launch {
                disableCharacteristicNotification(serviceUUID, characteristicUUID) { result ->
                    continuation.silentResume(result)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disableCharacteristicNotification(
        serviceUUID: UUID,
        characteristicUUID: UUID,
        callback: (Result) -> Unit,
    ): Boolean {
        if (bluetoothGatt == null) {
            val errorMessage = "BluetoothGatt is null."
            logger.error(TAG, errorMessage)
            callback(Result(errorMessage = errorMessage))
            return false
        }

        if (enableNotificationCallback.isSet() || disableNotificationCallback.isSet()) {
            val errorMessage = "Another disable/disable characteristic notification is in progress."
            logger.error(TAG, errorMessage)
            callback(Result(errorMessage = errorMessage))
            return false
        }

        if (!notificationManager.contains(characteristicUUID)) {
            val errorMessage = "No notification of characteristic $characteristicUUID is set."
            logger.error(TAG, errorMessage)
            callback(Result(errorMessage = errorMessage))
            return false
        }

        bluetoothGatt!!.let { gatt ->
            val service = gatt.getService(serviceUUID)
            if (service == null) {
                val errorMessage = "Service is null."
                logger.error(TAG, errorMessage)
                callback(Result(errorMessage = errorMessage))
                return false
            }

            val characteristic = service.getCharacteristic(characteristicUUID)
            if (characteristic == null) {
                val errorMessage = "Characteristic is null."
                logger.error(TAG, errorMessage)
                callback(Result(errorMessage = errorMessage))
                return false
            }

            if (!gatt.setCharacteristicNotification(characteristic, false)) {
                val errorMessage = "Failed to set characteristic notification."
                logger.error(TAG, errorMessage)
                callback(Result(errorMessage = errorMessage))
                return false
            }

            notificationManager.remove(characteristicUUID)

            disableNotificationCallback.set(characteristicUUID, callback)
            if (!gatt.writeDescriptorCompact(
                    characteristic.getDescriptor(DescriptorUUID.CLIENT_CHARACTERISTIC_CONFIG),
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                )
            ) {
                val errorMessage = "Failed to write descriptor."
                logger.error(TAG, errorMessage)
                disableNotificationCallback.resolve(Result(errorMessage = errorMessage))
                return false
            }

            return true
        }
    }

    private fun startCallbackLoop() {
        logger.debug(TAG, "startCallbackLoop")
        callbackCheckTimer = Timer()
        callbackCheckTimer?.apply {
            schedule(object : TimerTask() {
                override fun run() {
                    if (connectCallback.isTimeout()) {
                        disconnect()
                        connectCallback.resolve(
                            Result(
                                isSuccess = false,
                                errorMessage = "Connect timeout"
                            )
                        )
                    }

                    if (discoverServicesCallback.isTimeout()) {
                        discoverServicesCallback.resolve(
                            DiscoverServicesResult(
                                isSuccess = false,
                                errorMessage = "Discover services timeout"
                            )
                        )
                    }

                    if (requestMtuCallback.isTimeout()) {
                        requestMtuCallback.resolve(
                            MtuResult(
                                isSuccess = false,
                                errorMessage = "Request MTU timeout"
                            )
                        )
                    }

                    if (readCallback.isTimeout()) {
                        readCallback.resolve(
                            ReadResult(
                                isSuccess = false,
                                errorMessage = "Read characteristic timeout"
                            )
                        )
                    }

                    if (writeCallback.isTimeout()) {
                        writeCallback.resolve(
                            Result(
                                isSuccess = false,
                                errorMessage = "Write characteristic timeout"
                            )
                        )
                    }

                    if (enableNotificationCallback.isTimeout()) {
                        enableNotificationCallback.resolve(
                            Result(
                                isSuccess = false,
                                errorMessage = "Enable notification timeout"
                            )
                        )
                    }

                    if (disableNotificationCallback.isTimeout()) {
                        disableNotificationCallback.resolve(
                            Result(
                                isSuccess = false,
                                errorMessage = "Disable notification timeout"
                            )
                        )
                    }
                }
            }, 0, 1000)
        }
    }

    private fun stopCallbackCheckLoop() {
        logger.debug(TAG, "stopCallbackCheckLoop")
        callbackCheckTimer?.cancel()
        callbackCheckTimer = null
    }

    companion object {
        private const val TAG = "BleClient"
    }
}