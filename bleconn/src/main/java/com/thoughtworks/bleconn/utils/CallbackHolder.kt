package com.thoughtworks.bleconn.utils

import java.util.concurrent.ConcurrentHashMap

class CallbackHolder<T> {
    private var callback: ((T) -> Unit)? = null

    fun isSet(): Boolean {
        return callback != null
    }

    fun set(callback: ((T) -> Unit)) {
        this.callback = callback
    }

    fun resolve(result: T) {
        callback?.invoke(result)
        callback = null
    }
}

class KeyCallbackHolder<K, T> {
    private var key: K? = null
    private var callback: ((T) -> Unit)? = null

    fun isSet(): Boolean {
        return callback != null && key != null
    }

    fun set(key: K, callback: ((T) -> Unit)) {
        this.key = key
        this.callback = callback
    }

    fun getKey(): K? {
        return key
    }

    fun resolve(result: T) {
        callback?.invoke(result)
        callback = null
        key = null
    }
}

class EnableNotificationCallbackHolder<K, T, N> {
    private var key: K? = null
    private var callback: ((T) -> Unit)? = null
    private var onNotificationDataHandler: ((N) -> Unit)? = null

    fun isSet(): Boolean {
        return key != null && callback != null && onNotificationDataHandler != null
    }

    fun set(key: K, callback: ((T) -> Unit), onNotificationData: ((N) -> Unit)) {
        this.key = key
        this.callback = callback
        this.onNotificationDataHandler = onNotificationData
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
    }
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