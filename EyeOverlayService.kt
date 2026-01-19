package com.eyepool.master

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import com.eyepool.master.R
import com.eyepool.master.views.EyeFloatView
import kotlinx.coroutines.*

class EyeOverlayService : Service() {
    
    private lateinit var windowManager: WindowManager
    private lateinit var eyeView: EyeFloatView
    private var isServiceRunning = false
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    companion object {
        var isRunning = false
        var smartLinesEnabled = false
        var autoShotEnabled = true
        
        fun enableSmartLines() { smartLinesEnabled = true }
        fun disableSmartLines() { smartLinesEnabled = false }
        fun enableAutoShot() { autoShotEnabled = true }
        fun disableAutoShot() { autoShotEnabled = false }
    }
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_MANAGER_SERVICE) as WindowManager
        setupEyeOverlay()
        isRunning = true
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra("mode")) {
            "FLOATING_EYE" -> {
                showFloatingEye()
                startAutoServices()
            }
            "SMART_EYE" -> {
                showSmartEye()
                startSmartTracking()
            }
        }
        return START_STICKY
    }
    
    private fun setupEyeOverlay() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        eyeView = EyeFloatView(this)
        
        // إعدادات النافذة العائمة
        val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
        }
        
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 0
        params.y = 100
        
        // إعداد تفاعلات العين
        eyeView.onEyeClick = {
            // عند النقر على العين
            if (autoShotEnabled) {
                executeAutoShot()
            }
        }
        
        eyeView.onEyeTrack = { x, y ->
            // عند تتبع هدف
            trackTarget(x, y)
        }
    }
    
    private fun showFloatingEye() {
        try {
            windowManager.addView(eyeView, getLayoutParams())
            isServiceRunning = true
            
            // بدء قراءة الشاشة
            startScreenReading()
            
            Toast.makeText(this, "👁️ العين الذكية نشطة", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun showSmartEye() {
        // وضع ذكي مع ميزات إضافية
        showFloatingEye()
        eyeView.setEyeSize(150f)
    }
    
    private fun startAutoServices() {
        serviceScope.launch {
            // 1. خدمة التتبع التلقائي
            launch { startAutoTracking() }
            
            // 2. خدمة قراءة الشاشة
            launch { startScreenAnalysis() }
            
            // 3. خدمة الضرب التلقائي
            if (autoShotEnabled) {
                launch { startAutoAiming() }
            }
        }
    }
    
    private suspend fun startAutoTracking() {
        while (isServiceRunning) {
            // تتبع الكرات تلقائياً
            val balls = BallTracker.detectBalls()
            if (balls.isNotEmpty()) {
                val bestBall = balls.first() // أول كرة
                eyeView.trackTarget(bestBall.x, bestBall.y)
            }
            
            delay(500) // تحديث كل نصف ثانية
        }
    }
    
    private suspend fun startScreenAnalysis() {
        while (isServiceRunning) {
            // قراءة وتحليل الشاشة
            ScreenReaderService.analyzeScreen()
            delay(1000) // كل ثانية
        }
    }
    
    private suspend fun startAutoAiming() {
        while (isServiceRunning && autoShotEnabled) {
            // البحث عن أفضل ضربة
            val bestShot = findBestShot()
            bestShot?.let {
                // توجيه العين نحو الهدف
                eyeView.trackTarget(it.targetX, it.targetY)
                
                // إذا كانت الدقة عالية، تنفيذ الضربة
                if (it.accuracy >= 95) {
                    executePerfectShot(it)
                }
            }
            
            delay(1000) // كل ثانية
        }
    }
    
    private fun startSmartTracking() {
        // تتبع ذكي مع خطوط المسار
        if (smartLinesEnabled) {
            showBallLines()
        }
    }
    
    private fun showBallLines() {
        // عرض خطوط مسار الكرات
        BallLinesView.show(windowManager)
    }
    
    private fun executeAutoShot() {
        // تنفيذ ضربة تلقائية
        val shot = calculateBestShot()
        if (shot != null) {
            // محاكاة الضربة
            simulateShot(shot.angle, shot.power)
            
            // وميض العين
            eyeView.blink()
            
            // إرسال حدث الضربة
            sendShotEvent(shot)
        }
    }
    
    private fun executePerfectShot(shot: PerfectShot) {
        // تنفيذ ضربة مثالية
        simulateShot(shot.angle, shot.power)
        
        // وميض سريع
        eyeView.blink()
        
        // تأثير خاص
        showShotEffect()
    }
    
    private fun trackTarget(x: Float, y: Float) {
        // تتبع الهدف
        eyeView.trackTarget(x, y)
    }
    
    private fun findBestShot(): PerfectShot? {
        // خوارزمية للعثور على أفضل ضربة
        return PerfectShot(
            targetX = 500f,
            targetY = 300f,
            angle = 45f,
            power = 80f,
            accuracy = 98f
        )
    }
    
    private fun calculateBestShot(): ShotData? {
        // حساب أفضل ضربة
        return ShotData(
            angle = 30f,
            power = 75f,
            ballId = 1
        )
    }
    
    private fun simulateShot(angle: Float, power: Float) {
        // محاكاة الضربة
        // (سينفذ في لعبة البلياردو الفعلية)
    }
    
    private fun showShotEffect() {
        // تأثير بصرية للضربة
    }
    
    private fun sendShotEvent(shot: ShotData) {
        // إرسال حدث الضربة
    }
    
    private fun getLayoutParams(): WindowManager.LayoutParams {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        hideFloatingEye()
        serviceScope.cancel()
        isRunning = false
    }
    
    private fun hideFloatingEye() {
        try {
            windowManager.removeView(eyeView)
            isServiceRunning = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    // فئات البيانات
    data class PerfectShot(
        val targetX: Float,
        val targetY: Float,
        val angle: Float,
        val power: Float,
        val accuracy: Float
    )
    
    data class ShotData(
        val angle: Float,
        val power: Float,
        val ballId: Int
    )
// أضف داخل class EyeOverlayService:
companion object {
    var isRunning = false
}