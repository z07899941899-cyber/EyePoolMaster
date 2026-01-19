package com.eyepool.master

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class EyeFloatView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    // ألوان العين
    private val eyeWhite = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val eyeIris = Paint().apply {
        color = Color.parseColor("#4A148C") // بنفسجي غامق
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val eyePupil = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val eyeOutline = Paint().apply {
        color = Color.parseColor("#1A237E") // أزرق داكن
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }
    
    private val eyelidPaint = Paint().apply {
        color = Color.parseColor("#311B92") // بنفسجي داكن جداً
        style = Paint.Style.FILL
        isAntiAlias = true
        alpha = 200
    }
    
    private val eyelashesPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    
    // إحداثيات العين
    private var eyeCenterX = 0f
    private var eyeCenterY = 0f
    private var eyeRadius = 100f
    private var irisRadius = 40f
    private var pupilRadius = 15f
    
    // حركة القزحية
    private var irisOffsetX = 0f
    private var irisOffsetY = 0f
    private var pupilOffsetX = 0f
    private var pupilOffsetY = 0f
    
    // حركة الجفن
    private var eyelidPosition = 0f // 0: مفتوح بالكامل، 1: مغلق بالكامل
    private var isBlinking = false
    private var blinkProgress = 0f
    
    // معلومات التتبع
    private var isTracking = false
    private var targetX = 0f
    private var targetY = 0f
    
    // تأثيرات
    private var glowPaint = Paint().apply {
        color = Color.parseColor("#7C4DFF")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
        alpha = 100
    }
    
    // مستمع للتغيرات
    var onEyeClick: (() -> Unit)? = null
    var onEyeMove: ((x: Float, y: Float) -> Unit)? = null
    var onEyeTrack: ((targetX: Float, targetY: Float) -> Unit)? = null
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        eyeCenterX = w / 2f
        eyeCenterY = h / 2f
        eyeRadius = min(w, h) * 0.4f
        irisRadius = eyeRadius * 0.35f
        pupilRadius = irisRadius * 0.4f
        
        // بدء وميض عشوائي
        startRandomBlinking()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 1. رسم تأثير الإشعاع
        drawGlowEffect(canvas)
        
        // 2. رسم الجزء الأبيض من العين
        canvas.drawCircle(eyeCenterX, eyeCenterY, eyeRadius, eyeWhite)
        
        // 3. رسم الجفن
        drawEyelid(canvas)
        
        // 4. رسم القزحية (تتحرك حسب الهدف)
        val irisX = eyeCenterX + irisOffsetX
        val irisY = eyeCenterY + irisOffsetY
        canvas.drawCircle(irisX, irisY, irisRadius, eyeIris)
        
        // 5. رسم الحدود
        canvas.drawCircle(eyeCenterX, eyeCenterY, eyeRadius, eyeOutline)
        
        // 6. رسم البؤبؤ
        val pupilX = irisX + pupilOffsetX
        val pupilY = irisY + pupilOffsetY
        canvas.drawCircle(pupilX, pupilY, pupilRadius, eyePupil)
        
        // 7. رسم الرموش
        drawEyelashes(canvas)
        
        // 8. رسم الومضات
        drawGlints(canvas, pupilX, pupilY)
        
        // 9. إذا كان يتتبع، رسم خط التتبع
        if (isTracking) {
            drawTrackingLine(canvas, pupilX, pupilY)
        }
    }
    
    private fun drawGlowEffect(canvas: Canvas) {
        // تأثير إشعاع حول العين
        for (i in 1..5) {
            val radius = eyeRadius + i * 10
            glowPaint.alpha = (100 - i * 15).coerceAtLeast(10)
            canvas.drawCircle(eyeCenterX, eyeCenterY, radius, glowPaint)
        }
    }
    
    private fun drawEyelid(canvas: Canvas) {
        val currentEyelid = if (isBlinking) blinkProgress else eyelidPosition
        val eyelidHeight = eyeRadius * (1 - currentEyelid * 0.5f)
        
        // رسم شكل بيضاوي للجفن
        val eyelidRect = RectF(
            eyeCenterX - eyeRadius,
            eyeCenterY - eyeRadius,
            eyeCenterX + eyeRadius,
            eyeCenterY - eyeRadius + eyelidHeight * 2
        )
        
        canvas.drawOval(eyelidRect, eyelidPaint)
    }
    
    private fun drawEyelashes(canvas: Canvas) {
        val numLashes = 12
        val angleStep = 360f / numLashes
        
        for (i in 0 until numLashes) {
            val angle = Math.toRadians((i * angleStep).toDouble()).toFloat()
            val startX = eyeCenterX + cos(angle) * eyeRadius
            val startY = eyeCenterY + sin(angle) * eyeRadius
            
            val endX = startX + cos(angle) * 20f
            val endY = startY + sin(angle) * 20f
            
            canvas.drawLine(startX, startY, endX, endY, eyelashesPaint)
        }
    }
    
    private fun drawGlints(canvas: Canvas, pupilX: Float, pupilY: Float) {
        // ومضات صغيرة في العين
        val glintPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        
        // ومضة كبيرة
        canvas.drawCircle(
            pupilX - pupilRadius * 0.3f,
            pupilY - pupilRadius * 0.3f,
            pupilRadius * 0.4f,
            glintPaint
        )
        
        // ومضة صغيرة
        canvas.drawCircle(
            pupilX + pupilRadius * 0.2f,
            pupilY - pupilRadius * 0.2f,
            pupilRadius * 0.2f,
            glintPaint
        )
    }
    
    private fun drawTrackingLine(canvas: Canvas, fromX: Float, fromY: Float) {
        // خط من البؤبؤ إلى الهدف
        val linePaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 3f
            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
            alpha = 150
        }
        
        canvas.drawLine(fromX, fromY, targetX, targetY, linePaint)
        
        // دائرة حول الهدف
        canvas.drawCircle(targetX, targetY, 15f, 
            Paint().apply {
                color = Color.RED
                style = Paint.Style.STROKE
                strokeWidth = 2f
            })
    }
    
    fun trackTarget(x: Float, y: Float) {
        isTracking = true
        targetX = x
        targetY = y
        
        // تحريك القزحية نحو الهدف
        val dx = x - eyeCenterX
        val dy = y - eyeCenterY
        val distance = sqrt(dx * dx + dy * dy)
        
        val maxOffset = eyeRadius - irisRadius
        val scale = min(1f, maxOffset / max(distance, 1f))
        
        irisOffsetX = dx * scale * 0.5f
        irisOffsetY = dy * scale * 0.5f
        
        // تحريك البؤبؤ داخل القزحية
        val irisCenterX = eyeCenterX + irisOffsetX
        val irisCenterY = eyeCenterY + irisOffsetY
        val pupilDx = x - irisCenterX
        val pupilDy = y - irisCenterY
        val pupilDistance = sqrt(pupilDx * pupilDx + pupilDy * pupilDy)
        
        val pupilMaxOffset = irisRadius - pupilRadius
        val pupilScale = min(1f, pupilMaxOffset / max(pupilDistance, 1f))
        
        pupilOffsetX = pupilDx * pupilScale * 0.3f
        pupilOffsetY = pupilDy * pupilScale * 0.3f
        
        invalidate()
        
        // إرسال إحداثيات التتبع
        onEyeTrack?.invoke(x, y)
    }
    
    fun stopTracking() {
        isTracking = false
        // إعادة القزحية إلى المركز ببطء
        irisOffsetX *= 0.9f
        irisOffsetY *= 0.9f
        pupilOffsetX *= 0.9f
        pupilOffsetY *= 0.9f
        
        if (abs(irisOffsetX) < 0.1f && abs(irisOffsetY) < 0.1f) {
            irisOffsetX = 0f
            irisOffsetY = 0f
            pupilOffsetX = 0f
            pupilOffsetY = 0f
        }
        
        invalidate()
    }
    
    private fun startRandomBlinking() {
        // وميض عشوائي كل 3-8 ثواني
        postDelayed({
            blink()
            startRandomBlinking()
        }, (3000 + Math.random() * 5000).toLong())
    }
    
    fun blink() {
        isBlinking = true
        blinkProgress = 0f
        
        val blinkAnimator = object {
            fun update() {
                if (isBlinking) {
                    blinkProgress += 0.1f
                    if (blinkProgress >= 1f) {
                        blinkProgress = 1f
                        isBlinking = false
                        // إعادة فتح العين
                        postDelayed({
                            eyelidPosition = 0f
                            invalidate()
                        }, 50)
                    } else {
                        eyelidPosition = blinkProgress
                        invalidate()
                        postDelayed({ update() }, 16) // ~60fps
                    }
                }
            }
        }
        
        blinkAnimator.update()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // التأكد أن اللمس داخل العين
                val dx = event.x - eyeCenterX
                val dy = event.y - eyeCenterY
                val distance = sqrt(dx * dx + dy * dy)
                
                if (distance <= eyeRadius) {
                    // نظرة سريعة نحو نقطة اللمس
                    trackTarget(event.x, event.y)
                    
                    // تأثير نقر
                    performClick()
                    return true
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isTracking) {
                    trackTarget(event.x, event.y)
                    onEyeMove?.invoke(event.x, event.y)
                }
            }
            
            MotionEvent.ACTION_UP -> {
                stopTracking()
                onEyeClick?.invoke()
            }
        }
        return true
    }
    
    override fun performClick(): Boolean {
        super.performClick()
        // وميض عند النقر
        blink()
        return true
    }
    
    // وظائف للتحكم من الخارج
    fun setEyeColor(color: Int) {
        eyeIris.color = color
        invalidate()
    }
    
    fun setEyeSize(size: Float) {
        eyeRadius = size
        irisRadius = eyeRadius * 0.35f
        pupilRadius = irisRadius * 0.4f
        invalidate()
    }
    
    fun setEyelidOpenness(openness: Float) {
        eyelidPosition = (1f - openness).coerceIn(0f, 1f)
        invalidate()
    }
}