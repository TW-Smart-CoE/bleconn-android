package com.thoughtworks.bleconn.utils.navigator

interface Navigator {
    fun navigateBack()
    fun navigateToSelectScreen()
    fun navigateToBleServerScreen()
    fun navigateToBleScannerScreen()
    fun navigateToBleClientScreen(address: String)
}