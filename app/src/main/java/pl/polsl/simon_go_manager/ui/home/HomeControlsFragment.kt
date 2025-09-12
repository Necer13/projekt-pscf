package pl.polsl.simon_go_manager.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import okhttp3.*
import pl.polsl.simon_go_manager.R
import pl.polsl.simon_go_manager.ui.devices.Device
import pl.polsl.simon_go_manager.ui.devices.DeviceType
import pl.polsl.simon_go_manager.utils.DeviceStorage
import java.io.IOException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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
                    val relayCount = 4
                    val ip = device.ipAddress ?: "192.168.1.100"

                    val states = (device.value as? MutableList<Boolean>)
                        ?: MutableList(relayCount) { false }

                    for (i in 0 until relayCount) {
                        val switch = Switch(context).apply {
                            text = "Przekaźnik ${i + 1}"
                            isChecked = states[i]
                            setOnCheckedChangeListener { _, isChecked ->
                                states[i] = isChecked
                                device.value = states
                                saveDevices()

                                val command = when (i) {
                                    0 -> if (isChecked) "/s/0/1" else "/s/0/0"
                                    1 -> if (isChecked) "/s/1/1" else "/s/1/0"
                                    2 -> if (isChecked) "/s/2/1" else "/s/2/0"
                                    3 -> if (isChecked) "/s/3/1" else "/s/3/0"
                                    else -> ""
                                }
                                val url = "http://$ip$command"
                                sendCommandHttps(ip, command)
                                Toast.makeText(context, "Wysłano: $url", Toast.LENGTH_SHORT).show()
                            }
                        }
                        containerLayout.addView(switch)
                    }
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
                                val ip = device.ipAddress
                                val command = "/s/$progress"
                                val url = "http://$ip$command"
                                sendCommandHttps(ip, command)
                                Toast.makeText(context, "Wysłano: $url", Toast.LENGTH_SHORT).show()
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
                                val ip = device.ipAddress
                                val command = "/s/1/t/${progress * 100}"
                                val url = "http://$ip$command"
                                sendCommandHttps(ip, command)
                                Toast.makeText(context, "Wysłano: $url", Toast.LENGTH_SHORT).show()
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

    private fun sendCommandHttps(ip: String, command: String) {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            val client = OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()

            val url = "http://$ip$command"

            val request = Request.Builder()
                .url(url)
                .build()

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
}
