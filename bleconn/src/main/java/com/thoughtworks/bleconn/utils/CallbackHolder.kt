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

class NotificationHolder<T> {
    private val callbacks = ConcurrentHashMap<String, (T) -> Unit>()

    fun contains(characteristicUUID: String): Boolean {
        return callbacks.containsKey(characteristicUUID)
    }

    fun add(characteristicUUID: String, callback: (T) -> Unit) {
        callbacks[characteristicUUID] = callback
    }

    fun remove(characteristicUUID: String) {
        callbacks.remove(characteristicUUID)
    }

    fun clear() {
        callbacks.clear()
    }

    fun notify(characteristicUUID: String, result: T) {
        callbacks[characteristicUUID]?.invoke(result)
    }
}