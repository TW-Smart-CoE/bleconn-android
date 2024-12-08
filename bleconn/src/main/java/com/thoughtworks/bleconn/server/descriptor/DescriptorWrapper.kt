package com.thoughtworks.bleconn.server.descriptor

import android.bluetooth.BluetoothGattDescriptor
import java.util.UUID

class DescriptorWrapper(
    val uuid: String,
    val permissions: Int,
) {
    val descriptor = BluetoothGattDescriptor(
        UUID.fromString(uuid),
        permissions,
    )
}