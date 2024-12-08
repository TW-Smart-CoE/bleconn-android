package com.thoughtworks.bleconn.utils.navigator

import androidx.navigation.NavController
import com.thoughtworks.bleconn.app.ui.navigation.Screen

class NavigatorImpl(private val navController: NavController) : Navigator {
    override fun navigateBack() {
        navController.navigateUp()
    }

    override fun navigateToSelectScreen() {
        navController.navigate(Screen.SelectScreen.route) {
            popUpTo(Screen.PermissionScreen.route) {
                inclusive = true // This removes the PermissionScreen from the back stack
            }
            launchSingleTop = true // Ensures that only one instance of SelectScreen is created
        }
    }

    override fun navigateToBleServerScreen() {
        navController.navigate(Screen.BleServerScreen.route)
    }

    override fun navigateToBleScannerScreen() {
        navController.navigate(Screen.BleScannerScreen.route)
    }

    override fun navigateToBleClientScreen(address: String) {
        navController.navigate(Screen.BleClientScreen.routeWithArgs(address))
    }
}