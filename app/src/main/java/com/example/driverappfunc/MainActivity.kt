package com.example.driverappfunc

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.isNotEmpty
import com.example.driverappfunc.databinding.ActivityMainBinding
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private lateinit var toggleButton: ToggleButton
    private lateinit var button: Button

    private lateinit var cameraSource: CameraSource
    private lateinit var barcodeDetector: BarcodeDetector
    private var scannedValue = ""

    private val PERMISSION_REQUEST_CODE = 200

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        val aniSlide: Animation =
            AnimationUtils.loadAnimation(this@MainActivity, R.anim.scanner_animation)
        binding.barcodeLine.startAnimation(aniSlide)


        val isFlashAvailable = applicationContext.packageManager
            .hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
        if (!isFlashAvailable) {
            showNoFlashError()
        }
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager.cameraIdList[0]
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        toggleButton = findViewById(R.id.on_and_off_button)
        toggleButton.setOnCheckedChangeListener { _, isChecked -> switchFlashLight(isChecked) }

        button = findViewById(R.id.cameraButton)


        if (checkPermission()) {
            button.setOnClickListener {
                requestPermission()

            }

        } else {
            setupControls()
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, PERMISSION_REQUEST_CODE)
    }

    private fun showNoFlashError() {
        val alert = AlertDialog.Builder(this)
            .create()
        alert.setTitle("Oops!")
        alert.setMessage("Flash not available in this device...")
        alert.setButton(DialogInterface.BUTTON_POSITIVE, "OK") { _, _ -> finish() }
        alert.show()
    }

    private fun switchFlashLight(status: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, status)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            showNoFlashError()
        }
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE
        )
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, "Permission Granted", Toast.LENGTH_SHORT).show()
                setupControls()
            } else {
                Toast.makeText(applicationContext, "Permission Denied", Toast.LENGTH_SHORT).show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        showMessageOKCancel("You need to allow access permissions",
                            DialogInterface.OnClickListener { dialog, which ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    requestPermission()
                                }
                            })
                    }
                }
            }
        }
    }


    private fun showMessageOKCancel(message: String, okListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(this@MainActivity)
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private val processor = object : Detector.Processor<Barcode> {

        override fun release() {
            Toast.makeText(applicationContext, "Scanner has been closed", Toast.LENGTH_SHORT)
                .show()
        }

        override fun receiveDetections(detections: Detector.Detections<Barcode>) {

            if (detections != null && detections.detectedItems.isNotEmpty()) {
                val qrCodes: SparseArray<Barcode> = detections.detectedItems
                val code = qrCodes.valueAt(0)
                binding.scanResult.text = code.displayValue
            } else {
                binding.scanResult.text = ""
            }
        }
    }


    private fun setupControls() {
        barcodeDetector =
            BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.ALL_FORMATS).build()

        cameraSource = CameraSource.Builder(this, barcodeDetector)
            .setRequestedPreviewSize(1920, 1080)
            .setAutoFocusEnabled(true)
            .build()

        barcodeDetector.setProcessor(processor)





        binding.cameraSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            @SuppressLint("MissingPermission")
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    cameraSource.start(holder)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }


            @SuppressLint("MissingPermission")
            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                try {
                    cameraSource.start(holder)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraSource.stop()
            }
        })


    }

}
