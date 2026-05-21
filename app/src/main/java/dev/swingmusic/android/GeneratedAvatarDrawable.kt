package dev.swingmusic.android

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import java.util.Locale
import kotlin.math.min

class GeneratedAvatarDrawable(
    private val name: String,
    private val accent: Int,
    private val ink: Int,
    private val surface: Int
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val seed = name.lowercase(Locale.US).fold(17) { acc, char -> acc * 31 + char.code }

    override fun draw(canvas: Canvas) {
        val area = bounds
        val size = min(area.width(), area.height()).toFloat()
        val cx = area.exactCenterX()
        val cy = area.exactCenterY()
        val radius = size / 2f

        paint.style = Paint.Style.FILL
        paint.alpha = 255
        paint.color = blend(surface, accent, 0.18f)
        canvas.drawCircle(cx, cy, radius, paint)

        paint.color = rotateColor(blend(accent, ink, 0.28f), seed)
        canvas.drawCircle(
            cx - size * wave(0, 0.12f, 0.22f),
            cy - size * wave(1, 0.04f, 0.20f),
            size * wave(2, 0.25f, 0.34f),
            paint
        )

        paint.color = blend(surface, ink, wave(3, 0.12f, 0.22f))
        canvas.drawOval(
            RectF(
                cx - size * wave(4, 0.14f, 0.28f),
                cy + size * wave(5, 0.03f, 0.14f),
                cx + size * wave(6, 0.28f, 0.42f),
                cy + size * wave(7, 0.31f, 0.46f)
            ),
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.075f
        paint.color = blend(ink, surface, 0.36f)
        canvas.drawCircle(
            cx + size * wave(8, 0.12f, 0.22f),
            cy - size * wave(9, 0.08f, 0.21f),
            size * wave(10, 0.20f, 0.31f),
            paint
        )
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun wave(index: Int, minValue: Float, maxValue: Float): Float {
        val shifted = seed ushr ((index % 4) * 8)
        val value = ((shifted + index * 97) and 0xFF) / 255f
        return minValue + (maxValue - minValue) * value
    }

    private fun rotateColor(color: Int, amount: Int): Int {
        val nudge = (amount and 0x1F) - 16
        return Color.rgb(
            (Color.red(color) + nudge).coerceIn(0, 255),
            (Color.green(color) - nudge / 2).coerceIn(0, 255),
            (Color.blue(color) + nudge / 3).coerceIn(0, 255)
        )
    }

    private fun blend(from: Int, to: Int, amount: Float): Int {
        val ratio = amount.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(from) + (Color.red(to) - Color.red(from)) * ratio).toInt(),
            (Color.green(from) + (Color.green(to) - Color.green(from)) * ratio).toInt(),
            (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * ratio).toInt()
        )
    }
}
