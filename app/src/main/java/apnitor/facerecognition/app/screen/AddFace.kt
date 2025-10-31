package apnitor.facerecognition.app.screen

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import apnitor.facerecognition.app.HeadPoseEstimator
import apnitor.facerecognition.app.viewmodel.AddFaceScreenViewModel
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.util.ArrayDeque
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGetImage::class)
@Composable
fun AddFaceScreen(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val viewModel: AddFaceScreenViewModel = hiltViewModel()
    val uiScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }


    // -------- UI State --------
    var personName by remember { mutableStateOf("") }
    val steps = listOf("Look Right", "Look Left", "Look Slight Up", "Look Slight Down")
    var stepIndex by remember { mutableStateOf(0) }
    var started by remember { mutableStateOf(false) }
    var previewBitmapRaw by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmapDisplay by remember { mutableStateOf<Bitmap?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    val approvedBitmaps = remember { mutableStateListOf<Bitmap>() }

    // Camera permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val requestPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }
    LaunchedEffect(Unit) { if (!hasCameraPermission) requestPerm.launch(Manifest.permission.CAMERA) }

    // Pose estimator
    val headPoseEstimator = remember { HeadPoseEstimator() }
    DisposableEffect(Unit) { onDispose { headPoseEstimator.close() } }

    // Analyzer control/state
    var paused by remember { mutableStateOf(false) }
    var captureInProgress by remember { mutableStateOf(false) }
    var stableCount by remember { mutableStateOf(0) }
    val STABLE_FRAMES = 6

    // Smoothing windows (moving average)
    val yawWindow = remember { ArrayDeque<Float>() }
    val pitchWindow = remember { ArrayDeque<Float>() }
    val rollWindow = remember { ArrayDeque<Float>() }
    val SMOOTH_N = 5

    // Pose thresholds + constraints
    val yawEnter = 22f
    val pitchUpEnter = -15f
    val pitchDownEnter = 15f
    val rollLimit = 12f
    val minFaceFrac = 0.06f

    // ImageCapture for stills (high quality)
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val mainExecutor: Executor = remember { ContextCompat.getMainExecutor(context) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Add Employee Face") })
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Spacer(Modifier.height(12.dp))
            TextField(
                value = personName,
                onValueChange = { personName = it },
                label = { Text("Enter Employee Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            Box(Modifier.weight(1f)) {
                if (!hasCameraPermission) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Camera permission required")
                    }
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val provider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().apply {
                                setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val analyzer = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            analyzer.setAnalyzer(
                                Executors.newSingleThreadExecutor()
                            ) { imageProxy ->
                                analyzePoseFrame(
                                    imageProxy = imageProxy,
                                    started = started,
                                    paused = paused || captureInProgress || showPreview || isSaving,
                                    headPoseEstimator = headPoseEstimator,
                                    expected = steps.getOrNull(stepIndex),
                                    yawWindow = yawWindow,
                                    pitchWindow = pitchWindow,
                                    rollWindow = rollWindow,
                                    smoothN = SMOOTH_N,
                                    thresholds = Thresholds(
                                        yawEnter = yawEnter,
                                        pitchUpEnter = pitchUpEnter,
                                        pitchDownEnter = pitchDownEnter,
                                        rollAbsMax = rollLimit,
                                        minFaceFraction = minFaceFrac
                                    ),
                                    stableCount = stableCount,
                                    requiredStable = STABLE_FRAMES,
                                    onStablePose = {
                                        if (!captureInProgress) {
                                            captureInProgress = true
                                            imageCapture.takePicture(
                                                mainExecutor,
                                                object : ImageCapture.OnImageCapturedCallback() {
                                                    override fun onCaptureSuccess(img: ImageProxy) {
                                                        val raw = imageProxyToBitmap(img)
                                                        val display = raw.mirrored()
                                                        img.close()

                                                        previewBitmapRaw = raw
                                                        previewBitmapDisplay = display
                                                        showPreview = true
                                                        paused = true
                                                        captureInProgress = false
                                                    }
                                                    override fun onError(exc: ImageCaptureException) {
                                                        captureInProgress = false
                                                    }
                                                }
                                            )
                                        }
                                    },
                                    onStableCountUpdate = { stableCount = it }
                                )
                            }

                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_FRONT_CAMERA,
                                preview,
                                analyzer,
                                imageCapture
                            )
                            previewView
                        }
                    )
                }

                if (!started) {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "We will capture 4 angles:\nRight • Left • Up • Down",
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            enabled = personName.isNotEmpty() && hasCameraPermission,
                            onClick = { started = true }
                        ) { Text("Start") }
                    }
                } else if (!isSaving) {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    ) {
                        Surface(tonalElevation = 3.dp, shape = MaterialTheme.shapes.small) {
                            Text(
                                text = "Instruction: ${steps.getOrNull(stepIndex) ?: "Done"}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            if (showPreview && previewBitmapDisplay != null && previewBitmapRaw != null) {
                ConfirmCaptureDialog(
                    bitmap = previewBitmapDisplay!!,
                    onApprove = {
                        approvedBitmaps.add(previewBitmapRaw!!)
                        showPreview = false
                        previewBitmapRaw = null
                        previewBitmapDisplay = null
                        stableCount = 0
                        yawWindow.clear(); pitchWindow.clear(); rollWindow.clear()

                        if (stepIndex < steps.lastIndex) {
                            // Move to next step
                            stepIndex += 1
                            paused = false
                        } else {
                            // Final step completed - save and exit
                            isSaving = true
                            paused = true
                            uiScope.launch {
                                val result = runCatching {
                                    viewModel.saveApprovedBitmaps(personName, approvedBitmaps.toList())
                                }
                                isSaving = false
                                result.onSuccess {
                                    onNavigateBack()
                                }.onFailure { e ->
                                    // Handle error - could show a Snackbar or dialog
                                    paused = false
                                }
                            }
                        }
                    },
                    onRetake = {
                        showPreview = false
                        previewBitmapRaw = null
                        previewBitmapDisplay = null
                        paused = false
                        stableCount = 0
                    }
                )
            }

        }
    }
    if (isSaving) {
        AlertDialog(
            onDismissRequest = { /* block */ },
            title = { Text("Saving") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

}

private fun Bitmap.mirrored(): Bitmap {
    val m = Matrix().apply { preScale(-1f, 1f) }
    return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
}

/* ---------- Analyzer pipeline ---------- */

private data class Thresholds(
    val yawEnter: Float,
    val pitchUpEnter: Float,
    val pitchDownEnter: Float,
    val rollAbsMax: Float,
    val minFaceFraction: Float
)

private fun analyzePoseFrame(
    imageProxy: ImageProxy,
    started: Boolean,
    paused: Boolean,
    headPoseEstimator: HeadPoseEstimator,
    expected: String?,
    yawWindow: ArrayDeque<Float>,
    pitchWindow: ArrayDeque<Float>,
    rollWindow: ArrayDeque<Float>,
    smoothN: Int,
    thresholds: Thresholds,
    stableCount: Int,
    requiredStable: Int,
    onStablePose: () -> Unit,
    onStableCountUpdate: (Int) -> Unit
) {
    try {
        if (!started || paused || expected == null) return
        val pose = headPoseEstimator.estimate(imageProxy) ?: return

        if (pose.faceFraction < thresholds.minFaceFraction) {
            onStableCountUpdate(0); return
        }
        if (abs(pose.roll) > thresholds.rollAbsMax) {
            onStableCountUpdate(0); return
        }

        val correctedYaw = -pose.yaw
        val correctedPitch = -pose.pitch

        pushAndTrim(yawWindow, correctedYaw, smoothN)
        pushAndTrim(pitchWindow, correctedPitch, smoothN)
        pushAndTrim(rollWindow, pose.roll, smoothN)

        val yawAvg = yawWindow.average().toFloat()
        val pitchAvg = pitchWindow.average().toFloat()

        val match = when (expected) {
            "Look Right" -> yawAvg > thresholds.yawEnter
            "Look Left"  -> yawAvg < -thresholds.yawEnter
            "Look Slight Up"    -> pitchAvg < thresholds.pitchUpEnter
            "Look Slight Down"  -> pitchAvg > thresholds.pitchDownEnter
            else -> false
        }

        if (match) {
            val newCount = (stableCount + 1).coerceAtMost(requiredStable)
            onStableCountUpdate(newCount)
            if (newCount >= requiredStable) {
                onStableCountUpdate(0)
                yawWindow.clear(); pitchWindow.clear(); rollWindow.clear()
                onStablePose()
            }
        } else {
            onStableCountUpdate(0)
        }
    } finally {
        imageProxy.close()
    }
}

private fun <T> ArrayDeque<T>.average(): Double where T : Number {
    if (isEmpty()) return 0.0
    var sum = 0.0
    for (v in this) sum += v.toDouble()
    return sum / size
}

private fun pushAndTrim(deque: ArrayDeque<Float>, value: Float, maxSize: Int) {
    deque.addLast(value)
    while (deque.size > maxSize) deque.removeFirst()
}

/* ---------- Capture helpers ---------- */

@Composable
private fun ConfirmCaptureDialog(
    bitmap: Bitmap,
    onApprove: () -> Unit,
    onRetake: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* no outside dismiss */ },
        title = { Text("Use this photo?") },
        text = {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )
        },
        confirmButton = { TextButton(onClick = onApprove) { Text("✓ Use") } },
        dismissButton = { TextButton(onClick = onRetake) { Text("↺ Retake") } }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer: ByteBuffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
    var bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    val rotation = image.imageInfo.rotationDegrees
    if (rotation != 0) {
        val m = Matrix().apply { postRotate(rotation.toFloat()) }
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }
    return bmp
}