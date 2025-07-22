import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import pl.polsl.simon_go_manager.databinding.FragmentHomeBinding
import pl.polsl.simon_go_manager.ui.devices.Device
import pl.polsl.simon_go_manager.ui.devices.DeviceType
import pl.polsl.simon_go_manager.utils.DeviceStorage
import pl.polsl.simon_go_manager.utils.SimonGoApi

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var deviceList: MutableList<Device>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        deviceList = DeviceStorage.loadDevices(requireContext())
        displayControls()
    }

    private fun displayControls() {
        binding.controlsContainer.removeAllViews()
        LayoutInflater.from(requireContext())

        for (device in deviceList) {
            val deviceLayout = LinearLayout(requireContext())
            deviceLayout.orientation = LinearLayout.VERTICAL
            deviceLayout.setPadding(16, 16, 16, 16)

            val title = TextView(requireContext())
            title.text = device.name
            title.textSize = 18f
            deviceLayout.addView(title)

            when (device.type) {
                DeviceType.SWITCH_D -> {
                    val switch = Switch(requireContext())
                    switch.isChecked = device.value as? Boolean ?: false
                    switch.setOnCheckedChangeListener { _, isChecked ->
                        device.value = isChecked
                        DeviceStorage.saveDevices(requireContext(), deviceList)
                        sendCommandToDevice(device, if (isChecked) "on" else "off")
                        Toast.makeText(requireContext(), "${device.name} set to $isChecked", Toast.LENGTH_SHORT).show()
                    }
                    deviceLayout.addView(switch)
                }
                DeviceType.DIMMER -> {
                    val seekBar = SeekBar(requireContext())
                    seekBar.max = 100
                    seekBar.progress = (device.value as? Int) ?: 0
                    val valueText = TextView(requireContext())
                    valueText.text = "Brightness: ${seekBar.progress}"

                    seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            valueText.text = "Brightness: $progress"
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            val brightness = seekBar?.progress ?: 0
                            device.value = brightness
                            DeviceStorage.saveDevices(requireContext(), deviceList)
                            sendCommandToDevice(device, "brightness/$brightness")
                            Toast.makeText(requireContext(), "${device.name} brightness set to $brightness", Toast.LENGTH_SHORT).show()
                        }
                    })

                    deviceLayout.addView(valueText)
                    deviceLayout.addView(seekBar)
                }
                DeviceType.THERMOSTAT -> {
                    val seekBar = SeekBar(requireContext())
                    seekBar.max = 30
                    seekBar.min = 10
                    seekBar.progress = ((device.value as? Double)?.toInt() ?: 20)
                    val valueText = TextView(requireContext())
                    valueText.text = "Temperature: ${seekBar.progress}°C"

                    seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            valueText.text = "Temperature: $progress°C"
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            val temp = seekBar?.progress ?: 20
                            device.value = temp.toDouble()
                            DeviceStorage.saveDevices(requireContext(), deviceList)
                            sendCommandToDevice(device, "temperature/$temp")
                            Toast.makeText(requireContext(), "${device.name} temperature set to $temp°C", Toast.LENGTH_SHORT).show()
                        }
                    })

                    deviceLayout.addView(valueText)
                    deviceLayout.addView(seekBar)
                }
            }

            binding.controlsContainer.addView(deviceLayout)
        }
    }

    private fun sendCommandToDevice(device: Device, endpoint: String) {
        // Zakładamy, że device.ip istnieje — jeśli nie, zmień na odpowiednie pole
        val ip = device.ipAddress
        if (ip.isBlank()) {
            Toast.makeText(requireContext(), "Brak IP dla urządzenia ${device.name}", Toast.LENGTH_SHORT).show()
            return
        }

        SimonGoApi.sendCommand(ip, endpoint) { success, message ->
            Toast.makeText(requireContext(), message ?: "Brak odpowiedzi", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
