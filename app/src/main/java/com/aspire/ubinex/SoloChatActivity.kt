package com.aspire.ubinex

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.aspire.ubinex.adapter.ChatAdapter
import com.aspire.ubinex.databinding.ActivitySoloChatBinding
import com.aspire.ubinex.model.ChatModel
import com.aspire.ubinex.utils.PermissionHandler
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File

class SoloChatActivity : AppCompatActivity() {

    private lateinit var senderUid: String
    private lateinit var binding : ActivitySoloChatBinding
    private lateinit var chatAdapter : ChatAdapter
    private lateinit var messageList : ArrayList<ChatModel>
    private lateinit var auth : FirebaseAuth
    private lateinit var fireStoreDb : FirebaseFirestore
    private lateinit var dbRef : DatabaseReference
    private var isKeyboardVisible = false
    private lateinit var userStatusRef: DatabaseReference
    private lateinit var storage : FirebaseStorage
    private lateinit var receiverRoom : String
    private lateinit var senderRoom : String
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var currentCameraSelector : CameraSelector
    private val REQUEST_IMAGE_PICK = 25
    private val REQUEST_IMAGE_CAPTURE = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySoloChatBinding.inflate(layoutInflater)
        val context : Context = this
        messageList = ArrayList()
        auth = FirebaseAuth.getInstance()
        fireStoreDb = FirebaseFirestore.getInstance()
        dbRef = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()

        val name = intent.getStringExtra("name")
        val imageUrl = intent.getStringExtra("image")
        val receiverUid = intent.getStringExtra("uid").toString()
        senderUid = auth.currentUser!!.uid

        binding.soloChatReceiverUserName.text = name
        Glide.with(this).load(imageUrl).into(binding.soloChatReceiverImage)

        senderRoom = "$receiverUid$senderUid"
        receiverRoom = "$senderUid$receiverUid"

        chatAdapter = ChatAdapter(context,binding.soloChatRecycler, messageList,senderRoom,receiverRoom)

        setContentView(binding.root)

        userStatusRef = FirebaseDatabase.getInstance().reference.child("user_status")

        binding.soloChatBackBtn.setOnClickListener { finish() }

