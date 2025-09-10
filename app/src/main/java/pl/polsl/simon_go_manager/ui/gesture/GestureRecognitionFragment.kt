package pl.polsl.simon_go_manager.ui.gesture

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.mediapipe.tasks.vision.core.RunningMode
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import pl.polsl.simon_go_manager.GestureRecognizerHelper
import pl.polsl.simon_go_manager.databinding.FragmentGestureRecognitionBinding
import java.io.IOException
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class GestureRecognitionFragment : Fragment(),
    GestureRecognizerHelper.GestureRecognizerListener {

    companion object {
        private const val TAG = "HandGestureRecognizer"
    }

    private var _binding: FragmentGestureRecognitionBinding? = null
    private val binding get() = _binding!!

    private lateinit var gestureRecognizerHelper: GestureRecognizerHelper
    private val viewModel: CameraViewModel by activityViewModels()

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT

    private lateinit var backgroundExecutor: ExecutorService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGestureRecognitionBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backgroundExecutor = Executors.newSingleThreadExecutor()

        binding.previewView.post {
            setUpCamera()
        }

        backgroundExecutor.execute {
            gestureRecognizerHelper = GestureRecognizerHelper(
                context = requireContext(),
                runningMode = RunningMode.LIVE_STREAM,
                minHandDetectionConfidence = viewModel.currentMinHandDetectionConfidence,
                minHandTrackingConfidence = viewModel.currentMinHandTrackingConfidence,
                minHandPresenceConfidence = viewModel.currentMinHandPresenceConfidence,
                currentDelegate = viewModel.currentDelegate,
                gestureRecognizerListener = this
            )
        }
    }

    override fun onResume() {
        super.onResume()
        backgroundExecutor.execute {
            if (gestureRecognizerHelper.isClosed()) {
                gestureRecognizerHelper.setupGestureRecognizer()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (this::gestureRecognizerHelper.isInitialized) {
            viewModel.setMinHandDetectionConfidence(gestureRecognizerHelper.minHandDetectionConfidence)
            viewModel.setMinHandTrackingConfidence(gestureRecognizerHelper.minHandTrackingConfidence)
            viewModel.setMinHandPresenceConfidence(gestureRecognizerHelper.minHandPresenceConfidence)
            viewModel.setDelegate(gestureRecognizerHelper.currentDelegate)

            backgroundExecutor.execute {
                gestureRecognizerHelper.clearGestureRecognizer()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")
        val selector = CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding.previewView.display.rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding.previewView.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(backgroundExecutor) { image ->
                    recognizeHand(image)
                }
            }

        provider.unbindAll()

        try {
            camera = provider.bindToLifecycle(
                this, selector, preview, imageAnalyzer
            )
            preview?.setSurfaceProvider(binding.previewView.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun recognizeHand(imageProxy: ImageProxy) {
        gestureRecognizerHelper.recognizeLiveStream(imageProxy)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = binding.previewView.display.rotation
    }

    private fun sendCommandHttps(command: String) {
        try {
            // Zaufanie dla self-signed cert
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            val client = OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()

            val url = "https://192.168.1.50$command"

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("GestureCommand", "Błąd: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d("GestureCommand", "Odpowiedź: ${response.body?.string()}")
                }
            })

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var lastCommandTime = 0L
    private val COMMAND_COOLDOWN_MS = 1000L

    override fun onResults(resultBundle: GestureRecognizerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (_binding != null) {
                val gestures = resultBundle.results.first().gestures()
                val category = gestures.firstOrNull()?.maxByOrNull { it.score() }

                category?.let {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastCommandTime >= COMMAND_COOLDOWN_MS) {
                        lastCommandTime = currentTime

                        binding.textLabel.text = it.categoryName()
                        when (it.categoryName()) {
                            "Thumb_Up" -> {
                                sendCommandHttps("/s/1")
                                Toast.makeText(binding.root.context, "Włącz oba przekaźniki", Toast.LENGTH_SHORT).show()
                            }
                            "Thumb_Down" -> {
                                sendCommandHttps("/s/0")
                                Toast.makeText(binding.root.context, "Wyłącz oba przekaźniki", Toast.LENGTH_SHORT).show()
                            }
                            "Victory" -> {
                                sendCommandHttps("/s/2")
                                Toast.makeText(binding.root.context, "Zmień stan obu przekaźników", Toast.LENGTH_SHORT).show()
                            }
                            "Closed_Fist" -> {
                                sendCommandHttps("/s/dec/14")
                                Toast.makeText(binding.root.context, "Jasność -20", Toast.LENGTH_SHORT).show()
                            }
                            "Pointing_Up" -> {
                                sendCommandHttps("/s/t/inc/0A")
                                Toast.makeText(binding.root.context, "Temperatura +10", Toast.LENGTH_SHORT).show()
                            }
                            "Pointing_Down" -> {
                                sendCommandHttps("/s/t/dec/0A")
                                Toast.makeText(binding.root.context, "Temperatura -10", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                Toast.makeText(binding.root.context, "Nieznany gest: ${it.categoryName()}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        binding.textScore.text = String.format(Locale.US, "%.2f", it.score())
                    }
                } ?: run {
                    binding.textLabel.text = "--"
                    binding.textScore.text = "--"
                }

                binding.overlay.setResults(
                    resultBundle.results.first(),
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )
                binding.overlay.invalidate()
            }
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            binding.textLabel.text = "--"
            binding.textScore.text = "--"
        }
    }
}
