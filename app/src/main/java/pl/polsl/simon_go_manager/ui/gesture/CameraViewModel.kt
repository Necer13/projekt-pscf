
package pl.polsl.simon_go_manager.ui.gesture

import androidx.lifecycle.ViewModel
import pl.polsl.simon_go_manager.GestureRecognizerHelper

/**
 *  This ViewModel is used to store hand landmarker helper settings
 */
class CameraViewModel : ViewModel() {

    private var _delegate: Int = GestureRecognizerHelper.DELEGATE_CPU
    private var _minHandDetectionConfidence: Float =
        GestureRecognizerHelper.DEFAULT_HAND_DETECTION_CONFIDENCE
    private var _minHandTrackingConfidence: Float = GestureRecognizerHelper
        .DEFAULT_HAND_TRACKING_CONFIDENCE
    private var _minHandPresenceConfidence: Float = GestureRecognizerHelper
        .DEFAULT_HAND_PRESENCE_CONFIDENCE
    //private var _maxHands: Int = GestureRecognizerHelper.DEFAULT_NUM_HANDS

    val currentDelegate: Int get() = _delegate
    val currentMinHandDetectionConfidence: Float
        get() =
            _minHandDetectionConfidence
    val currentMinHandTrackingConfidence: Float
        get() =
            _minHandTrackingConfidence
    val currentMinHandPresenceConfidence: Float
        get() =
            _minHandPresenceConfidence
    //val currentMaxHands: Int get() = _maxHands

    fun setDelegate(delegate: Int) {
        _delegate = delegate
    }

    fun setMinHandDetectionConfidence(confidence: Float) {
        _minHandDetectionConfidence = confidence
    }
    fun setMinHandTrackingConfidence(confidence: Float) {
        _minHandTrackingConfidence = confidence
    }
    fun setMinHandPresenceConfidence(confidence: Float) {
        _minHandPresenceConfidence = confidence
    }

//    fun setMaxHands(maxResults: Int) {
//        _maxHands = maxResults
//    }
}