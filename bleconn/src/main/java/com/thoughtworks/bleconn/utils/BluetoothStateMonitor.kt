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
    var listener: ((Int) -> Unit)? = null

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
                listener?.invoke(state)
            }
        }
    }

    fun start(listener: (Int) -> Unit): Boolean {
        if (this.listener != null) {
            logger.debug(TAG, "Callback is already set")
            return false
        }

        logger.debug(TAG, "Start monitoring Bluetooth state")
        this.listener = listener
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)

        return try {
            context.registerReceiver(bluetoothReceiver, filter)
            return true
        } catch (e: Throwable) {
            logger.error(TAG, e.message.toString())
            this.listener = null
            false
        }
    }

    fun stop() {
        logger.debug(TAG, "Stop monitoring Bluetooth state")
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: IllegalArgumentException) {
            logger.error(TAG, e.message.toString())
        }
        this.listener = null
    }

    companion object {
        private const val TAG = "BluetoothStateMonitor"
    }
}
