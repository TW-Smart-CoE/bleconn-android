package com.thoughtworks.bleconn.utils

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