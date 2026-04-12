package com.example.camera

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import coil.compose.AsyncImage

import com.example.camera.ui.component.ButtonTiny
import com.example.camera.ui.theme.CameraTheme

import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class MainActivity : ComponentActivity() {

    private val cameraImageState = mutableStateOf<Bitmap?>(null)
    private val galleryUriState = mutableStateOf<Uri?>(null)


    private fun getDummyBitmap(): Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.qr)

    private fun isUrl(text: String?): Boolean {
        return text != null &&
                (text.startsWith("http://") || text.startsWith("https://"))
    }

    private fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun showQrDialog(url: String) {
        AlertDialog.Builder(this)
            .setTitle("Buka Link")
            .setMessage("Apakah ingin membuka link ini?\n\n$url")
            .setPositiveButton("Telusuri") { _, _ -> openLink(url) }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // =========================
    // 🔥 CONVERT URI → BITMAP
    // =========================

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    // =========================
    // QR SCAN LOGIC
    // =========================

    private fun scanQr(bitmap: Bitmap) {
        val scanner = BarcodeScanning.getClient()
        val image = InputImage.fromBitmap(bitmap, 0)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isEmpty()) {
                    showToast("QR tidak ditemukan")
                    return@addOnSuccessListener
                }

                val result = barcodes.first().rawValue

                if (isUrl(result)) {
                    showQrDialog(result!!)
                } else {
                    showToast("QR: $result")
                }
            }
            .addOnFailureListener {
                showToast("Gagal scan QR")
            }
    }


    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            cameraImageState.value = it
            galleryUriState.value = null
            scanQr(it)
        } ?: showToast("Gagal memuat gambar")
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) takePictureLauncher.launch(null)
        else showToast("Camera permission denied")
    }

    private fun openCamera() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            takePictureLauncher.launch(null)
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // =========================
    // 🔹 UI
    // =========================

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        super.onCreate(savedInstanceState)

        setContent {
            CameraTheme {

                val galleryUri by galleryUriState
                val cameraBitmap by cameraImageState

                val imagePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let {
                        galleryUriState.value = it
                        cameraImageState.value = null

                        // 🔥 AUTO SCAN DARI GALERI
                        val bitmap = uriToBitmap(it)
                        bitmap?.let { bmp ->
                            scanQr(bmp)
                        } ?: showToast("Gagal memuat gambar dari galeri")
                    }
                }

                splashScreen.setOnExitAnimationListener { splashScreenView ->
                    splashScreenView.iconView.animate()
                        .alpha(0f)
                        .setDuration(600)
                        .withEndAction { splashScreenView.remove() }
                        .start()
                }

                Scaffold {
                    Surface(modifier = Modifier.padding(12.dp)) {
                        Column {

                            ImagePreview(cameraBitmap, galleryUri)

                            ActionButtons(
                                onCamera = { openCamera() },
                                onGallery = { imagePickerLauncher.launch("image/*") },
                                onDummy = {
                                    val dummy = getDummyBitmap()
                                    cameraImageState.value = dummy
                                    galleryUriState.value = null
                                    scanQr(dummy)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================
// 🔹 COMPOSABLE TERPISAH
// =========================

@Composable
fun ImagePreview(bitmap: Bitmap?, uri: Uri?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .drawBehind {
                drawRect(
                    color = Color.Gray,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> AsyncImage(
                model = bitmap,
                contentDescription = "Camera Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            uri != null -> AsyncImage(
                model = uri,
                contentDescription = "Gallery Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            else -> Icon(
                painter = painterResource(id = R.drawable.ic_add_photo),
                contentDescription = "Add Image",
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun ActionButtons(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDummy: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        ButtonTiny(onClick = onCamera, text = "Scan QR")
        ButtonTiny(onClick = onGallery, text = "Gallery")
        ButtonTiny(onClick = onDummy, text = "Test QR")
    }
}

@Preview
@Composable
fun PreviewMainActivity() {
    MainActivity()
}