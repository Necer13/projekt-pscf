package pl.polsl.simon_go_manager.ui.home

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

        // Załaduj urządzenia
        deviceList = DeviceStorage.loadDevices(requireContext())
        displayControls()
    }

    private fun displayControls() {
        binding.controlsContainer.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())

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
                            device.value = seekBar?.progress ?: 0
                            DeviceStorage.saveDevices(requireContext(), deviceList)
                            Toast.makeText(requireContext(), "${device.name} brightness set to ${device.value}", Toast.LENGTH_SHORT).show()
                        }
                    })

                    deviceLayout.addView(valueText)
                    deviceLayout.addView(seekBar)
                }
                DeviceType.THERMOSTAT -> {
                    val seekBar = SeekBar(requireContext())
                    seekBar.max = 30  // Max temp 30°C
                    seekBar.min = 10  // Min temp 10°C (API 26+)
                    seekBar.progress = ((device.value as? Double)?.toInt() ?: 20) - 10
                    val valueText = TextView(requireContext())
                    valueText.text = "Temperature: ${seekBar.progress + 10}°C"

                    seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            valueText.text = "Temperature: ${progress + 10}°C"
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            val temp = (seekBar?.progress ?: 10) + 10
                            device.value = temp.toDouble()
                            DeviceStorage.saveDevices(requireContext(), deviceList)
                            Toast.makeText(requireContext(), "${device.name} temperature set to $temp°C", Toast.LENGTH_SHORT).show()
                        }
                    })

                    deviceLayout.addView(valueText)
                    deviceLayout.addView(seekBar)
                }
                else -> {
                    val info = TextView(requireContext())
                    info.text = "Brak obsługi dla tego typu urządzenia."
                    deviceLayout.addView(info)
                }
            }

            binding.controlsContainer.addView(deviceLayout)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
