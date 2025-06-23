package com.thoughtworks.bleconn.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import androidx.annotation.RequiresPermission

@SuppressLint("DeprecatedSdkApi")
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
            @Suppress("DEPRECATION")
            characteristic.value = value
            characteristic.writeType = writeType
            @Suppress("DEPRECATION")
            return writeCharacteristic(characteristic)
        }
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    fun BluetoothGatt.writeDescriptorCompact(
        descriptor: BluetoothGattDescriptor,
        value: ByteArray,
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return writeDescriptor(descriptor, value) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = value
            @Suppress("DEPRECATION")
            return writeDescriptor(descriptor)
        }
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    fun BluetoothGattServer.notifyCharacteristicChangedCompact(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        confirm: Boolean,
        value: ByteArray,
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifyCharacteristicChanged(
                device,
                characteristic,
                confirm,
                value,
            ) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = value
            @Suppress("DEPRECATION")
            notifyCharacteristicChanged(device, characteristic, confirm)
        }
    }

    fun getPropertiesString(properties: Int): String {
        val propertyList = mutableListOf<String>()

        if (properties and BluetoothGattCharacteristic.PROPERTY_BROADCAST != 0) {
            propertyList.add("BROADCAST")
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
            propertyList.add("READ")
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
            propertyList.add("WRITE_NO_RESPONSE")
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
            propertyList.add("WRITE")
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
            propertyList.add("NOTIFY")
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
            propertyList.add("INDICATE")
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE != 0) {
            propertyList.add("SIGNED_WRITE")
        }

        return propertyList.joinToString(separator = " ")
    }
}
