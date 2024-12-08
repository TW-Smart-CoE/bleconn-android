package com.thoughtworks.bleconn.server.service

import android.bluetooth.BluetoothGattService
import com.thoughtworks.bleconn.server.characteristic.CharacteristicWrapper
import java.util.UUID

class ServiceWrapper(
    val uuid: String,
    val serviceType: Int,
    val characteristicsWrappers: List<CharacteristicWrapper>,
) {
    val service = BluetoothGattService(
        UUID.fromString(uuid),
        serviceType
    )

    init {
        characteristicsWrappers.forEach {
            service.addCharacteristic(it.characteristic)
        }
    }
}