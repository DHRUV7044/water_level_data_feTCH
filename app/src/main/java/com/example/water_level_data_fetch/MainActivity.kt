package com.example.water_level_data_fetch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { checkPermissionsAndStartService() }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { checkPermissionsAndStartService() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate: Starting permission check")
        checkPermissionsAndStartService()
    }

    private fun checkPermissionsAndStartService() {
        val hasNotificationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        val hasOverlayPermission = Settings.canDrawOverlays(this)

        Log.d("MainActivity", "Notification permission granted: $hasNotificationPermission")
        Log.d("MainActivity", "Overlay permission granted: $hasOverlayPermission")

        when {
            !hasNotificationPermission -> {
                Log.d("MainActivity", "Requesting notification permission")
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            !hasOverlayPermission -> {
                Log.d("MainActivity", "Requesting overlay permission")
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:$packageName".toUri()
                )
                overlayPermissionLauncher.launch(intent)
            }
            else -> {
                Log.d("MainActivity", "All permissions granted, starting service")
                startOverlayService()
            }
        }
    }

    private fun startOverlayService() {
        Log.d("MainActivity", "startOverlayService: Starting OverlayService")
        val intent = Intent(this, OverlayService::class.java)
        startForegroundService(intent)
        finish()
    }
}