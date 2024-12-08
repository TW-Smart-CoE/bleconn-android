package com.thoughtworks.bleconn.server.characteristic

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.thoughtworks.bleconn.server.descriptor.DescriptorWrapper
import java.util.UUID

class CharacteristicWrapper(
    val uuid: String,
    val properties: Int,
    val permissions: Int,
    val handleReadWrite: (deviceAddress: String, value: ByteArray) -> Int = { _, _ -> BluetoothGatt.GATT_SUCCESS },
    val descriptorWrappers: List<DescriptorWrapper> = emptyList(),
) {
    val characteristic = BluetoothGattCharacteristic(
        UUID.fromString(uuid),
        properties,
        permissions,
    )

    init {
        descriptorWrappers.forEach {
            characteristic.addDescriptor(it.descriptor)
        }
    }
}