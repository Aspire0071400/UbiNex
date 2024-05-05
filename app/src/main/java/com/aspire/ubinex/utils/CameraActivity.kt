package com.aspire.ubinex.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.aspire.ubinex.R
import com.aspire.ubinex.databinding.CameraActivityBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class CameraActivity :AppCompatActivity(){

    private lateinit var binding : CameraActivityBinding
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraSelector: CameraSelector
    private lateinit var camera: Camera
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var currentUri : Uri
    private lateinit var resultIntent : Intent
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CameraActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cameraProvider = ProcessCameraProvider.getInstance(this).get()
        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        binding.flipCamBtn.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT){
                CameraSelector.LENS_FACING_BACK
            }else{
                CameraSelector.LENS_FACING_FRONT
            }
            bindCameraUseCases()
        }

        binding.flashBtn.setOnClickListener {
            if (camera.cameraInfo.hasFlashUnit()) {
                val torchState = camera.cameraInfo.torchState.value
                when (torchState) {
                    0 -> {
                        camera.cameraControl.enableTorch(true)
                        binding.flashBtn.setImageResource(R.drawable.flash_on)
                    }
                    1 -> {
                        camera.cameraControl.enableTorch(false)
                        binding.flashBtn.setImageResource(R.drawable.flash_off)
                    }
                    else -> {
                        Toast.makeText(this, "Flash not supported on current Camera", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Flash not supported on current Device", Toast.LENGTH_SHORT).show()
            }
        }


        binding.captureShutter.setOnClickListener {
            takePhoto()
        }

        binding.nextBtn.setOnClickListener {
            resultIntent = Intent().putExtra("capturedUri",currentUri.toString())
            Toast.makeText(this,"$resultIntent dgdsghj  $currentUri",Toast.LENGTH_LONG).show()
            setResult(RESULT_OK,resultIntent)
            finish()

        }

        binding.retakeBtn.setOnClickListener {
            binding.capturedPreviewImage.visibility = View.GONE
            binding.retakeBtn.visibility = View.GONE
            binding.nextBtn.visibility = View.GONE
            binding.previewView.visibility = View.VISIBLE
            binding.captureShutter.visibility = View.VISIBLE
            binding.flipCamBtn.visibility = View.VISIBLE
            binding.flashBtn.visibility = View.VISIBLE
        }

    }

    private fun takePhoto() {

        val fileName = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Ubinex-Image")
            }
        }

        try {
            val outputOptions = ImageCapture.OutputFileOptions
                .Builder(contentResolver,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues)
                .build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Toast.makeText(this@CameraActivity, "Photo saved: ${outputFileResults.savedUri}", Toast.LENGTH_SHORT).show()
                        val currentPhotoUri = outputFileResults.savedUri!!
                        currentUri = currentPhotoUri
                        previewCapturedImage(currentPhotoUri)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(this@CameraActivity, "Error saving photo: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this@CameraActivity, "Error creating temp file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun previewCapturedImage(currentPhotoUri: Uri) {
        camera.cameraControl.enableTorch(false)
        binding.flashBtn.setImageResource(R.drawable.flash_off)
        binding.capturedPreviewImage.visibility = View.VISIBLE
        binding.previewView.visibility = View.INVISIBLE
        Glide.with(this)
            .load(currentPhotoUri)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.capturedPreviewImage)
        binding.retakeBtn.visibility = View.VISIBLE
        binding.nextBtn.visibility = View.VISIBLE
        binding.captureShutter.visibility = View.GONE
        binding.flipCamBtn.visibility = View.GONE
        binding.flashBtn.visibility = View.GONE

    }

    private fun startCamera(){

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))

    }

    private fun bindCameraUseCases() {
        val screenAspectRatio = aspectRatio(binding.previewView.width,binding.previewView.height)
        val resolution = ResolutionSelector.Builder().setAspectRatioStrategy(
            AspectRatioStrategy(
                screenAspectRatio,AspectRatioStrategy.FALLBACK_RULE_AUTO
            )
        ).build()


        val preview = Preview.Builder()
            .setResolutionSelector(resolution)
            .build()
            .also {
                it.setSurfaceProvider (binding.previewView.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(resolution)
            .build()

        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(this,cameraSelector,preview,imageCapture)
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    private fun aspectRatio(width : Int, height : Int) : Int {
        val previewRatio = maxOf(width,height).toDouble() / minOf(width,height)
        return  if(abs(previewRatio - 4.0 / 3.0) <= abs(previewRatio - 16.0 / 9.0)){
            AspectRatio.RATIO_4_3
        }else{
            AspectRatio.RATIO_16_9
        }
    }

    override fun finish() {
        super.finish()
        cameraExecutor.shutdown()
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, CameraActivity::class.java)
        }
    }

}