package com.thoughtworks.bleconn.advertiser

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import com.thoughtworks.bleconn.utils.logger.DefaultLogger
import com.thoughtworks.bleconn.utils.logger.Logger
import com.thoughtworks.bleconn.utils.logger.debug
import com.thoughtworks.bleconn.utils.logger.error
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BleAdvertiser(
    context: Context,
    private val logger: Logger = DefaultLogger(),
) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val advertiser = bluetoothAdapter.bluetoothLeAdvertiser
    private var advertiseCallback: AdvertiseCallback? = null

    @SuppressLint("MissingPermission")
    private fun start(
        settings: AdvertiseSettings,
        data: AdvertiseData,
        callback: AdvertiseCallback,
    ) {
        advertiser.startAdvertising(settings, data, callback)
    }

    suspend fun start(
        settings: AdvertiseSettings,
        data: AdvertiseData,
    ): Int {
        return suspendCoroutine { continuation ->
            CoroutineScope(Dispatchers.IO).launch {
                advertiseCallback = object : AdvertiseCallback() {
                    private fun resumeWithoutComplain(result: Int) {
                        try {
                            continuation.resume(
                                result
                            )
                        } catch (_: Exception) {
                        } finally {
                        }
                    }

                    override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                        logger.debug(TAG, "Advertising started successfully")
                        resumeWithoutComplain(0)
                    }

                    override fun onStartFailure(errorCode: Int) {
                        logger.error(TAG, "Advertising failed with error code: $errorCode")
                        resumeWithoutComplain(errorCode)
                    }
                }

                start(settings, data, advertiseCallback!!)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        advertiseCallback?.let {
            advertiser.stopAdvertising(it)
            advertiseCallback = null
        }
    }

    companion object {
        private const val TAG = "BleAdvertiser"
    }
}