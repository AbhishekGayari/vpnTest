package com.xforia.vpnx

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {
lateinit var startVpnButton: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        startVpnButton = findViewById(R.id.startVpnButton)

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
        val blockedSites = listOf("whatsapp.com", "facebook.com", "youtube.com")

        saveBlockedWebsites(this, blockedSites)
        val listView = findViewById<ListView>(R.id.blockedSitesListView)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, blockedSites)
        listView.adapter = adapter
    }

    fun saveBlockedWebsites(context: Context, websites: List<String>) {
        startVpnButton.text = getString(R.string.domains_are_restricted)
        val sharedPreferences = context.getSharedPreferences("BlockedWebsites", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("blocked_domains", websites.toSet()).apply()
        showExitKioskAlert()
    }

    private fun showExitKioskAlert() {
        AlertDialog.Builder(this)
            .setTitle("Exit Kiosk Mode")
            .setMessage("To exit kiosk mode:\n\n1. Long press the power button\n2. Tap 'Exit Kiosk Mode' if shown\n3. Require admin PIN to exit.")
            .setPositiveButton("OK", null)
            .show()

    }

    private fun launchPermissionRequest() {
        Toast.makeText(this, "Enable Accessibility for ${getString(R.string.app_name)}", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        permissionLauncher.launch(intent)
    }
}