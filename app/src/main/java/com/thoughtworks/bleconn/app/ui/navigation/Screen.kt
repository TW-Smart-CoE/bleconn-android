package com.thoughtworks.bleconn.app.ui.navigation

sealed class Screen(val route: String) {
    data object PermissionScreen : Screen("permission_screen")
    data object SelectScreen : Screen("select_screen")
    data object BleServerScreen : Screen("ble_server_screen")
    data object BleScannerScreen : Screen("ble_scanner_screen")
    data object BleClientScreen : Screen("ble_client_screen/{address}")

    fun routeWithArgs(vararg args: String): String {
        return buildString {
            append(route.split("/").first())
            args.forEach { arg ->
                append("/$arg")
            }
        }
    }
}

