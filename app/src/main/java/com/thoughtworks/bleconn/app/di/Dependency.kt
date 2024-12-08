package com.thoughtworks.bleconn.app.di

import android.content.Context
import com.thoughtworks.bleconn.advertiser.BleAdvertiser
import com.thoughtworks.bleconn.app.foundation.dispatcher.CoroutineDispatchers
import com.thoughtworks.bleconn.client.BleClient
import com.thoughtworks.bleconn.scanner.BleScanner
import com.thoughtworks.bleconn.server.BleServer
import com.thoughtworks.bleconn.utils.navigator.Navigator
import kotlinx.coroutines.CoroutineScope

interface Dependency {
    fun setNavigator(navigator: Navigator)

    val navigator: Navigator
    val context: Context
    val coroutineDispatchers: CoroutineDispatchers
    val coroutineScope: CoroutineScope
    val bleServer: BleServer
    val bleClient: BleClient
    val bleAdvertiser: BleAdvertiser
    val bleScanner: BleScanner
}