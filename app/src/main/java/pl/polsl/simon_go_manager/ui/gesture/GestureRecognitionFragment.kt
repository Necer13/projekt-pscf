package pl.polsl.simon_go_manager.ui.gesture

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Camera
import androidx.camera.core.AspectRatio
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.navigation.activity
import com.google.mediapipe.tasks.core.Delegate
import pl.polsl.simon_go_manager.HandLandmarkerHelper
import pl.polsl.simon_go_manager.ui.gesture.CameraViewModel
import pl.polsl.simon_go_manager.R
import pl.polsl.simon_go_manager.PermissionsFragment
import pl.polsl.simon_go_manager.databinding.FragmentGestureRecognitionBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import pl.polsl.simon_go_manager.Gesture
import pl.polsl.simon_go_manager.GestureRecognizer
import pl.polsl.simon_go_manager.ThumbsUpGesture
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class GestureRecognitionFragment : Fragment(), HandLandmarkerHelper.LandmarkerListener {

    companion object {
        private const val TAG = "Hand Landmarker"
        // --- Stability Thresholds ---
        // Number of consecutive frames a gesture needs to be present to be confirmed
        private const val GESTURE_CONFIRMATION_THRESHOLD = 3
        // Number of consecutive frames a gesture needs to be absent to be confirmed lost
        private const val GESTURE_LOST_THRESHOLD = 5
    }

    private var _fragmentCameraBinding: FragmentGestureRecognitionBinding? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var handLandmarkerHelper: HandLandmarkerHelper
    private val viewModel: CameraViewModel by activityViewModels()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT

    /** Blocking ML operations are performed using this executor */
    private lateinit var backgroundExecutor: ExecutorService

    private lateinit var gestureRecognizer: GestureRecognizer

    // Stores the current confirmed active gestures
    private val confirmedActiveGestures = mutableSetOf<String>()

    // Tracks potential gestures and their stability counts for activation
    private val potentialActivationCounts = mutableMapOf<String, Int>()

    // Tracks active gestures and their stability counts for deactivation
    private val potentialDeactivationCounts = mutableMapOf<String, Int>()


    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
//        if (!PermissionsFragment.hasPermissions(requireContext())) {
//            Navigation.findNavController(
//                requireActivity(), R.id.previewView
//            ).navigate(R.id.action_camera_to_permissions)
//        }

        // Start the HandLandmarkerHelper again when users come back
        // to the foreground.
        backgroundExecutor.execute {
            if (handLandmarkerHelper.isClose()) {
                handLandmarkerHelper.setupHandLandmarker()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if(this::handLandmarkerHelper.isInitialized) {
            viewModel.setMaxHands(handLandmarkerHelper.maxNumHands)
            viewModel.setMinHandDetectionConfidence(handLandmarkerHelper.minHandDetectionConfidence)
            viewModel.setMinHandTrackingConfidence(handLandmarkerHelper.minHandTrackingConfidence)
            viewModel.setMinHandPresenceConfidence(handLandmarkerHelper.minHandPresenceConfidence)
            viewModel.setDelegate(handLandmarkerHelper.currentDelegate)

            // Close the HandLandmarkerHelper and release resources
            backgroundExecutor.execute { handLandmarkerHelper.clearHandLandmarker() }
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding =
            FragmentGestureRecognitionBinding.inflate(inflater, container, false)

        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize our background executor
        backgroundExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        fragmentCameraBinding.previewView.post {
            // Set up the camera and its use cases
            setUpCamera()
        }

        // Create the HandLandmarkerHelper that will handle the inference
        backgroundExecutor.execute {
            handLandmarkerHelper = HandLandmarkerHelper(
                context = requireContext(),
                runningMode = RunningMode.LIVE_STREAM,
                minHandDetectionConfidence = viewModel.currentMinHandDetectionConfidence,
                minHandTrackingConfidence = viewModel.currentMinHandTrackingConfidence,
                minHandPresenceConfidence = viewModel.currentMinHandPresenceConfidence,
                maxNumHands = viewModel.currentMaxHands,
                currentDelegate = viewModel.currentDelegate,
                handLandmarkerHelperListener = this
            )
        }

        // Initialize GestureRecognizer with the gestures you want to detect
        val supportedGestures = listOf(
            ThumbsUpGesture()
            // Add more gestures here: e.g., VSignGesture(), ThumbsDownGesture()
        )
        gestureRecognizer = GestureRecognizer(supportedGestures)

        // Attach listeners to UI control widgets
        //initBottomSheetControls()
    }





    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.previewView.display.rotation)
            .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.previewView.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(backgroundExecutor) { image ->
                        detectHand(image)
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.previewView.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectHand(imageProxy: ImageProxy) {
        handLandmarkerHelper.detectLiveStream(
            imageProxy = imageProxy,
            isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            fragmentCameraBinding.previewView.display.rotation
    }

    // Update UI after hand have been detected. Extracts original
    // image height/width to scale and place the landmarks properly through
    // OverlayView
    override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (_fragmentCameraBinding == null) return@runOnUiThread

            val handLandmarkerResult = resultBundle.results.firstOrNull()
            val gesturesRecognizedThisFrame = if (handLandmarkerResult != null) {
                gestureRecognizer.recognizeGestures(handLandmarkerResult).map { it.name }.toSet()
            } else {
                emptySet()
            }

            Log.d(TAG, "Frame - Recognized: ${gesturesRecognizedThisFrame.joinToString()}, Confirmed: ${confirmedActiveGestures.joinToString()}")

            // --- Process for Activation ---
            for (gestureName in gesturesRecognizedThisFrame) {
                if (gestureName !in confirmedActiveGestures) { // Only consider if not already confirmed
                    val currentCount = potentialActivationCounts.getOrDefault(gestureName, 0) + 1
                    potentialActivationCounts[gestureName] = currentCount
                    potentialDeactivationCounts.remove(gestureName) // Reset deactivation count if seen again

                    if (currentCount >= GESTURE_CONFIRMATION_THRESHOLD) {
                        if (confirmedActiveGestures.add(gestureName)) { // .add() returns true if added
                            Log.i(TAG, "Gesture Activated (Confirmed): $gestureName")
                            Toast.makeText(requireContext(), "Activated: $gestureName", Toast.LENGTH_SHORT).show()
                            // --- YOUR ACTION FOR CONFIRMED NEW GESTURE GOES HERE ---
                        }
                        potentialActivationCounts.remove(gestureName) // Reset count after confirmation
                    }
                } else {
                    // If it's already confirmed, ensure its potential deactivation count is reset
                    potentialDeactivationCounts.remove(gestureName)
                    // And ensure it's not in potential activation queue
                    potentialActivationCounts.remove(gestureName)
                }
            }

            // --- Process for Deactivation ---
            val gesturesNoLongerSeenThisFrame = confirmedActiveGestures - gesturesRecognizedThisFrame
            for (gestureName in gesturesNoLongerSeenThisFrame) {
                val currentCount = potentialDeactivationCounts.getOrDefault(gestureName, 0) + 1
                potentialDeactivationCounts[gestureName] = currentCount
                // Do NOT reset potentialActivationCounts here, as it might just be a flicker

                if (currentCount >= GESTURE_LOST_THRESHOLD) {
                    if (confirmedActiveGestures.remove(gestureName)) { // .remove() returns true if removed
                        Log.i(TAG, "Gesture Deactivated (Confirmed): $gestureName")
                        // --- YOUR ACTION FOR CONFIRMED LOST GESTURE GOES HERE (OPTIONAL) ---
                    }
                    potentialDeactivationCounts.remove(gestureName) // Reset count after confirmation
                }
            }

            // --- Cleanup potential activation counts for gestures not seen this frame ---
            val gesturesToRemoveFromPotentialActivation = potentialActivationCounts.keys - gesturesRecognizedThisFrame
            for(gestureName in gesturesToRemoveFromPotentialActivation) {
                potentialActivationCounts.remove(gestureName)
            }


            // --- UI Update (displaying only confirmed active gestures) ---
            if (confirmedActiveGestures.isNotEmpty()) {
                // fragmentCameraBinding.gestureTextView.text = "Active: ${confirmedActiveGestures.joinToString()}"
            } else {
                // fragmentCameraBinding.gestureTextView.text = ""
            }
            // Determine if ANY gesture is currently confirmed active
            val isAnyGestureConfirmedActive = confirmedActiveGestures.isNotEmpty()

            // Pass necessary information to OverlayView, including the gesture active state
            fragmentCameraBinding.overlay.setResults(
                resultBundle.results.first(),
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                RunningMode.LIVE_STREAM,
                isGestureCurrentlyActive = isAnyGestureConfirmedActive // Pass the state here
            )
            //fragmentCameraBinding.overlay.invalidate()
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            if (errorCode == HandLandmarkerHelper.GPU_ERROR) {
                    // Try again with the fallback GPU delegate

            }
        }
    }
}


//private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
//
//        cameraProviderFuture.addListener({
//            val cameraProvider = cameraProviderFuture.get()
//            val preview = Preview.Builder().build().also {
//                it.setSurfaceProvider(binding.previewView.surfaceProvider)
//            }
//
//            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
//            cameraProvider.unbindAll()
//            cameraProvider.bindToLifecycle(
//                viewLifecycleOwner, cameraSelector, preview
//            )
//
//        }, ContextCompat.getMainExecutor(requireContext()))
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}