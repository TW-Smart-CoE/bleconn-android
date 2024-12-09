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