package com.eyepool.master

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.eyepool.master.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var isServiceRunning = false
    
    companion object {
        private const val OVERLAY_PERMISSION_CODE = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        checkOverlayPermission()
    }
    
    private fun setupUI() {
        // زر تشغيل/إيقاف العين
        binding.btnEyeControl.setOnClickListener {
            if (checkOverlayPermission()) {
                toggleEyeService()
            } else {
                requestOverlayPermission()
            }
        }
        
        // زر الإعداد السريع (مؤقت)
        binding.btnQuickSetup.setOnClickListener {
            Toast.makeText(this, "ميزة قريباً", Toast.LENGTH_SHORT).show()
        }
        
        // زر اكتشاف اللعبة (مؤقت)
        binding.btnDetectGame.setOnClickListener {
            Toast.makeText(this, "افتح لعبة بلياردو أولاً", Toast.LENGTH_SHORT).show()
            binding.tvGameStatus.text = "🎮 افتح لعبة بلياردو"
        }
        
        // زر الخطوط الذكية (مؤقت)
        binding.btnSmartLines.setOnClickListener {
            Toast.makeText(this, "ميزة قريباً", Toast.LENGTH_SHORT).show()
        }
        
        // زر الضرب التلقائي (مؤقت)
        binding.btnAutoShot.setOnClickListener {
            Toast.makeText(this, "ميزة قريباً", Toast.LENGTH_SHORT).show()
        }
        
        updateUI()
    }
    
    private fun toggleEyeService() {
        if (isServiceRunning) {
            // إيقاف الخدمة
            stopService(Intent(this, EyeOverlayService::class.java))
            isServiceRunning = false
            binding.tvStatus.text = "⏸️ الخدمة متوقفة"
            binding.btnEyeControl.text = "👁️ تشغيل العين"
        } else {
            // تشغيل الخدمة
            val intent = Intent(this, EyeOverlayService::class.java).apply {
                putExtra("mode", "FLOATING_EYE")
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            
            isServiceRunning = true
            binding.tvStatus.text = "✅ الخدمة نشطة"
            binding.btnEyeControl.text = "👁️ إيقاف العين"
            Toast.makeText(this, "العين العائمة تعمل!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_CODE)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_CODE) {
            if (checkOverlayPermission()) {
                toggleEyeService()
            } else {
                Toast.makeText(this, "يجب منح صلاحية العرض التلقائي", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateUI() {
        // لا تحتاج لعمل شيء حالياً
    }
    
    override fun onResume() {
        super.onResume()
        updateUI()
    }
}