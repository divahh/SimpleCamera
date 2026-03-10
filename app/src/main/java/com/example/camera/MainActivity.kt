package com.example.camera

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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

class MainActivity: ComponentActivity() {
    private val cameraImageState = mutableStateOf<Bitmap?>(null)
    private val galleryUriState = mutableStateOf<Uri?>(null)

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            cameraImageState.value = bitmap
            galleryUriState.value = null
            Toast.makeText(this, "Terdapat gambar yang ditampilkan", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) takePictureLauncher.launch(null)
        else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }
    
    private fun cameraIntent(){
        if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePictureLauncher.launch(null)
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

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
                    }
                }

                splashScreen.setOnExitAnimationListener { splashScreenView ->
                    splashScreenView.iconView
                        .animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(0f)
                        .setDuration(600)
                        .withEndAction { splashScreenView.remove() }
                        .start()
                }

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(start = 12.dp, top = 20.dp, end = 12.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .fillMaxWidth()
                                    .aspectRatio(9f / 16f)
                                    .drawBehind {
                                        val strokeWidth = 2.dp.toPx()
                                        val dash = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()), 0f)

                                        drawRect(
                                            color = Color.Gray,
                                            size = size,
                                            style = Stroke(width = strokeWidth, pathEffect = dash)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    cameraBitmap != null -> AsyncImage(
                                        model = cameraBitmap,
                                        contentDescription = "Camera Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    galleryUri != null -> AsyncImage(
                                        model = galleryUri,
                                        contentDescription = "Gallery Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    else -> Icon(
                                        painter = painterResource(id = R.drawable.ic_add_photo),
                                        contentDescription = "Add New Image",
                                        tint = Color.Gray
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .align(Alignment.CenterHorizontally)
                            ) {
                                ButtonTiny(
                                    onClick = { cameraIntent() },
                                    text = "Open Camera"
                                )
                                ButtonTiny(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    text = "Open Gallery"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewMainActivity() {
    MainActivity()
}