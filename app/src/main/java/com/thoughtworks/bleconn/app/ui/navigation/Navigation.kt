package com.thoughtworks.bleconn.app.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.thoughtworks.bleconn.app.di.Dependency
import com.thoughtworks.bleconn.app.ui.views.bleclient.BleClientScreen
import com.thoughtworks.bleconn.app.ui.views.blescanner.BleScannerScreen
import com.thoughtworks.bleconn.app.ui.views.bleserver.BleServerScreen
import com.thoughtworks.bleconn.app.ui.views.permission.PermissionRequestScreen
import com.thoughtworks.bleconn.app.ui.views.select.SelectScreen
import com.thoughtworks.bleconn.utils.BluetoothPermissionUtils
import com.thoughtworks.bleconn.utils.navigator.NavigatorImpl

@SuppressLint("InlinedApi")
@Composable
fun Navigation(dependency: Dependency) {
    val navController = rememberNavController()
    dependency.setNavigator(NavigatorImpl(navController))

    NavHost(navController = navController, startDestination = Screen.PermissionScreen.route) {
        composable(route = Screen.PermissionScreen.route) {
            PermissionRequestScreen(
                requiredPermissions = BluetoothPermissionUtils.runtimePermissions(),
                onAllPermissionsGranted = {
                    dependency.navigator.navigateToSelectScreen()
                }
            )
        }
        composable(route = Screen.SelectScreen.route) {
            SelectScreen(dependency)
        }
        composable(route = Screen.BleServerScreen.route) {
            BleServerScreen(dependency)
        }
        composable(route = Screen.BleScannerScreen.route) {
            BleScannerScreen(dependency)
        }
        composable(route = Screen.BleClientScreen.route) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address") ?: ""
            BleClientScreen(dependency, address)
        }
    }
}