        binding.soloChatKeyboardHandler.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            if (isKeyboardVisible) {
                imm.hideSoftInputFromWindow(binding.soloChatMessageField.windowToken, 0)
                binding.soloChatMessageField.clearFocus()
                isKeyboardVisible = false
            } else {
                binding.soloChatMessageField.requestFocus()
                imm.showSoftInput(binding.soloChatMessageField, InputMethodManager.SHOW_IMPLICIT)
                binding.soloChatMessageField.requestFocus()
                isKeyboardVisible = true
            }
        }

        binding.soloChatRecycler.layoutManager = LinearLayoutManager(this@SoloChatActivity)
        binding.soloChatRecycler.adapter = chatAdapter

        dbRef.child("chats").child(senderRoom).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = ArrayList<ChatModel>()
                    for (postSnapshot in snapshot.children) {
                        val message = postSnapshot.getValue(ChatModel::class.java)
                        message?.let { list.add(it) }
                    }
                    messageList.clear()
                    messageList.addAll(list)
                    chatAdapter.notifyDataSetChanged()
                    chatAdapter.scrollToEnd()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SoloChatActivity, "Network Error, check your network!", Toast.LENGTH_SHORT)
                        .show()
                }
            })

        binding.soloChatSend.setOnClickListener {
            val messageText = binding.soloChatMessageField.text.toString()
            if (messageText.isNotBlank()) {
                val timestamp = System.currentTimeMillis()
                val messageObject = ChatModel(
                    message = messageText,
                    senderId = senderUid,
                    timeStamp = timestamp
                )

                dbRef.child("chats").child(senderRoom).child("messages")
                    .push().setValue(messageObject).addOnSuccessListener {
                        dbRef.child("chats").child(receiverRoom).child("messages")
                            .push().setValue(messageObject)
                            .addOnSuccessListener {
                                binding.soloChatMessageField.text = null
                                binding.soloChatMessageField.clearFocus()
                            }
                    }
            }
        }


        binding.soloChatShareAttachment.setOnClickListener {
            checkStoragePermissionAndOpenGallery()
        }

        binding.soloChatCamera.setOnClickListener {
            //dispatchTakePictureIntent()
            //openCameraPreview()
            checkCameraPermissionAndOpenCamera()
        }

        val handler = Handler()
        binding.soloChatMessageField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val currentUserID = auth.currentUser?.uid ?: return
                val userStatusMap = hashMapOf<String, Any>("status" to "typing...")
                userStatusRef.child(currentUserID).updateChildren(userStatusMap)

                handler.removeCallbacksAndMessages(null)
                handler.postDelayed(userStoppedTyping, 2000)
            }

            private val userStoppedTyping = Runnable {
                val currentUserID = auth.currentUser?.uid ?: return@Runnable
                val userStatusMap = hashMapOf<String, Any>("status" to "active")
                userStatusRef.child(currentUserID).updateChildren(userStatusMap)
            }
        })

        setupUserStatusListener(receiverUid)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_IMAGE_PICK -> if (resultCode == Activity.RESULT_OK) {
                handleImagePickResult(data)
            }
            REQUEST_IMAGE_CAPTURE -> if (resultCode == Activity.RESULT_OK) {
                handleImageCaptureResult(data)
            }
        }
    }


    private fun uploadImageToStorage(imageUri: Uri) {
        val time = System.currentTimeMillis()
        val storageRef = FirebaseStorage.getInstance().reference.child("chats").child(senderRoom).child("$time")

        storageRef.putFile(imageUri).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                storageRef.downloadUrl.addOnCompleteListener { uriTask ->
                    val imageUrl = uriTask.result.toString()
                    val messageTxt = "-/*photo*/-"
                    val timestamp = System.currentTimeMillis()
                    val messageWithImageObject = ChatModel(
                        message = messageTxt,
                        senderId = senderUid,
                        timeStamp = timestamp,
                        imageUrl = imageUrl
                    )
                    sendMessageToFirebase(messageWithImageObject)
                }
            } else {
                Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessageToFirebase(message: ChatModel) {
        dbRef.child("chats").child(senderRoom).child("messages")
            .push().setValue(message).addOnSuccessListener {
                dbRef.child("chats").child(receiverRoom).child("messages")
                    .push().setValue(message)
                    .addOnSuccessListener {
                        binding.soloChatMessageField.text = null
                        Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun getImageUriFromBitmap(context: Context, bitmap: Bitmap): Uri {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val path = MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "Image Title", null)
        return Uri.parse(path)
    }

    private fun handleImageCaptureResult(data: Intent?) {
        val imageBitmap = data?.extras?.get("data") as? Bitmap
        imageBitmap?.let { bitmap ->
            val imageUri = getImageUriFromBitmap(this, bitmap)
            uploadImageToStorage(imageUri)
        }
    }

    private fun handleImagePickResult(data: Intent?) {
        val selectedImageUri = data?.data
        selectedImageUri?.let { uri ->
            uploadImageToStorage(uri)
        }
    }

    private fun checkStoragePermissionAndOpenGallery() {
        if (PermissionHandler.hasStoragePermission(this)) {
            openGallery()
        } else {
            PermissionHandler.requestStoragePermission(this)
        }
    }

    private fun checkCameraPermissionAndOpenCamera() {
        if (PermissionHandler.hasCameraPermission(this)) {
            openCameraPreview()
        } else {
            PermissionHandler.requestCameraPermission(this)
        }
    }

    private fun openGallery() {
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*"
        startActivityForResult(intent,REQUEST_IMAGE_PICK)
    }

    private fun openCameraPreview() {
        val inflater = LayoutInflater.from(this)
        val cameraView = inflater.inflate(R.layout.camera_on_chatroom,null)

        val previewView = cameraView.findViewById<PreviewView>(R.id.previewView)
        val btnCapture = cameraView.findViewById<ImageButton>(R.id.capture_shutter)
        val flipCamera = cameraView.findViewById<ImageButton>(R.id.flip_cam_btn)
        val retake = cameraView.findViewById<ImageButton>(R.id.retake_btn)
        val nextButton = cameraView.findViewById<ImageButton>(R.id.next_btn)
        val capturedImageView = findViewById<ImageView>(R.id.captured_preview_image)

        setContentView(cameraView)

        currentCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        // Initialize CameraX
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(previewView)
        }, ContextCompat.getMainExecutor(this))

        btnCapture.setOnClickListener {
            // Capture image
            captureImage(retake,nextButton)
        }

        flipCamera.setOnClickListener { flipCamera(previewView, currentCameraSelector) }
        retake.setOnClickListener {
            capturedImageView.visibility = View.GONE
            retake.visibility = View.GONE
            nextButton.visibility = View.GONE
            btnCapture.visibility = View.VISIBLE
            flipCamera.visibility = View.VISIBLE
        }
        nextButton.setOnClickListener {  }
    }
    private fun flipCamera(previewView: PreviewView, currentCameraState : CameraSelector) {

        if (currentCameraState == CameraSelector.DEFAULT_BACK_CAMERA) {
            currentCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            currentCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }

        bindCameraUseCases(previewView)
    }

    private fun bindCameraUseCases(previewView: PreviewView) {
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        val cameraSelector = currentCameraSelector

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview)
        } catch (e: Exception) {
            Toast.makeText(this,"Use case binding failed",Toast.LENGTH_SHORT).show()
        }
    }

    private fun captureImage(retake: ImageButton, nextButton: ImageButton) {
        // Create a file to save the captured image
        val photoFile = File(externalMediaDirs.firstOrNull(), "${System.currentTimeMillis()}.jpg")

        // Set up image capture use case
        val imageCapture = ImageCapture.Builder()
            .setTargetRotation(windowManager.defaultDisplay.rotation)
            .build()

        // Set up output options to save the captured image
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Capture the image
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // Image saved successfully, display it in ImageView
                    val capturedImageUri = Uri.fromFile(photoFile)
                    retake.visibility = View.VISIBLE
                    nextButton.visibility = View.VISIBLE
                    displayCapturedImage(capturedImageUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    // Handle error
                    Toast.makeText(this@SoloChatActivity, "Error capturing image", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun displayCapturedImage(imageUri: Uri) {
        // Load the captured image into an ImageView for preview
        val capturedImageView = findViewById<ImageView>(R.id.captured_preview_image)
        capturedImageView.visibility = View.VISIBLE
        Glide.with(this)
            .load(imageUri)
            .into(capturedImageView)
    }

    private fun setupUserStatusListener(receiverUid: String) {
        userStatusRef.child(receiverUid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.child("status").value?.toString()
                    binding.soloChatCurrentStatus.text = when (status) {
                        "online" -> "Online"
                        "offline" -> "Offline"
                        "active" -> "Active"
                        "typing..." -> "Typing..."
                        else -> "Unknown"
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateUserStatus(status: String) {
        val currentUserID = auth.currentUser?.uid ?: return
        val userStatusMap = hashMapOf<String, Any>("status" to status)
        userStatusRef.child(currentUserID).updateChildren(userStatusMap)
    }

    override fun onPause() {
        updateUserStatus("online")
        super.onPause()
    }

    override fun onResume() {
        updateUserStatus("active")
        super.onResume()
    }

    override fun onBackPressed() {
        updateUserStatus("online")
        super.onBackPressed()
        finish()
    }

    override fun finish() {
        updateUserStatus("online")
        super.finish()
    }

}
