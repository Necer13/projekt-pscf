package pl.polsl.simon_go_manager.ui.devices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
//import androidx.compose.ui.geometry.isEmpty
//import androidx.compose.ui.semantics.text
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import pl.polsl.simon_go_manager.databinding.FragmentDevicesBinding
import pl.polsl.simon_go_manager.databinding.ItemDeviceBinding

class DevicesFragment : Fragment() {

    private var _binding: FragmentDevicesBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val deviceList = mutableListOf<Device>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val devicesViewModel =
            ViewModelProvider(this).get(DevicesViewModel::class.java)

        _binding = FragmentDevicesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textDashboard
        devicesViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Now you can access your button using the binding object
        // Assuming your button in fragment_devices.xml has an id like: android:id="@+id/myButton"
        binding.addDevButton.setOnClickListener {
            // Example: Add a new device (you'll have a proper way to do this)
            addNewDevice(
                Device(
                    "New Device ${deviceList.size + 1}",
                    DeviceType.SWITCH_D,
                    "192.168.1.${100 + deviceList.size}"
                )
            )
            Toast.makeText(requireContext(), "Add Device Clicked (Not Implemented)", Toast.LENGTH_SHORT).show()
        }
    }


    private fun populateInitialDevices() {
        // This is just sample data. In a real app, you'd get this from a database,
        // network API, or a ViewModel.
        if (deviceList.isEmpty()) { // Add only if the list is empty to avoid duplicates on config changes
            deviceList.add(Device("Living Room Light", DeviceType.DIMMER, "192.168.1.50"))
            deviceList.add(Device("Kitchen Thermostat", DeviceType.TERMOSTAT, "192.168.1.51"))
            deviceList.add(Device("Office Switch", DeviceType.SWITCH_D, "192.168.1.52"))
        }
    }

    private fun displayDevices() {
        // Clear any existing views in the container (important if you re-call this method)
        binding.devicesListContainer.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())

        for (device in deviceList) {
            // Inflate the item layout using ViewBinding for item_device.xml
            val itemBinding = ItemDeviceBinding.inflate(inflater, binding.devicesListContainer, false)

            // Populate the views in the item layout
            itemBinding.textViewDeviceName.text = device.name
            itemBinding.textViewDeviceType.text = "Type: ${device.type.name}" // .name gives the string representation of enum
            itemBinding.textViewDeviceIp.text = "IP: ${device.ipAddress}"

            // You can set an OnClickListener for each item if needed
            itemBinding.root.setOnClickListener {
                Toast.makeText(requireContext(), "Clicked on ${device.name}", Toast.LENGTH_SHORT).show()
                // Handle item click, e.g., navigate to a device detail screen
            }

            // Add the inflated and populated item view to the container
            binding.devicesListContainer.addView(itemBinding.root)
        }
    }

    // Example function to add a new device and refresh the list
    fun addNewDevice(device: Device) {
        deviceList.add(device)
        displayDevices() // Refresh the displayed list
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}