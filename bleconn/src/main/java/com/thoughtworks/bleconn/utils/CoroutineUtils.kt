package com.thoughtworks.bleconn.utils

import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

fun <T> Continuation<T>.silentResume(
    result: T,
    finallyAction: (() -> Unit)? = null,
) {
    try {
        resume(result)
    } catch (_: Throwable) {
        // Log or handle the exception if needed
    } finally {
        finallyAction?.invoke()
    }
}