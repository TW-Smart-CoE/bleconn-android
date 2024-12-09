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
)