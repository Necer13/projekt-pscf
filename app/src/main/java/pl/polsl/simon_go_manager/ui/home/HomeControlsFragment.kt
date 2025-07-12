package pl.polsl.simon_go_manager.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import pl.polsl.simon_go_manager.R
import pl.polsl.simon_go_manager.ui.devices.Device
import pl.polsl.simon_go_manager.ui.devices.DeviceType
import pl.polsl.simon_go_manager.utils.DeviceStorage

class HomeControlsFragment : Fragment() {

    private lateinit var containerLayout: LinearLayout
    private lateinit var devices: MutableList<Device>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.fragment_home_controls, container, false)
        containerLayout = view.findViewById(R.id.containerLayout)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        devices = DeviceStorage.loadDevices(requireContext())
        renderDevices()
    }

    private fun renderDevices() {
        val context = requireContext()
        containerLayout.removeAllViews()

        for (device in devices) {
            val nameView = TextView(context).apply {
                text = device.name
                textSize = 18f
                setPadding(0, 16, 0, 8)
            }
            containerLayout.addView(nameView)

            when (device.type) {
                DeviceType.SWITCH_D -> {
                    val toggle = Switch(context).apply {
                        isChecked = device.value as? Boolean ?: false
                        setOnCheckedChangeListener { _, isChecked ->
                            device.value = isChecked
                            saveDevices()
                        }
                    }
                    containerLayout.addView(toggle)
                }

                DeviceType.DIMMER -> {
                    val label = TextView(context)
                    val seek = SeekBar(context).apply {
                        max = 100
                        progress = (device.value as? Double ?: 0.0).toInt()
                        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                                label.text = "Poziom: $progress%"
                                device.value = progress
                            }

                            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                                saveDevices()
                            }
                        })
                    }
                    label.text = "Poziom: ${seek.progress}%"
                    containerLayout.addView(seek)
                    containerLayout.addView(label)
                }

                DeviceType.THERMOSTAT -> {
                    val label = TextView(context)
                    val seek = SeekBar(context).apply {
                        max = 30
                        progress = (device.value as? Double ?: 20.0).toInt()
                        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                                label.text = "$progress°C"
                                device.value = progress
                            }

                            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                                saveDevices()
                            }
                        })
                    }
                    label.text = "${seek.progress}°C"
                    containerLayout.addView(seek)
                    containerLayout.addView(label)
                }
            }
        }
    }

    private fun saveDevices() {
        DeviceStorage.saveDevices(requireContext(), devices)
    }
}
