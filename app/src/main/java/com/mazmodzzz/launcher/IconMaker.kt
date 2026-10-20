package com.mazmodzzz.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap

object IconMaker {
    const val SIZE = 192

    fun appIcon(ctx: Context, pkg: String): Bitmap {
        return try {
            ctx.packageManager.getApplicationIcon(pkg).toBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).also { it.eraseColor(Color.DKGRAY) }
        }
    }

    fun centerCrop(src: Bitmap): Bitmap {
        val s = minOf(src.width, src.height)
        val x = (src.width - s) / 2
        val y = (src.height - s) / 2
        return Bitmap.createScaledBitmap(Bitmap.createBitmap(src, x, y, s, s), SIZE, SIZE, true)
    }

    fun decodeUri(ctx: Context, uri: Uri): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?.let { centerCrop(it) }
        } catch (e: Exception) {
            null
        }
    }

    fun loadCustom(ctx: Context, id: String): Bitmap? {
        val f = CloneStore.iconFile(ctx, id)
        return if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
    }

    /** Draws the base icon and, optionally, a coloured tag badge (like "d0") in the corner. */
    fun render(base: Bitmap, tag: String, accent: Int, badge: Boolean): Bitmap {
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawBitmap(base, null, Rect(0, 0, SIZE, SIZE), Paint(Paint.FILTER_BITMAP_FLAG))
        if (badge && tag.isNotBlank()) {
            val text = tag.take(3)
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.textSize = 40f
            p.typeface = Typeface.DEFAULT_BOLD
            val w = p.measureText(text)
            val padX = 14f
            val h = 56f
            val rect = RectF(SIZE - w - padX * 2 - 4f, SIZE - h - 4f, SIZE - 4f, SIZE - 4f)
            p.color = accent
            c.drawRoundRect(rect, 18f, 18f, p)
            p.color = Color.WHITE
            c.drawText(text, rect.left + padX, rect.bottom - 14f, p)
        }
        return out
    }

    fun forEntry(ctx: Context, e: CloneEntry): Bitmap {
        val base = (if (e.customIcon) loadCustom(ctx, e.id) else null) ?: appIcon(ctx, e.pkg)
        return render(base, e.tag, e.accent, e.badge)
    }
}
