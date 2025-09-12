package pl.polsl.simon_go_manager.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import okhttp3.*
import pl.polsl.simon_go_manager.databinding.FragmentHomeBinding
import pl.polsl.simon_go_manager.ui.devices.Device
import pl.polsl.simon_go_manager.ui.devices.DeviceType
import pl.polsl.simon_go_manager.utils.DeviceStorage
import java.io.IOException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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
        for (device in deviceList) {
            val deviceLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
            }

            val title = TextView(requireContext()).apply {
                text = device.name
                textSize = 18f
            }
            deviceLayout.addView(title)

            when (device.type) {
                DeviceType.SWITCH_D -> {
                    val switch = Switch(requireContext()).apply {
                        isChecked = device.value as? Boolean ?: false
                        setOnCheckedChangeListener { _, isChecked ->
                            device.value = isChecked
                            DeviceStorage.saveDevices(requireContext(), deviceList)

                            val ip = device.ipAddress ?: "192.168.1.100"
                            val command = if (isChecked) "/s/0/1" else "/s/0/0"
                            val url = "http://$ip$command"
                            sendCommandHttps(ip, command)
                            Toast.makeText(requireContext(), "Wysłano: $url", Toast.LENGTH_SHORT).show()
                        }
                    }
                    deviceLayout.addView(switch)
                }

                DeviceType.DIMMER -> {
                    val valueText = TextView(requireContext())
                    val seekBar = SeekBar(requireContext()).apply {
                        max = 100
                        progress = (device.value as? Int) ?: 0
                        valueText.text = "Brightness: $progress"

                        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                                valueText.text = "Brightness: $progress"
                            }

                            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                                val value = seekBar?.progress ?: 0
                                device.value = value
                                DeviceStorage.saveDevices(requireContext(), deviceList)

                                val ip = device.ipAddress ?: "192.168.1.100"
                                val command = "/s/$value"
                                val url = "http://$ip$command"
                                sendCommandHttps(ip, command)
                                Toast.makeText(requireContext(), "Wysłano: $url", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                    deviceLayout.addView(valueText)
                    deviceLayout.addView(seekBar)
                }

                DeviceType.THERMOSTAT -> {
                    val valueText = TextView(requireContext())
                    val seekBar = SeekBar(requireContext()).apply {
                        max = 20   // zakres 10–30°C
                        min = 0
                        progress = ((device.value as? Double)?.toInt() ?: 20) - 10
                        valueText.text = "Temperature: ${progress + 10}°C"

                        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                                valueText.text = "Temperature: ${progress + 10}°C"
                            }

                            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                                val temp = (seekBar?.progress ?: 10) + 10
                                device.value = temp.toDouble()
                                DeviceStorage.saveDevices(requireContext(), deviceList)

                                val ip = device.ipAddress ?: "192.168.1.100"
                                val command = "/s/1/t/${temp * 100}"
                                val url = "http://$ip$command"
                                sendCommandHttps(ip, command)
                                Toast.makeText(requireContext(), "Wysłano: $url", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                    deviceLayout.addView(valueText)
                    deviceLayout.addView(seekBar)
                }

                else -> {
                    val info = TextView(requireContext()).apply {
                        text = "Brak obsługi dla tego typu urządzenia."
                    }
                    deviceLayout.addView(info)
                }
            }

            binding.controlsContainer.addView(deviceLayout)
        }
    }

    private fun sendCommandHttps(ip: String, command: String) {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL").apply {
                init(null, trustAllCerts, java.security.SecureRandom())
            }
            val sslSocketFactory = sslContext.socketFactory

            val client = OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()

            val url = "http://$ip$command"
            val request = Request.Builder().url(url).build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    response.close()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
