package pl.polsl.simon_go_manager.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import pl.polsl.simon_go_manager.R
import DeviceConfiguration
import GestureAction
import pl.polsl.simon_go_manager.utils.ConfigurationStorage

class AdvancedSettingsFragment : Fragment() {

    private val configList: MutableList<DeviceConfiguration> by lazy {
        ConfigurationStorage.load(requireContext()).toMutableList()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_advanced_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(view) {
        val gestureInput = findViewById<EditText>(R.id.gestureInput)
        val actionInput = findViewById<EditText>(R.id.actionInput)
        val ipInput = findViewById<EditText>(R.id.deviceIpInput)
        val saveButton = findViewById<Button>(R.id.saveButton)

        saveButton.setOnClickListener {
            val gesture = gestureInput.text.toString().trim()
            val action = actionInput.text.toString().trim()
            val ip = ipInput.text.toString().trim()

            if (gesture.isBlank() || action.isBlank() || ip.isBlank()) {
                Toast.makeText(context, "Wszystkie pola są wymagane", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val actionEnum = GestureAction.entries.find { it.displayName == action }
            if (actionEnum == null) {
                Toast.makeText(context, "Nieznana akcja: $action", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val existingConfig = configList.find { it.deviceName == ip }
            if (existingConfig != null) {
                // zakładam, że chcesz nadpisać konfigurację — dostosuj jeśli ma być inaczej
                configList.remove(existingConfig)
            }
            configList.add(DeviceConfiguration(ip, gesture, actionEnum))


            ConfigurationStorage.save(requireContext(), configList)
            Toast.makeText(context, "Zapisano konfigurację", Toast.LENGTH_SHORT).show()

            gestureInput.text.clear()
            actionInput.text.clear()
            ipInput.text.clear()
        }
    }
}
