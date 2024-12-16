package com.thoughtworks.bleconn.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.thoughtworks.bleconn.utils.logger.DefaultLogger
import com.thoughtworks.bleconn.utils.logger.Logger
import com.thoughtworks.bleconn.utils.logger.error

class BleScanner(
    context: Context,
    private val logger: Logger = DefaultLogger(),
) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private var scanCallback: ScanCallback? = null

    fun isStarted(): Boolean {
        return scanCallback != null
    }

    @SuppressLint("MissingPermission")
    fun start(
        filters: List<ScanFilter>,
        settings: ScanSettings,
        onFound: (result: ScanResult) -> Unit,
        onError: (errorCode: Int) -> Unit = {},
    ): Boolean {
        if (!bluetoothAdapter.isEnabled) {
            logger.error(TAG, "Bluetooth is not enabled")
            onError(ScanCallback.SCAN_FAILED_INTERNAL_ERROR)
            return false
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            logger.error(TAG, "Failed to get scanner")
            onError(ScanCallback.SCAN_FAILED_INTERNAL_ERROR)
            return false
        }

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                onFound(result)
            }

            override fun onBatchScanResults(results: List<ScanResult>) {
                for (result in results) {
                    onFound(result)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                onError(errorCode)
            }
        }

        scanner.startScan(filters, settings, scanCallback)
        return true
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        scanCallback?.let {
            bluetoothAdapter.bluetoothLeScanner?.stopScan(it)
            scanCallback = null
        }
    }

    companion object {
        private const val TAG = "BleScanner"
    }
}