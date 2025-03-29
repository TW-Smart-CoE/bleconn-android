# BleConn - Android BLE 连接库

## 概述
BleConn 是一个简化 Android 蓝牙低功耗(BLE)操作的库，包括广播、扫描、客户端和服务端功能。它提供了基于协程的 API，便于与现代 Android 应用集成。

## 库组件

### 核心功能
- **BleAdvertiser**: 启动/停止 BLE 广播，可配置参数
- **BleScanner**: 扫描附近的 BLE 设备，支持过滤器
- **BleClient**: 连接 BLE 设备并执行操作
- **BleServer**: 创建 BLE 服务和特征，作为外围设备

### 实用工具
- 蓝牙权限处理
- GATT 操作工具
- 协程调度器
- 日志系统

## 示例应用功能
包含的示例应用演示了:
- BLE 设备扫描和连接
- BLE 广播
- 客户端操作(读写特征值)
- 服务端操作(创建服务/特征)
- 性能测试

## 快速开始

### 安装
将库添加到您的项目:

```kotlin
dependencies {
    implementation("com.github.TW-Smart-CoE:bleconn-android:Tag")
}
```

### 基本用法

```kotlin
// 初始化扫描器
val scanner = BleScanner(context)

// 开始扫描
scanner.startScan { device ->
    // 处理发现的设备
}

// 初始化客户端
val client = BleClient(context)

// 连接设备
client.connect(deviceAddress) { connectionState ->
    // 处理连接状态变化
}
```

## API 参考

### BleScanner
- `startScan()` - 开始 BLE 扫描
- `stopScan()` - 停止扫描
- `scanResults` - 扫描结果流

### BleClient
- `connect()` - 连接 BLE 设备
- `disconnect()` - 断开连接
- `readCharacteristic()` - 读取特征值
- `writeCharacteristic()` - 写入特征值

## 要求
- Android 8.0+ (API 26+)
- 蓝牙 4.0+ 硬件
- 位置权限(Android 10+ 需要)

## 许可证
[Apache 2.0](LICENSE.md)
