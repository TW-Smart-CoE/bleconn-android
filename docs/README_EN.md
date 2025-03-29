# BleConn - Android BLE Connection Library

## Overview
BleConn is an Android library that simplifies Bluetooth Low Energy (BLE) operations including advertising, scanning, client and server functionality. It provides a coroutine-based API for easy integration with modern Android apps.

## Library Components

### Core Features
- **BleAdvertiser**: Start/stop BLE advertising with configurable parameters
- **BleScanner**: Scan for nearby BLE devices with filters
- **BleClient**: Connect to BLE devices and perform operations
- **BleServer**: Create BLE services and characteristics to act as a peripheral

### Utilities
- Bluetooth permission handling
- GATT operation utilities
- Coroutine dispatchers
- Logging system

## Sample App Features
The included sample application demonstrates:
- BLE device scanning and connection
- BLE advertising
- Client operations (read/write characteristics)
- Server operations (create services/characteristics)
- Performance testing

## Getting Started

### Installation
Add the library to your project:

```kotlin
dependencies {
    implementation("com.github.TW-Smart-CoE:bleconn-android:Tag")
}
```

### Basic Usage

```kotlin
// Initialize scanner
val scanner = BleScanner(context)

// Start scanning
scanner.startScan { device ->
    // Handle discovered devices
}

// Initialize client
val client = BleClient(context)

// Connect to device
client.connect(deviceAddress) { connectionState ->
    // Handle connection state changes
}
```

## API Reference

### BleScanner
- `startScan()` - Start BLE scanning
- `stopScan()` - Stop scanning
- `scanResults` - Flow of scan results

### BleClient
- `connect()` - Connect to BLE device
- `disconnect()` - Disconnect
- `readCharacteristic()` - Read characteristic value
- `writeCharacteristic()` - Write characteristic value

## Requirements
- Android 8.0+ (API 26+)
- Bluetooth 4.0+ hardware
- Location permission (for Android 10+)

## License
[Apache 2.0](LICENSE.md)
