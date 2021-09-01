package com.example.camera

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.internal.annotation.CameraExecutor
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.camera.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var outputdirectory: File
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        outputdirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startcamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                Constants.REQ_PERMISSION,
                Constants.REQUEST_CODE_PERMISSION
            )
        }

        binding.cameraCaptureButton.setOnClickListener()
        {
            takephoto()
        }

    }

    private fun getOutputDirectory(): File {
        val mediadir = externalMediaDirs.firstOrNull()?.let { mFile ->
            File(mFile, resources.getString(R.string.app_name))
                .apply { mkdirs() }
        }

        return if (mediadir != null && mediadir.exists())
            mediadir else filesDir
    }

    private fun takephoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            outputdirectory,
            SimpleDateFormat(
                Constants.file_name_format,
                Locale.getDefault()
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputoption = ImageCapture.OutputFileOptions
            .Builder(photoFile)
            .build()

        imageCapture.takePicture(
            outputoption,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {


                    var saveuri = Uri.fromFile(photoFile)
                    val msg = "Photo saved"
                    Toast.makeText(this@MainActivity, "$msg $saveuri ", Toast.LENGTH_SHORT).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(
                        Constants.TAG,
                        "onError:${exception.message}",
                        exception
                    )
                }


            }


        )
    }

    private fun allPermissionsGranted(): Boolean = Constants.REQ_PERMISSION.all {
        ContextCompat.checkSelfPermission(
            baseContext,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == Constants.REQUEST_CODE_PERMISSION) {
            startcamera()
        } else {
            Toast.makeText(this, "Camera Permissions not granted.", Toast.LENGTH_SHORT).show()
            finish()
        }

    }

    private fun startcamera() {
        val cameraProviderfeature = ProcessCameraProvider.getInstance(this)
        val cameraProvider: ProcessCameraProvider = cameraProviderfeature.get()

        cameraProviderfeature.addListener(
            {
                val preview = Preview
                    .Builder()
                    .build()
                    .also { mPreview ->
                        mPreview.setSurfaceProvider(binding.viewFinder.createSurfaceProvider())
                    }
                imageCapture = ImageCapture.Builder()
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)


                } catch (e: Exception) {
                    Log.d(Constants.TAG, "Camera Start Failed.")
                }
            }, ContextCompat.getMainExecutor(this)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}
