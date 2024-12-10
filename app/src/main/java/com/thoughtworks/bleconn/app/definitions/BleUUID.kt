package com.thoughtworks.bleconn.app.definitions

import java.util.UUID

object BleUUID {
    val SERVICE = UUID.fromString("c27d7b88-26a5-4d6c-be82-7d7873dad979")
    val CHARACTERISTIC_DEVICE_INFO = UUID.fromString("5efe1dfb-f80a-411c-9a6b-41caf5ac6dba")
    val CHARACTERISTIC_WIFI = UUID.fromString("5cef40d1-c4c5-431c-b159-a7e895fce2bc")
    val CHARACTERISTIC_DEVICE_STATUS = UUID.fromString("55bedfda-55a9-4d2e-b2df-8d7e1ae3faf3")
}