package com.marozzi.multicircleprogressview

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.core.content.ContextCompat
import com.marozzi.multicircleprogressview.MultiCircleProgressView.TextPosition.*


class MultiCircleProgressView : View {

    /**
     * Indicate the text position inside the view
     */
    enum class TextPosition {
        Top,
        Center,
        Bottom
    }

    /**
     * Array of text
     */
    private val texts: Array<Text> = arrayOf(Text(Top), Text(Center), Text(Bottom))

    /**
     * Array of circle, every circle has own draw method and settings
     */
    private var circles: Array<Circle> = arrayOf()

    /**
     * Set the current progress of the circles, the range is from 0 to 100
     */
    var progress: Int = 0
        private set

    /**
     * Padding between the two circles
     */
    private var circlePadding: Float = 0f

    /**
     * Actual size of the complete circle.
     */
    private var circleSize: Int = 0

    private var circleProgressColor: Int = 0
    private var circleGuideColor: Int = 0
    private var circleThickness: Float = 0f

    /**
     * Starting Angle to start the progress Animation.
     */
    var circleStartAngle: Float = 0f

    /**
     * True if start the progress from the inside, false from the outside
     */
    var reversed: Boolean = false

    var enableValueIndicator: Boolean = true
        set(value) {
            field = value
            texts.find { it.textPosition == Center }?.text = ""
            invalidate()
        }

    /**
     * Animation duration
     */
    var animationDuration: Long = 0

    /**
     * Progress Animation set
     */
    private var animator: AnimatorSet? = null

    constructor(context: Context?) : super(context) {
        init(null, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs, defStyleAttr)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        init(attrs, defStyleAttr)
    }


    /**
     * Initialize all drawing parameters from the custom Attributes.
     */
    private fun init(attrs: AttributeSet?, defStyle: Int) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.MultiCircleProgressView, defStyle, 0)

        circleThickness = a.getDimensionPixelSize(
            R.styleable.MultiCircleProgressView_mcp_circle_thickness,
            10
        ).toFloat()

        circleProgressColor = a.getColor(
            R.styleable.MultiCircleProgressView_mcp_circle_progress_color,
            ContextCompat.getColor(context, R.color.mcp_circle_progress_color)
        )

        circleGuideColor = a.getColor(
            R.styleable.MultiCircleProgressView_mcp_circle_guide_color,
            ContextCompat.getColor(context, R.color.mcp_circle_guide_color)
        )

        circlePadding = a.getDimensionPixelSize(
            R.styleable.MultiCircleProgressView_mcp_circle_padding,
            0
        ).toFloat()

        animationDuration = a.getInteger(
            R.styleable.MultiCircleProgressView_mcp_anim_duration,
            1000
        ).toLong()

        circleStartAngle = a.getFloat(R.styleable.MultiCircleProgressView_mcp_circle_start_angle, 0f)

        reversed = a.getBoolean(R.styleable.MultiCircleProgressView_mcp_reversed, false)

        enableValueIndicator = a.getBoolean(R.styleable.MultiCircleProgressView_mcp_enable_value_indicator, true)

        val topIndicator = a.getString(R.styleable.MultiCircleProgressView_mcp_text_top_indicator)
        val bottomIndicator = a.getString(R.styleable.MultiCircleProgressView_mcp_text_bottom_indicator)
        setTextIndicator(topIndicator ?: "", bottomIndicator ?: "")

        val circleNumber = a.getInteger(R.styleable.MultiCircleProgressView_mcp_circle_number, 1)
        setCircleNumber(circleNumber)

        progress = a.getInteger(R.styleable.MultiCircleProgressView_mcp_progress, 0)
        if (progress > 0)
            resetAnimation()

        a.recycle()

        texts.forEach {
            when (it.textPosition) {
                Top -> it.setTextSize(
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        13f,
                        resources.displayMetrics
                    )
                )
                Center -> it.setTextSize(
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        42f,
                        resources.displayMetrics
                    )
                )
                Bottom -> it.setTextSize(
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        11f,
                        resources.displayMetrics
                    )
                )
            }
        }
    }

    fun setCircleNumber(num: Int) {
        val tmpCircles = mutableListOf<Circle>()
        for (i in 0 until num) {
            tmpCircles.add(Circle(circleProgressColor, circleGuideColor, circleThickness))
        }
        circles = tmpCircles.toTypedArray()
    }

    fun setProgress(@IntRange(from = 0, to = 100) progress: Int) {
        setProgress(progress, progress)
    }

    fun setProgress(@IntRange(from = 0, to = 100) progress: Int, value: Int) {
        val oldProgress = this.progress
        this.progress = progress
        resetAnimation(oldProgress, value)
    }

    fun setTextIndicator(top: String, bottom: String) {
        texts.forEach {
            if (it.textPosition == Top)
                it.text = top
            else if (it.textPosition == Bottom)
                it.text = bottom
        }
        invalidate()
    }

    fun setTextProperty(@ColorInt color: Int, typeface: Typeface, textPosition: TextPosition) {
        texts.forEach {
            if (it.textPosition == textPosition) {
                it.setTextColor(color)
                it.setTextTypeface(typeface)
            }
        }
        invalidate()
    }

