package dev.swingmusic.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class ColorPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var onColorChanged: ((Int) -> Unit)? = null

    private val hsv = floatArrayOf(0f, 0f, 1f)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }
    private val wheelRect = RectF()
    private val sliderRect = RectF()
    private var wheelBitmap: Bitmap? = null
    private var wheelCenterX = 0f
    private var wheelCenterY = 0f
    private var wheelRadius = 0f
    private var trackingSlider = false

    val selectedColor: Int
        get() = Color.HSVToColor(hsv)

    fun setColor(color: Int) {
        Color.colorToHSV(color, hsv)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minWidth = dp(260f).roundToInt()
        val width = resolveSize(minWidth, widthMeasureSpec)
        val wheelSize = max(dp(210f).roundToInt(), width - paddingLeft - paddingRight)
        val wantedHeight = paddingTop + wheelSize + dp(64f).roundToInt() + paddingBottom
        setMeasuredDimension(width, resolveSize(wantedHeight, heightMeasureSpec))
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        val availableWidth = width - paddingLeft - paddingRight
        val availableHeight = height - paddingTop - paddingBottom - dp(56f).roundToInt()
        val diameter = min(availableWidth, availableHeight).coerceAtLeast(dp(160f).roundToInt())
        wheelRadius = diameter / 2f
        wheelCenterX = width / 2f
        wheelCenterY = paddingTop + wheelRadius
        wheelRect.set(
            wheelCenterX - wheelRadius,
            wheelCenterY - wheelRadius,
            wheelCenterX + wheelRadius,
            wheelCenterY + wheelRadius
        )
        sliderRect.set(
            paddingLeft + dp(10f),
            wheelRect.bottom + dp(26f),
            width - paddingRight - dp(10f),
            wheelRect.bottom + dp(44f)
        )
        wheelBitmap = buildWheelBitmap((wheelRadius * 2).roundToInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        wheelBitmap?.let { canvas.drawBitmap(it, null, wheelRect, null) }

        if (hsv[2] < 1f) {
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(((1f - hsv[2]) * 210).roundToInt(), 0, 0, 0)
            canvas.drawCircle(wheelCenterX, wheelCenterY, wheelRadius, paint)
        }

        drawSelectionMarker(canvas)
        drawBrightnessSlider(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                trackingSlider = sliderRect.insetHit(event.x, event.y, dp(12f))
                updateFromTouch(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                updateFromTouch(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                updateFromTouch(event.x, event.y)
                trackingSlider = false
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return true
    }

    private fun updateFromTouch(x: Float, y: Float) {
        if (trackingSlider || sliderRect.insetHit(x, y, dp(8f))) {
            hsv[2] = ((x - sliderRect.left) / sliderRect.width()).coerceIn(0f, 1f)
        } else {
            val dx = x - wheelCenterX
            val dy = y - wheelCenterY
            val distance = hypot(dx, dy)
            if (distance <= wheelRadius * 1.08f) {
                hsv[0] = ((Math.toDegrees(atan2(dy, dx).toDouble()) + 360.0) % 360.0).toFloat()
                hsv[1] = (distance / wheelRadius).coerceIn(0f, 1f)
            }
        }
        onColorChanged?.invoke(selectedColor)
        invalidate()
    }

    private fun drawSelectionMarker(canvas: Canvas) {
        val angle = Math.toRadians(hsv[0].toDouble())
        val distance = hsv[1] * wheelRadius
        val x = wheelCenterX + cos(angle).toFloat() * distance
        val y = wheelCenterY + sin(angle).toFloat() * distance

        markerPaint.color = Color.BLACK
        markerPaint.strokeWidth = dp(5f)
        canvas.drawCircle(x, y, dp(8f), markerPaint)
        markerPaint.color = Color.WHITE
        markerPaint.strokeWidth = dp(2f)
        canvas.drawCircle(x, y, dp(8f), markerPaint)
    }

    private fun drawBrightnessSlider(canvas: Canvas) {
        val fullColor = Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], 1f))
        paint.shader = LinearGradient(
            sliderRect.left,
            0f,
            sliderRect.right,
            0f,
            Color.BLACK,
            fullColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(sliderRect, sliderRect.height() / 2f, sliderRect.height() / 2f, paint)
        paint.shader = null

        val knobX = sliderRect.left + sliderRect.width() * hsv[2]
        markerPaint.color = Color.BLACK
        markerPaint.strokeWidth = dp(5f)
        canvas.drawCircle(knobX, sliderRect.centerY(), dp(9f), markerPaint)
        markerPaint.color = Color.WHITE
        markerPaint.strokeWidth = dp(2f)
        canvas.drawCircle(knobX, sliderRect.centerY(), dp(9f), markerPaint)
    }

    private fun buildWheelBitmap(size: Int): Bitmap {
        val safeSize = size.coerceAtLeast(1)
        val pixels = IntArray(safeSize * safeSize)
        val radius = safeSize / 2f
        val center = radius
        for (y in 0 until safeSize) {
            for (x in 0 until safeSize) {
                val dx = x - center
                val dy = y - center
                val distance = hypot(dx, dy)
                pixels[y * safeSize + x] = if (distance <= radius) {
                    val hue = ((atan2(dy, dx) * 180f / PI.toFloat()) + 360f) % 360f
                    val saturation = (distance / radius).coerceIn(0f, 1f)
                    Color.HSVToColor(floatArrayOf(hue, saturation, 1f))
                } else {
                    Color.TRANSPARENT
                }
            }
        }
        return Bitmap.createBitmap(pixels, safeSize, safeSize, Bitmap.Config.ARGB_8888)
    }

    private fun RectF.insetHit(x: Float, y: Float, inset: Float): Boolean {
        return x >= left - inset && x <= right + inset && y >= top - inset && y <= bottom + inset
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
