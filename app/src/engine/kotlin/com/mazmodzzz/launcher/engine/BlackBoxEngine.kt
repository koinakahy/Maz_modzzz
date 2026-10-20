package com.mazmodzzz.launcher.engine

// Only compiled when the BlackBox "bcore" module sits in ./bcore (see README).
// Written from BlackBox's public README; class and method names may need small
// fixes for the fork you use. Paste any compile error to Claude and it can adjust.

import android.app.Application
import android.content.Context
import com.mazmodzzz.launcher.CloneEngine
import com.mazmodzzz.launcher.CloneEntry
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.app.configuration.ClientConfiguration

object BlackBoxEngine : CloneEngine {
    override val id = "virtual"

    override fun attach(base: Context) {
        BlackBoxCore.get().doAttachBaseContext(base, object : ClientConfiguration() {
            override fun getHostPackageName(): String = base.packageName
        })
    }

    override fun onCreate(app: Application) {
        BlackBoxCore.get().doCreate()
    }

    override fun launch(ctx: Context, e: CloneEntry): Boolean {
        val core = BlackBoxCore.get()
        if (!core.isInstalled(e.pkg, 0)) {
            val r = core.installPackageAsUser(e.pkg, 0)
            if (!r.success) return false
        }
        return core.launchApk(e.pkg, 0)
    }
}
