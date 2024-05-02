package com.aspire.ubinex.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHandler {

    private const val REQUEST_PERMISSION_CODE = 123

    // Camera Permission Functions

    fun requestCameraPermission(activity: Activity) {
        requestPermission(activity, Manifest.permission.CAMERA)
    }

    fun hasCameraPermission(activity: AppCompatActivity): Boolean {
        return checkPermission(activity, Manifest.permission.CAMERA)
    }

    // Storage Permission Functions

    fun requestStoragePermission(activity: Activity) {
        val storagePermissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
        requestPermission(activity, *storagePermissions)
    }

    fun hasStoragePermission(activity: AppCompatActivity): Boolean {
        return checkPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    // Helper Functions

    private fun requestPermission(activity: Activity, vararg permissions: String) {
        val permissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsNeeded.toTypedArray(),
                REQUEST_PERMISSION_CODE
            )
        }
    }

    private fun checkPermission(activity: AppCompatActivity, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }

    // Handle Permissions Result

    fun handlePermissionsResult(
        requestCode: Int,
        grantResults: IntArray,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                onSuccess()
            } else {
                onFailure()
            }
        }
    }
}

