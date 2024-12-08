package com.thoughtworks.bleconn.app.definitions

object Manufacturer {
    const val ID = 0x1234
    val data = byteArrayOf(0x00, 0x00, 0x00, 0x01, 0x00, 0x02)
    val dataMask = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x00, 0x00)
}