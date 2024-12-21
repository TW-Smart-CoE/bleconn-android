package com.thoughtworks.bleconn.utils

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.thoughtworks.bleconn.utils.logger.DefaultLogger
import com.thoughtworks.bleconn.utils.logger.Logger
import com.thoughtworks.bleconn.utils.logger.debug
import com.thoughtworks.bleconn.utils.logger.error

class BluetoothStateMonitor(
    private val context: Context,
    private val logger: Logger = DefaultLogger(),
) {
    val callbackHolder: CallbackHolder<Int> = CallbackHolder()

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF -> {
                        logger.debug(TAG, "Bluetooth is OFF")
                    }

                    BluetoothAdapter.STATE_ON -> {
                        logger.debug(TAG, "Bluetooth is ON")
                    }
                }
                callbackHolder.resolve(state)
            }
        }
    }

    fun start(callback: (Int) -> Unit): Boolean {
        if (callbackHolder.isSet()) {
            logger.debug(TAG, "Callback is already set")
            return false
        }

        logger.debug(TAG, "Start monitoring Bluetooth state")
        callbackHolder.set(callback)
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)

        return try {
            val result = context.registerReceiver(bluetoothReceiver, filter)
            if (result == null) {
                logger.error(TAG, "Failed to register receiver")
                callbackHolder.clear()
                false
            } else {
                true
            }
        } catch (e: Throwable) {
            logger.error(TAG, "Receiver is already registered")
            callbackHolder.clear()
            false
        }
    }

    fun stop() {
        logger.debug(TAG, "Stop monitoring Bluetooth state")
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: IllegalArgumentException) {
            logger.error(TAG, "Receiver is not registered")
        }
        callbackHolder.clear()
    }

    companion object {
        private const val TAG = "BluetoothStateMonitor"
    }
}
