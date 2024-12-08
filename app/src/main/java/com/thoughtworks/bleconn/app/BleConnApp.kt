package com.thoughtworks.bleconn.app

import android.app.Application
import com.thoughtworks.bleconn.app.di.Dependency
import com.thoughtworks.bleconn.app.di.DependencyImpl

class BleConnApp : Application() {
    private lateinit var dependency: DependencyImpl

    override fun onCreate() {
        super.onCreate()
        dependency = DependencyImpl(this)
    }

    fun getDependency(): Dependency {
        return dependency
    }
}