package com.thoughtworks.bleconn.utils

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import androidx.annotation.RequiresPermission

object GattUtils {
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    fun BluetoothGatt.writeCharacteristicCompact(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        writeType: Int,
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return writeCharacteristic(
                characteristic,
                value,
                writeType
            ) == BluetoothStatusCodes.SUCCESS
        } else {
            characteristic.value = value
            characteristic.writeType = writeType
            return writeCharacteristic(characteristic)
        }
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    fun BluetoothGattServer.notifyCharacteristicChangedCompact(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        confirm: Boolean,
        toByteArray: ByteArray,
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifyCharacteristicChanged(
                device,
                characteristic,
                confirm,
                toByteArray,
            ) == BluetoothStatusCodes.SUCCESS
        } else {
            characteristic.value = toByteArray
            notifyCharacteristicChanged(device, characteristic, confirm)
        }
    }
}

