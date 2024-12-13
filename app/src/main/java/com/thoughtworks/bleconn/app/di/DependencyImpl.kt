package com.thoughtworks.bleconn.app.di

import android.content.Context
import com.thoughtworks.bleconn.advertiser.BleAdvertiser
import com.thoughtworks.bleconn.app.foundation.dispatcher.AppDispatchers
import com.thoughtworks.bleconn.app.foundation.dispatcher.CoroutineDispatchers
import com.thoughtworks.bleconn.client.BleClient
import com.thoughtworks.bleconn.scanner.BleScanner
import com.thoughtworks.bleconn.server.BleServer
import com.thoughtworks.bleconn.utils.logger.DefaultLogger
import com.thoughtworks.bleconn.utils.navigator.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class DependencyImpl(
    context: Context,
) : Dependency {
    private var _navigator: Navigator? = null
    private val _context = context
    private val _coroutineDispatchers: CoroutineDispatchers = AppDispatchers()
    private val _coroutineScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + _coroutineDispatchers.defaultDispatcher)
    private val _bleLogger = DefaultLogger()
    private val _bleServer = BleServer(context, _bleLogger)
    private val _bleClient = BleClient(context, _bleLogger)
    private val _bleAdvertiser = BleAdvertiser(context, _bleLogger)
    private val _bleScanner = BleScanner(context, _bleLogger)

    override fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    override val navigator: Navigator
        get() = _navigator!!

    override val context: Context
        get() = _context

    override val coroutineDispatchers: CoroutineDispatchers
        get() = _coroutineDispatchers

    override val coroutineScope: CoroutineScope
        get() = _coroutineScope

    override val bleServer: BleServer
        get() = _bleServer

    override val bleClient: BleClient
        get() = _bleClient

    override val bleAdvertiser: BleAdvertiser
        get() = _bleAdvertiser

    override val bleScanner: BleScanner
        get() = _bleScanner
}