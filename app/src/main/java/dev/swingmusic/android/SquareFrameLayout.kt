package dev.swingmusic.android

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import kotlin.math.roundToInt

class SquareFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val heightLimit = MeasureSpec.getSize(heightMeasureSpec).takeIf { it > 0 } ?: measuredWidth
        val size = minOf(measuredWidth, (heightLimit * 0.48f).roundToInt())
        setMeasuredDimension(size, size)
        val exact = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        super.onMeasure(exact, exact)
    }
}
