package com.aspire.ubinex.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHandler {

    private const val REQUEST_PERMISSION_CODE = 111

    private val cameraXPermissionsList = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private val storagePermissionsList = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    fun requestCameraPermissions(activity: Activity) {
        requestPermissions(activity, cameraXPermissionsList, REQUEST_PERMISSION_CODE)
    }

    fun checkCameraPermissions(activity: Activity): Boolean {
        return checkPermissions(activity, cameraXPermissionsList)
    }

    fun requestStoragePermissions(activity: Activity) {
        requestPermissions(activity, storagePermissionsList, REQUEST_PERMISSION_CODE)
    }

    fun checkStoragePermissions(activity: Activity): Boolean {
        return checkPermissions(activity, storagePermissionsList)
    }

    private fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ActivityCompat.requestPermissions(activity, permissions, requestCode)
        }
    }

    private fun checkPermissions(activity: Activity, permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
}
