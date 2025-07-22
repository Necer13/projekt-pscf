package pl.polsl.simon_go_manager.ui.devices

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import pl.polsl.simon_go_manager.databinding.FragmentDevicesBinding
import pl.polsl.simon_go_manager.databinding.ItemDeviceBinding
import pl.polsl.simon_go_manager.R
import pl.polsl.simon_go_manager.utils.DeviceStorage

class DevicesFragment : Fragment() {

    private var _binding: FragmentDevicesBinding? = null
    private val binding get() = _binding!!
    private lateinit var deviceList: MutableList<Device>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val devicesViewModel = ViewModelProvider(this).get(DevicesViewModel::class.java)
        _binding = FragmentDevicesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        devicesViewModel.text.observe(viewLifecycleOwner) {
            binding.textDashboard.text = it
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addDevButton.setOnClickListener {
            showAddDeviceDialog()
        }

        deviceList = DeviceStorage.loadDevices(requireContext())
        val hardcodedDevice = Device(
            name = "Ściemniacz",
            type = DeviceType.DIMMER,
            ipAddress = "192.168.1.111",
            value = false
        )

        if (deviceList.none { it.name == hardcodedDevice.name && it.ipAddress == hardcodedDevice.ipAddress }) {
            deviceList.add(hardcodedDevice)
            DeviceStorage.saveDevices(requireContext(), deviceList)
        }
        displayDevices()
    }

    private fun showAddDeviceDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_device, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.deviceNameInput)
        val ipInput = dialogView.findViewById<EditText>(R.id.deviceIpInput)
        val typeSpinner = dialogView.findViewById<Spinner>(R.id.deviceTypeSpinner)

        val deviceTypes = DeviceType.entries.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, deviceTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = adapter

        AlertDialog.Builder(requireContext())
            .setTitle("Dodaj urządzenie")
            .setView(dialogView)
            .setPositiveButton("Dodaj") { _, _ ->
                val name = nameInput.text.toString().trim()
                val ip = ipInput.text.toString().trim()
                val typeName = typeSpinner.selectedItem.toString()

                if (name.isBlank() || ip.isBlank()) {
                    Toast.makeText(requireContext(), "Wszystkie pola są wymagane", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (!android.util.Patterns.IP_ADDRESS.matcher(ip).matches()) {
                    Toast.makeText(requireContext(), "Niepoprawny adres IP", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                try {
                    val type = DeviceType.valueOf(typeName)
                    val defaultValue: Any = when (type.toString().lowercase()) {
                        "switch" -> false
                        "dimmer" -> 0
                        "thermostat" -> 20.0
                        else -> ""
                    }
                    val newDevice = Device(name, type, ip, defaultValue)
                    addNewDevice(newDevice)
                    Toast.makeText(requireContext(), "Dodano: $name", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Nieznany typ urządzenia", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun addNewDevice(device: Device) {

        deviceList.add(device)
        DeviceStorage.saveDevices(requireContext(), deviceList)
        displayDevices()
    }

    private fun displayDevices() {
        binding.devicesListContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        for ((index, device) in deviceList.withIndex()) {
            val itemBinding = ItemDeviceBinding.inflate(inflater, binding.devicesListContainer, false)

            itemBinding.textViewDeviceName.text = device.name
            itemBinding.textViewDeviceType.text = "Typ: ${device.type.name}"
            itemBinding.textViewDeviceIp.text = "IP: ${device.ipAddress}"

            itemBinding.root.setOnClickListener {
                Toast.makeText(requireContext(), "Kliknięto: ${device.name}", Toast.LENGTH_SHORT).show()
            }

            itemBinding.buttonDeleteDevice.setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Usuń urządzenie")
                    .setMessage("Czy na pewno chcesz usunąć urządzenie ${device.name}?")
                    .setPositiveButton("Tak") { _, _ ->
                        deviceList.removeAt(index)
                        DeviceStorage.saveDevices(requireContext(), deviceList)
                        displayDevices()
                        Toast.makeText(requireContext(), "Usunięto urządzenie", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Nie", null)
                    .show()
            }

            binding.devicesListContainer.addView(itemBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
