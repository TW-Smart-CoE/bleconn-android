package com.thoughtworks.bleconn.server.service

import android.bluetooth.BluetoothGattService
import com.thoughtworks.bleconn.server.characteristic.CharacteristicHolder
import java.util.UUID

class ServiceHolder(
    val uuid: UUID,
    val serviceType: Int,
    val characteristicsHolders: List<CharacteristicHolder>,
) {
    val service = BluetoothGattService(
        uuid,
        serviceType
    )

    init {
        characteristicsHolders.forEach {
            service.addCharacteristic(it.characteristic)
        }
    }
}