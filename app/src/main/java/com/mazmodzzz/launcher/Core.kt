package com.mazmodzzz.launcher

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** One entry in the launcher list. Shown label looks like "[d0] Facebook". */
data class CloneEntry(
    val id: String,
    val pkg: String,
    val name: String,
    val tag: String,
    val customIcon: Boolean,
    val accent: Int,
    val badge: Boolean,
    val mode: String // "shortcut" or "virtual"
) {
    val label: String get() = if (tag.isBlank()) name else "[$tag] $name"
}

object CloneStore {
    private fun prefs(c: Context) = c.getSharedPreferences("clones", Context.MODE_PRIVATE)

    fun load(c: Context): List<CloneEntry> {
        return try {
            val arr = JSONArray(prefs(c).getString("list", "[]"))
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                CloneEntry(
                    id = o.getString("id"),
                    pkg = o.getString("pkg"),
                    name = o.getString("name"),
                    tag = o.optString("tag", ""),
                    customIcon = o.optBoolean("customIcon", false),
                    accent = o.optInt("accent", 0xFF00E5FF.toInt()),
                    badge = o.optBoolean("badge", true),
                    mode = o.optString("mode", "shortcut")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(c: Context, list: List<CloneEntry>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id).put("pkg", it.pkg).put("name", it.name)
                    .put("tag", it.tag).put("customIcon", it.customIcon)
                    .put("accent", it.accent).put("badge", it.badge).put("mode", it.mode)
            )
        }
        prefs(c).edit().putString("list", arr.toString()).apply()
    }

    fun iconFile(c: Context, id: String): File {
        val dir = File(c.filesDir, "icons")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$id.png")
    }
}

/** An engine knows how to open an entry. */
interface CloneEngine {
    val id: String
    fun attach(base: Context) {}
    fun onCreate(app: Application) {}
    fun launch(ctx: Context, e: CloneEntry): Boolean
}

object Engines {
    /** Found by name at runtime, so the app still builds when the virtual engine is absent. */
    val virtual: CloneEngine? by lazy {
        try {
            val cls = Class.forName("com.mazmodzzz.launcher.engine.BlackBoxEngine")
            cls.getDeclaredField("INSTANCE").get(null) as? CloneEngine
        } catch (t: Throwable) {
            null
        }
    }
}

/** Always works: opens the real app, and can pin a home-screen shortcut with your name + icon. */
object ShortcutEngine : CloneEngine {
    override val id = "shortcut"

    override fun launch(ctx: Context, e: CloneEntry): Boolean {
        val i = ctx.packageManager.getLaunchIntentForPackage(e.pkg) ?: return false
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
        return true
    }

    fun pin(ctx: Context, e: CloneEntry): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(ctx)) return false
        val i = ctx.packageManager.getLaunchIntentForPackage(e.pkg) ?: return false
        i.action = Intent.ACTION_MAIN
        val sc = ShortcutInfoCompat.Builder(ctx, "mz_${e.id}")
            .setShortLabel(e.label)
            .setLongLabel(e.label)
            .setIcon(IconCompat.createWithBitmap(IconMaker.forEntry(ctx, e)))
            .setIntent(i)
            .build()
        return ShortcutManagerCompat.requestPinShortcut(ctx, sc, null)
    }
}
