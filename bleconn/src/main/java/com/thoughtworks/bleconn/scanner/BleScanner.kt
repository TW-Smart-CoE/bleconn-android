package com.thoughtworks.bleconn.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context

class BleScanner(
    context: Context,
) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val scanner = bluetoothAdapter.bluetoothLeScanner
    private var scanCallback: ScanCallback? = null

    @SuppressLint("MissingPermission")
    fun start(
        filters: List<ScanFilter>,
        settings: ScanSettings,
        onFound: (result: ScanResult) -> Unit,
        onError: (errorCode: Int) -> Unit = {},
    ) {
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
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        scanCallback?.let {
            scanner.stopScan(it)
            scanCallback = null
        }
    }

    companion object {
        private const val TAG = "BleScanner"
    }
}