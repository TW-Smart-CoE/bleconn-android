package com.thoughtworks.bleconn.utils

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.thoughtworks.bleconn.utils.logger.DefaultLogger
import com.thoughtworks.bleconn.utils.logger.Logger
import com.thoughtworks.bleconn.utils.logger.debug

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

    fun start(callback: (Int) -> Unit) {
        if (callbackHolder.isSet()) {
            logger.debug(TAG, "Callback is already set")
            return
        }

        callbackHolder.set(callback)
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothReceiver, filter)
    }

    fun stop() {
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: IllegalArgumentException) {
            logger.debug(TAG, "Receiver is not registered")
        }
        callbackHolder.clear()
    }

    companion object {
        private const val TAG = "BluetoothStateMonitor"
    }
}
