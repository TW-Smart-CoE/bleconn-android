package com.thoughtworks.bleconn.utils

import java.util.concurrent.ConcurrentHashMap

class CallbackHolder<T>(
    private val timeout: Int = 0,
) {
    private var callback: ((T) -> Unit)? = null
    private var setTime: Long = 0L

    fun isSet(): Boolean {
        val isSet = callback != null
        return if (timeout > 0) {
            isSet && (System.currentTimeMillis() - setTime < timeout)
        } else {
            isSet
        }
    }

    fun set(callback: ((T) -> Unit)) {
        this.callback = callback
        this.setTime = System.currentTimeMillis()
    }

    fun resolve(result: T) {
        callback?.invoke(result)
        callback = null
    }

    fun isTimeout() =
        callback != null && timeout > 0 && System.currentTimeMillis() - setTime > timeout
}

class KeyCallbackHolder<K, T>(
    private val timeout: Int = 0,
) {
    private var key: K? = null
    private var callback: ((T) -> Unit)? = null
    private var setTime: Long = 0L

    fun isSet(): Boolean {
        val isSet = callback != null && key != null
        return if (timeout > 0) {
            isSet && (System.currentTimeMillis() - setTime < timeout)
        } else {
            isSet
        }
    }

    fun set(key: K, callback: ((T) -> Unit)) {
        this.key = key
        this.callback = callback
        this.setTime = System.currentTimeMillis()
    }

    fun getKey(): K? {
        return key
    }

    fun resolve(result: T) {
        callback?.invoke(result)
        callback = null
        key = null
        setTime = 0L
    }

    fun isTimeout() =
        callback != null && timeout > 0 && System.currentTimeMillis() - setTime > timeout
}

class EnableNotificationCallbackHolder<K, T, N>(
    private val timeout: Int = 0,
) {
    private var key: K? = null
    private var callback: ((T) -> Unit)? = null
    private var onNotificationDataHandler: ((N) -> Unit)? = null
    private var setTime: Long = 0L

    fun isSet(): Boolean {
        val isSet = key != null && callback != null && onNotificationDataHandler != null
        return if (timeout > 0) {
            isSet && (System.currentTimeMillis() - setTime < timeout)
        } else {
            isSet
        }
    }

    fun set(key: K, callback: ((T) -> Unit), onNotificationData: ((N) -> Unit)) {
        this.key = key
        this.callback = callback
        this.onNotificationDataHandler = onNotificationData
        this.setTime = System.currentTimeMillis()
    }

    fun getKey(): K? {
        return key
    }

    fun getOnNotificationDataHandler(): ((N) -> Unit)? {
        return onNotificationDataHandler
    }

    fun resolve(result: T) {
        callback?.invoke(result)
        callback = null
        onNotificationDataHandler = null
        setTime = 0L
    }

    fun isTimeout() =
        callback != null && timeout > 0 && System.currentTimeMillis() - setTime > timeout
}

class NotificationHolder<K, V> {
    private val callbacks = ConcurrentHashMap<K, (V) -> Unit>()

    fun contains(key: K): Boolean {
        return callbacks.containsKey(key)
    }

    fun add(key: K, callback: (V) -> Unit) {
        callbacks[key] = callback
    }

    fun remove(key: K) {
        callbacks.remove(key)
    }

    fun clear() {
        callbacks.clear()
    }

    fun notify(key: K, result: V) {
        callbacks[key]?.invoke(result)
    }
}