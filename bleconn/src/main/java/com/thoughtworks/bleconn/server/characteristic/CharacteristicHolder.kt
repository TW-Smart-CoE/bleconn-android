package com.thoughtworks.bleconn.server.characteristic

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.thoughtworks.bleconn.server.descriptor.DescriptorHolder
import java.util.UUID

class CharacteristicHolder(
    val uuid: String,
    val properties: Int,
    val permissions: Int,
    val handleReadWrite: (deviceAddress: String, value: ByteArray) -> ReadWriteResult = { _, _ ->
        ReadWriteResult(
            status = BluetoothGatt.GATT_SUCCESS,
        )
    },
    val descriptorHolders: List<DescriptorHolder> = emptyList(),
    val notificationHolder: NotificationHolder? = null,
) {
    class NotificationHolder(
        val handleNotification: () -> ByteArray,
        val intervalSeconds: Int = 0,
        val subscribedDevices: MutableSet<BluetoothDevice> = mutableSetOf(),
    )

    data class ReadWriteResult(
        val status: Int,
        val value: ByteArray = byteArrayOf(),
    )

    init {
        if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 || properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
            require(notificationHolder != null) {
                "NotificationHolder is required when PROPERTY_NOTIFY/PROPERTY_INDICATE is set"
            }
        }
    }

    val characteristic = BluetoothGattCharacteristic(
        UUID.fromString(uuid),
        properties,
        permissions,
    )

    init {
        descriptorHolders.forEach {
            characteristic.addDescriptor(it.descriptor)
        }
    }
}