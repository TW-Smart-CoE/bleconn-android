package com.thoughtworks.bleconn.client

import android.bluetooth.BluetoothGattService

abstract class BaseResult(
    val isSuccess: Boolean = false,
    val errorMessage: String = "",
)

class Result(
    isSuccess: Boolean = false,
    errorMessage: String = "",
) : BaseResult(isSuccess, errorMessage)

class DiscoverServicesResult(
    isSuccess: Boolean = false,
    errorMessage: String = "",
    val services: List<BluetoothGattService> = emptyList(),
) : BaseResult(isSuccess, errorMessage)

class MtuResult(
    isSuccess: Boolean = false,
    errorMessage: String = "",
    val mtu: Int = 0,
) : BaseResult(isSuccess, errorMessage)

class ReadResult(
    isSuccess: Boolean = false,
    errorMessage: String = "",
    val value: ByteArray = byteArrayOf(),
) : BaseResult(isSuccess, errorMessage)

data class NotificationData(
    val value: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotificationData

        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }
}