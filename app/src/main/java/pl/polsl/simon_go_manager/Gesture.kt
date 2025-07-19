package pl.polsl.simon_go_manager

import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
//import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarksConnections
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmark

// Interface for all gestures
interface Gesture {
    val name: String
    fun isDetected(handLandmarkerResult: HandLandmarkerResult, handIndex: Int): Boolean
}

// Class to manage and check for multiple gestures
class GestureRecognizer(private val gestures: List<Gesture>) {

    fun recognizeGestures(handLandmarkerResult: HandLandmarkerResult): List<Gesture> {
        val detectedGestures = mutableListOf<Gesture>()
        if (handLandmarkerResult.landmarks().isNotEmpty()) {
            for (handIndex in handLandmarkerResult.landmarks().indices) {
                for (gesture in gestures) {
                    if (gesture.isDetected(handLandmarkerResult, handIndex)) {
                        detectedGestures.add(gesture)
                        // Optional: If a gesture is detected for a hand,
                        // you might want to stop checking other gestures for that same hand
                        // break
                    }
                }
            }
        }
        return detectedGestures.distinctBy { it.name } // Ensure unique gestures if detected by multiple hands
    }
}

class ThumbsUpGesture : Gesture {
    override val name: String = "Thumbs Up"

    override fun isDetected(handLandmarkerResult: HandLandmarkerResult, handIndex: Int): Boolean {
        val landmarks = handLandmarkerResult.landmarks()[handIndex]

        // Basic Thumbs Up Logic:
        // 1. Thumb tip is above thumb MCP (Metacarpophalangeal joint - base of thumb)
        // 2. Thumb tip is significantly higher than the MCPs of other fingers.
        // 3. Other fingers (index, middle, ring, pinky) are somewhat curled or closed:
        //    Their tips are lower than their PIP (Proximal Interphalangeal) joints or MCP joints.

        // Get specific landmark coordinates
        val thumbTip = landmarks[HandLandmark.THUMB_TIP]
        val thumbIp = landmarks[HandLandmark.THUMB_IP] // Intermediate thumb joint
        val thumbMcp = landmarks[HandLandmark.THUMB_MCP]

        val indexFingerTip = landmarks[HandLandmark.INDEX_FINGER_TIP]
        val indexFingerPip = landmarks[HandLandmark.INDEX_FINGER_PIP]
        val indexFingerMcp = landmarks[HandLandmark.INDEX_FINGER_MCP]

        val middleFingerTip = landmarks[HandLandmark.MIDDLE_FINGER_TIP]
        val middleFingerPip = landmarks[HandLandmark.MIDDLE_FINGER_PIP]
        val middleFingerMcp = landmarks[HandLandmark.MIDDLE_FINGER_MCP]

        val ringFingerTip = landmarks[HandLandmark.RING_FINGER_TIP]
        val ringFingerPip = landmarks[HandLandmark.RING_FINGER_PIP]
        val ringFingerMcp = landmarks[HandLandmark.RING_FINGER_MCP]

        val pinkyTip = landmarks[HandLandmark.PINKY_TIP]
        val pinkyPip = landmarks[HandLandmark.PINKY_PIP]
        val pinkyMcp = landmarks[HandLandmark.PINKY_MCP]

        // --- Thumb Conditions ---
        // Thumb tip is above thumb IP and MCP (Y decreases as you go up in image coordinates)
        val isThumbUp = thumbTip.y() < thumbIp.y() && thumbTip.y() < thumbMcp.y()

        // Thumb is extended (distance between tip and MCP is significant along the primary axis of the thumb)
        // This is a more robust check than just y-coordinate if the hand is rotated.
        // For simplicity, we'll stick to Y for now, but consider vector math for robustness.

        // --- Other Fingers Curled ---
        // (Tip Y is greater than PIP Y, or tip Y is greater than MCP Y)
        val isIndexFingerCurled = indexFingerTip.y() > indexFingerPip.y() || indexFingerTip.y() > indexFingerMcp.y()
        val isMiddleFingerCurled = middleFingerTip.y() > middleFingerPip.y() || middleFingerTip.y() > middleFingerMcp.y()
        val isRingFingerCurled = ringFingerTip.y() > ringFingerPip.y() || ringFingerTip.y() > ringFingerMcp.y()
        val isPinkyCurled = pinkyTip.y() > pinkyPip.y() || pinkyTip.y() > pinkyMcp.y()

        // --- Thumb is higher than other finger knuckles ---
        // (Thumb tip Y is less than the Y of other finger MCPs)
        val isThumbHigher = thumbTip.y() < indexFingerMcp.y() &&
                thumbTip.y() < middleFingerMcp.y() &&
                thumbTip.y() < ringFingerMcp.y() &&
                thumbTip.y() < pinkyMcp.y()

        return isThumbUp && isThumbHigher &&
                isIndexFingerCurled && isMiddleFingerCurled &&
                isRingFingerCurled && isPinkyCurled
    }
}