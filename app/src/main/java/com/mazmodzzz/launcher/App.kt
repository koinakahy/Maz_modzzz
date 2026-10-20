package com.mazmodzzz.launcher

import android.app.Application
import android.content.Context

class App : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        try { Engines.virtual?.attach(base) } catch (t: Throwable) { }
    }

    override fun onCreate() {
        super.onCreate()
        try { Engines.virtual?.onCreate(this) } catch (t: Throwable) { }
    }
}