//    val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//        color = Color.BLUE
//        style = Paint.Style.STROKE
//        strokeWidth = 5f
//        strokeCap = Paint.Cap.BUTT
//    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        circles.forEach {
            it.onDraw(canvas)
        }
        texts.forEach {
            it.onDraw(canvas)
        }
//        val width = width - paddingLeft.toFloat()
//        val height = width - paddingTop.toFloat()
//        canvas.drawLine(width / 2, top, width / 2, bottom, paintLine)
//        canvas.drawLine(left, height / 2, right, height / 2, paintLine)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val xPad = paddingLeft + paddingRight
        val yPad = paddingTop + paddingBottom
        val width = measuredWidth - xPad
        val height = measuredHeight - yPad
        circleSize = if (width < height) width else height
        setMeasuredDimension(circleSize + xPad, circleSize + yPad)
        updateRectAngleBounds()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        circleSize = if (w < h) w else h
        updateRectAngleBounds()
    }

    /**
     * Set rectangle bounds for drawing two circles.
     */
    private fun updateRectAngleBounds() {
        var innerPadding = 0f
        circles.forEach {
            it.updateRectAngleBounds(innerPadding)
            innerPadding += circlePadding
        }

        val left = paddingLeft.toFloat() + innerPadding * 1.5f
        val right = width - innerPadding * 1.5f

        val top = paddingTop.toFloat() + innerPadding * 1.25f
        val bottom = width - innerPadding * 1.25f

        Log.d("MultiCircleProgressView", "top $top bottom $bottom bottom/3 ${bottom / 3f}")

        //TOP
        val rectTop = RectF(
            left,
            top,
            right,
            top + ((bottom / 3f) / 1.5f)
        )
        Log.d("MultiCircleProgressView", "  rectTop $rectTop")

        //CENTER
        val rectCenter = RectF(
            left,
            rectTop.bottom,
            right,
            rectTop.bottom + rectTop.height()
        )
        Log.d("MultiCircleProgressView", "  rectCenter $rectCenter")

        //BOTTOM
        val rectBottom = RectF(
            left,
            rectCenter.bottom,
            right,
            rectCenter.bottom + rectCenter.height()
        )
        Log.d("MultiCircleProgressView", "  rectBottom $rectBottom")

        texts.forEach {
            when (it.textPosition) {
                Top -> it.rect = rectTop
                Center -> it.rect = rectCenter
                Bottom -> it.rect = rectBottom
            }

        }
    }

    override fun setVisibility(visibility: Int) {
        val currentVisibility = getVisibility()
        super.setVisibility(visibility)
        if (visibility != currentVisibility) {
            if (visibility == View.VISIBLE) {
                resetAnimation()
            } else if (visibility == View.GONE || visibility == View.INVISIBLE) {
                stopAnimation()
            }
        }
    }

    /**
     * Stops the animation
     */
    private fun stopAnimation() {
        animator?.run {
            if (isRunning)
                cancel()
            removeAllListeners()
        }
        animator = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        resetAnimation()
    }

    /**
     * Reset the animation from the old progress to the new one
     * @return the duration of the animations
     */
    private fun resetAnimation(oldProgress: Int = 0, valueProgress: Int = 0) {
        stopAnimation()

        // Build the whole AnimatorSet
        animator = AnimatorSet()
        var prevSet: Animator? = null
        var nextSet: Animator?

        val circleDuration = animationDuration / circles.size
        val singleCircleMaxProgress = 100 / circles.size
        var currentCircleProgress = 0f

        var animDurationResult = 0L

        Log.d(
            "MultiCircleProgressView",
            "-------------------------- progress $progress oldProgress $oldProgress -------------------------- "
        )

        val circleTmp = if (reversed) circles.reversedArray() else circles
        circleTmp.forEach { circle ->
            Log.d(
                "MultiCircleProgressView",
                "step $currentCircleProgress"
            )
            val circleProgress = if (progress >= singleCircleMaxProgress + currentCircleProgress) {
                100f
            } else {
                val diff = progress - currentCircleProgress
                if (diff > 0) (diff / singleCircleMaxProgress) * 100 else 0f
            }
            Log.d(
                "MultiCircleProgressView",
                "   fromProgress ${circle.progress}% toProgress $circleProgress%"
            )

            if (progress > oldProgress) { //increment
                nextSet = when {
                    circle.progress == 100f -> {
                        animDurationResult += 0L
                        createCircleAnimator(circle, circle.progress, circleProgress, 0L)
                    }
                    circle.progress > 0f -> {
                        val diff = circleProgress - circle.progress
                        val diffDuration = (circleDuration - (diff * (circleDuration / 100))).toLong()
                        Log.d(
                            "MultiCircleProgressView",
                            "   diff $diff duration $diffDuration"
                        )
                        animDurationResult += diffDuration
                        createCircleAnimator(circle, circle.progress, circleProgress, diffDuration)
                    }
                    else -> {
                        animDurationResult += circleDuration
                        createCircleAnimator(circle, 0f, circleProgress, circleDuration)
                    }
                }

                val builder = animator!!.play(nextSet)
                prevSet?.let {
                    builder.after(it)
                }
                prevSet = nextSet
            } else { //decrement
                nextSet = if (circleProgress == 0f) {
                    val duration = (circle.progress * (circleDuration / 100)).toLong()
                    Log.d(
                        "MultiCircleProgressView",
                        "   duration $duration"
                    )
                    animDurationResult += duration
                    createCircleAnimator(circle, circle.progress, 0f, duration)
                } else {
                    val diff = circleProgress - circle.progress
                    val duration = (circleDuration - (diff * (circleDuration / 100))).toLong()
                    Log.d(
                        "MultiCircleProgressView",
                        "   diff $diff duration $duration"
                    )
                    animDurationResult += duration
                    createCircleAnimator(circle, circle.progress, circleProgress, duration)
                }

                val builder = animator!!.play(nextSet)
                prevSet?.let {
                    builder.before(it)
                }
                prevSet = nextSet
            }

            currentCircleProgress += singleCircleMaxProgress
        }

        if (enableValueIndicator)
            texts.find { it.textPosition == Center }?.let {
                animator!!.play(createTextAnimator(it, it.text.toIntOrNull() ?: 0, valueProgress, animationDuration))
            }

        animator!!.start()
    }

    /**
     * Create the Circle Progress Animation sequence
     */
    private fun createCircleAnimator(
        circle: Circle,
        from: Float = 0f,
        to: Float,
        duration: Long
    ): Animator {
        val valueAnimator = ValueAnimator.ofFloat(from, to)
        valueAnimator.duration = duration
        valueAnimator.interpolator = LinearInterpolator()
        valueAnimator.addUpdateListener { animation ->
            circle.progress = animation.animatedValue as Float
            invalidate()
        }
        return valueAnimator
    }

    /**
     * Create the Circle Progress Animation sequence
     */
    private fun createTextAnimator(
        text: Text,
        from: Int = 0,
        to: Int,
        duration: Long
    ): Animator {
        val valueAnimator = ValueAnimator.ofInt(from, to)
        valueAnimator.duration = duration
        valueAnimator.interpolator = LinearInterpolator()
        valueAnimator.addUpdateListener { animation ->
            text.text = animation.animatedValue.toString()
            invalidate()
        }
        return valueAnimator
    }

    private inner class Circle(@ColorInt colorProgress: Int, @ColorInt colorGuide: Int, val width: Float) {

        /**
         * Draw the progress
         */
        private var circleProgressPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

        /**
         * Draw the guide
         */
        private var circleGuidePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

        /**
         * Rect for drawing the circle
         */
        private var circleRect: RectF = RectF()

        /**
         * Current progress of this circle
         */
        var progress: Float = 0f

        init {
            circleProgressPaint.color = colorProgress
            circleProgressPaint.style = Paint.Style.STROKE
            circleProgressPaint.strokeWidth = width
            circleProgressPaint.strokeCap = Paint.Cap.ROUND

            circleGuidePaint.color = colorGuide
            circleGuidePaint.style = Paint.Style.STROKE
            circleGuidePaint.strokeWidth = width / 4
            circleGuidePaint.strokeCap = Paint.Cap.BUTT
        }

        fun onDraw(canvas: Canvas) {
            canvas.drawCircle(
                circleRect.centerX(),
                circleRect.centerY(),
                circleRect.width() / 2,
                circleGuidePaint
            )

            val value = (progress / 100f) * 360f
            canvas.drawArc(
                circleRect,
                circleStartAngle,
                value,
                false,
                circleProgressPaint
            )
        }

        /**
         * Set bounds for drawing circle.
         */
        fun updateRectAngleBounds(otherPadding: Float) {
            val paddingLeft = paddingLeft
            val paddingTop = paddingTop
            circleRect.set(
                paddingLeft.toFloat() + width + otherPadding,
                paddingTop.toFloat() + width + otherPadding,
                circleSize.toFloat() - paddingLeft.toFloat() - width - otherPadding,
                circleSize.toFloat() - paddingTop.toFloat() - width - otherPadding
            )
        }
    }

    private inner class Text(val textPosition: TextPosition) {

        private var paint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

        /**
         * Rect for drawing the text inside
         */
        var rect: RectF = RectF()

        var text: String = ""

        init {
            paint.textSize = 16f
            paint.color = Color.BLACK
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        fun setTextColor(@ColorInt color: Int) {
            paint.color = color
        }

        fun setTextTypeface(typeface: Typeface) {
            paint.typeface = typeface
        }

        fun setTextSize(textSize: Float) {
            paint.textSize = textSize
        }

        fun onDraw(canvas: Canvas) {
            //left and right
//            canvas.drawLine(rect.left, rect.top, rect.left, rect.bottom, paint)
//            canvas.drawLine(rect.right, rect.top, rect.right, rect.bottom, paint)

            //top and bottom
//            canvas.drawLine(rect.left, rect.top, rect.right, rect.top, paint)
//            canvas.drawLine(rect.left, rect.bottom, rect.right, rect.bottom, paint)

            //text
            when (textPosition) {
                Top -> canvas.drawText(text, rect.centerX(), rect.top + (rect.bottom / 3), paint)
                Center -> canvas.drawText(text, rect.centerX(), rect.bottom, paint)
                Bottom -> canvas.drawText(text, rect.centerX(), rect.centerY(), paint)
            }
        }
    }
}