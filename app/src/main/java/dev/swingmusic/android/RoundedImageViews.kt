package dev.swingmusic.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

open class RoundedImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    private val clipPath = Path()
    private val rect = RectF()
    protected open val cornerRadius: Float
        get() = 10f * resources.displayMetrics.density

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        rebuildPath(width, height)
    }

    override fun draw(canvas: Canvas) {
        val checkpoint = canvas.save()
        canvas.clipPath(clipPath)
        super.draw(canvas)
        canvas.restoreToCount(checkpoint)
    }

    private fun rebuildPath(width: Int, height: Int) {
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        clipPath.reset()
        clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
    }
}

class CircleImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RoundedImageView(context, attrs, defStyleAttr) {
    override val cornerRadius: Float
        get() = (kotlin.math.min(width, height) / 2f).coerceAtLeast(0f)
}
