package pl.polsl.simon_go_manager.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import pl.polsl.simon_go_manager.R
import pl.polsl.simon_go_manager.data.GestureConfigManager
import pl.polsl.simon_go_manager.databinding.FragmentAdvancedSettingsBinding
import pl.polsl.simon_go_manager.model.GestureAction
import pl.polsl.simon_go_manager.ui.devices.Device
import pl.polsl.simon_go_manager.utils.DeviceStorage
import org.json.JSONObject
import pl.polsl.simon_go_manager.data.DefaultActions
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class AdvancedSettingsFragment : Fragment() {

    private var _binding: FragmentAdvancedSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var gestureConfigManager: GestureConfigManager
    private lateinit var devices: MutableList<Device>

    // Lista gestów: pierwsze = klucz wewnętrzny, drugie = nazwa do pokazania w UI
    private val gesturesList = listOf(
        "Thumb_Up" to "Kciuk w górę",
        "Thumb_Down" to "Kciuk w dół",
        "Victory" to "Zwycięstwo (V)",
        "Closed_Fist" to "Zaciśnięta pięść",
        "Pointing_Up" to "Wskazanie w górę",
        "Open_Palm" to "Otwarta dłoń"
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
        devices = DeviceStorage.loadDevices(requireContext())

        setupSpinners()
        setupButtons()
        refreshMappings()
    }

    private fun setupSpinners() {
        // Spinner dla gestów (pokazujemy nazwy po polsku)
        val gestureAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            gesturesList.map { it.second }
        )
        gestureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.gestureSpinner.adapter = gestureAdapter

        // Spinner dla urządzeń
        val deviceNames = devices.map { it.name }
        val deviceAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            deviceNames
        )
        deviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.deviceSpinner.adapter = deviceAdapter

        // Po wyborze urządzenia ustaw spinner akcji
        binding.deviceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                val selectedDevice = devices[position]
                val actions = getActionsForDevice(selectedDevice)
                val actionAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    actions.map { it.description }
                )
                actionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.actionSpinner.adapter = actionAdapter
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupButtons() {
        // Zapis mapowania gest → akcja
        binding.saveMappingButton.setOnClickListener {
            val selectedIndex = binding.gestureSpinner.selectedItemPosition
            val selectedGestureInternal = gesturesList[selectedIndex].first
            val selectedGestureDisplayName = gesturesList[selectedIndex].second

            val selectedDevice = devices[binding.deviceSpinner.selectedItemPosition]
            val actions = getActionsForDevice(selectedDevice)
            val selectedAction = actions[binding.actionSpinner.selectedItemPosition]

            val gestureAction = GestureAction(
                command = selectedAction.command,
                description = selectedAction.description,
                ipAddress = selectedDevice.ipAddress
            )

            gestureConfigManager.saveGestureAction(selectedGestureInternal, gestureAction)

            Toast.makeText(
                requireContext(),
                "Zapisano: $selectedGestureDisplayName → ${selectedDevice.name} (${selectedAction.description})",
                Toast.LENGTH_SHORT
            ).show()
            refreshMappings()
        }

        // Eksport konfiguracji gestów
        binding.exportButton.setOnClickListener {
            val data = JSONObject(gestureConfigManager.getAllGestures().mapValues {
                JSONObject().apply {
                    put("command", it.value.command)
                    put("description", it.value.description)
                    put("ipAddress", it.value.ipAddress)
                }
            }).toString(2)

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "gestures_config.json")
            }
            startActivityForResult(intent, 1001)
        }

        // Import konfiguracji gestów
        binding.importButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            startActivityForResult(intent, 1002)
        }
    }

    private fun getActionsForDevice(device: Device) =
        DefaultActions.getActionsForDeviceType(device.type)

    private fun refreshMappings() {
        binding.mappingsContainer.removeAllViews()
        val mappings = gestureConfigManager.getAllGestures()

        if (mappings.isEmpty()) {
            val tv = TextView(requireContext()).apply { text = "Brak zapisanych mapowań" }
            binding.mappingsContainer.addView(tv)
            return
        }

        for ((gesture, action) in mappings) {
            val displayName = gesturesList.firstOrNull { it.first == gesture }?.second ?: gesture
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            val tv = TextView(requireContext()).apply {
                text = "$displayName → ${action.description} [${action.ipAddress}${action.command}]"
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val btn = Button(requireContext()).apply {
                text = "Usuń"
                setOnClickListener {
                    gestureConfigManager.removeGesture(gesture)
                    refreshMappings()
                }
            }
            row.addView(tv)
            row.addView(btn)
            binding.mappingsContainer.addView(row)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data?.data == null) return
        val uri: Uri = data.data!!

        when (requestCode) {
            1001 -> {
                requireContext().contentResolver.openOutputStream(uri)?.use {
                    OutputStreamWriter(it).use { writer ->
                        val dataJson = JSONObject(gestureConfigManager.getAllGestures().mapValues {
                            JSONObject().apply {
                                put("command", it.value.command)
                                put("description", it.value.description)
                                put("ipAddress", it.value.ipAddress)
                            }
                        }).toString(2)
                        writer.write(dataJson)
                    }
                }
            }
            1002 -> {
                requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                    val text = BufferedReader(InputStreamReader(stream)).readText()
                    val obj = JSONObject(text)
                    for (key in obj.keys()) {
                        val entry = obj.getJSONObject(key)
                        gestureConfigManager.saveGestureAction(
                            key,
                            GestureAction(
                                entry.getString("command"),
                                entry.getString("description"),
                                entry.getString("ipAddress")
                            )
                        )
                    }
                    refreshMappings()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
