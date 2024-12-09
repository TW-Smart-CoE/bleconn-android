package com.thoughtworks.bleconn.server.descriptor

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattDescriptor
import java.util.UUID

class DescriptorHolder(
    val uuid: UUID,
    val permissions: Int,
    val handleReadWrite: (deviceAddress: String, value: ByteArray) -> ReadWriteResult = { _, _ ->
        ReadWriteResult(
            status = BluetoothGatt.GATT_SUCCESS,
        )
    },
) {
    data class ReadWriteResult(
        val status: Int,
        val value: ByteArray = byteArrayOf(),
    )

    val descriptor = BluetoothGattDescriptor(
        uuid,
        permissions,
    )
}