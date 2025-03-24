package com.xforia.vpnx

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {
    private val VPN_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        checkPermissions()

    }
    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)

        // Ensure the enabled services string is not null and contains the expected service name
        val res = !enabledServices.isNullOrEmpty() && enabledServices.contains(AppBlockerService::class.java.name)
        Log.d("AppMonitor1", "Permission Error22 $res")
        return res
    }
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        checkPermissions()
    }

    private fun checkPermissions() {
        when {
            !isAccessibilityServiceEnabled() -> launchPermissionRequest()
            else -> startApp()
        }

    }

    private fun startApp() {
        saveBlockedWebsites(this, listOf("example.com", "facebook.com", "youtube.com"))
    }
    fun saveBlockedWebsites(context: Context, websites: List<String>) {
        val sharedPreferences = context.getSharedPreferences("BlockedWebsites", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("blocked_domains", websites.toSet()).apply()
    }

    private fun launchPermissionRequest() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        permissionLauncher.launch(intent)
    }
}