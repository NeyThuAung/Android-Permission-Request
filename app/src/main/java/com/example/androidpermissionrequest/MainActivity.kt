package com.example.androidpermissionrequest

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.androidpermissionrequest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val cameraPermission = android.Manifest.permission.CAMERA
    private val cameraAndStoragePermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE // Needed for older versions
            )
        }

    // Permission launcher for single or multiple permissions
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val deniedPermissions = permissions.filterNot { it.value }.keys
        if (deniedPermissions.isEmpty()) {
            Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            handlePermissionDenied(deniedPermissions.toList())
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Single Permission button
        binding.btnSinglePermission.setOnClickListener {
            checkAndRequestSinglePermission()
        }

        // Multiple Permissions button
        binding.btnMultiplePermission.setOnClickListener {
            checkAndRequestMultiplePermissions()
        }

    }

    private fun checkAndRequestSinglePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                cameraPermission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, cameraPermission)) {
                showRationaleDialog(singlePermission = true)
            } else {
                permissionLauncher.launch(arrayOf(cameraPermission))
            }
        } else {
            Toast.makeText(this, "Camera permission already granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestMultiplePermissions() {
        val missingPermissions = cameraAndStoragePermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            if (missingPermissions.any { permission ->
                    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
                }) {
                showRationaleDialog(singlePermission = false)
            } else {
                permissionLauncher.launch(missingPermissions.toTypedArray())
            }
        } else {
            Toast.makeText(this, "All permissions already granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRationaleDialog(singlePermission: Boolean) {
        val message = if (singlePermission) {
            "Camera permission is required to capture photos."
        } else {
            "Camera and Storage permissions are required for full functionality."
        }
        AlertDialog.Builder(this)
            .setTitle("Permission Needed")
            .setMessage(message)
            .setPositiveButton("Allow") { _, _ ->
                if (singlePermission) {
                    permissionLauncher.launch(arrayOf(cameraPermission))
                } else {
                    permissionLauncher.launch(cameraAndStoragePermissions)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun handlePermissionDenied(deniedPermissions: List<String>) {
        if (deniedPermissions.any { permission ->
                !ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
            }) {
            showSettingsDialog()
        } else {
            showRationaleDialog(singlePermission = deniedPermissions.size == 1)
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Denied")
            .setMessage("You have denied permissions permanently. Go to App Settings to manually enable them for full functionality.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}