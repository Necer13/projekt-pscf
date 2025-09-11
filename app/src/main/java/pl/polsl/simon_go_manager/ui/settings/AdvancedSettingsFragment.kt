package pl.polsl.simon_go_manager.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import pl.polsl.simon_go_manager.data.DefaultActions
import pl.polsl.simon_go_manager.data.GestureConfigManager
import pl.polsl.simon_go_manager.databinding.FragmentAdvancedSettingsBinding
import pl.polsl.simon_go_manager.model.GestureAction

class AdvancedSettingsFragment : Fragment() {

    private var _binding: FragmentAdvancedSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var gestureConfigManager: GestureConfigManager

    // Lista gestów dostępnych w Mediapipe
    private val gestures = listOf(
        "Thumb_Up",
        "Thumb_Down",
        "Victory",
        "Closed_Fist",
        "Pointing_Up",
        "Pointing_Down"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdvancedSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gestureConfigManager = GestureConfigManager(requireContext())

        setupSpinners()
        setupButtons()
        refreshMappings() // od razu pokaż aktualne przypisania przy starcie
    }

    private fun setupSpinners() {
        // Spinner dla gestów
        val gestureAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            gestures
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.gestureSpinner.adapter = gestureAdapter

        // Spinner dla akcji
        val actionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            DefaultActions.actions.map { it.description }
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.actionSpinner.adapter = actionAdapter
    }

    private fun setupButtons() {
        // Zapisz przypisanie gest → akcja
        binding.saveMappingButton.setOnClickListener {
            val selectedGesture = gestures[binding.gestureSpinner.selectedItemPosition]
            val selectedAction = DefaultActions.actions[binding.actionSpinner.selectedItemPosition]

            gestureConfigManager.saveGestureAction(selectedGesture, selectedAction)
            Toast.makeText(
                requireContext(),
                "Zapisano akcję '${selectedAction.description}' dla gestu '$selectedGesture'",
                Toast.LENGTH_SHORT
            ).show()

            // odśwież widok zapisanych mapowań
            refreshMappings()
        }

        // Pokaż wszystkie przypisania (opcjonalne, bo i tak refreshMappings robi to samo)
        binding.showMappingsButton.setOnClickListener {
            refreshMappings()
        }
    }

    // Funkcja do odświeżania widoku mapowań
    private fun refreshMappings() {
        val allMappings = gestureConfigManager.getAllGestures()
        if (allMappings.isEmpty()) {
            binding.mappingTextView.text = "No mappings yet"
        } else {
            val builder = StringBuilder()
            for ((gesture, action) in allMappings) {
                builder.append("👉 $gesture → ${action.description} [${action.command}]\n")
            }
            binding.mappingTextView.text = builder.toString().trim()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
