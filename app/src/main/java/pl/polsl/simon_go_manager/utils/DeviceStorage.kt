package pl.polsl.simon_go_manager.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pl.polsl.simon_go_manager.ui.devices.Device
import pl.polsl.simon_go_manager.ui.devices.DeviceDTO
import pl.polsl.simon_go_manager.ui.devices.DeviceType

object DeviceStorage {

    private const val PREF_NAME = "device_prefs"
    private const val KEY_DEVICES = "device_list"

    fun saveDevices(context: Context, devices: List<Device>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val dtoList = devices.map {
            DeviceDTO(
                name = it.name,
                type = it.type.name,
                ipAddress = it.ipAddress,
                value = it.value
            )
        }

        val json = Gson().toJson(dtoList)
        editor.putString(KEY_DEVICES, json)
        editor.apply()
    }


    fun loadDevices(context: Context): MutableList<Device> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_DEVICES, null) ?: return mutableListOf()

        val typeToken = object : TypeToken<MutableList<DeviceDTO>>() {}.type
        val deviceDTOs: MutableList<DeviceDTO> = Gson().fromJson(json, typeToken)

        return deviceDTOs.map {
            val deviceType = try {
                DeviceType.valueOf(it.type?.uppercase() ?: "")
            } catch (e: Exception) {
                DeviceType.SWITCH_D // domyślny typ gdy nie uda się sparsować
            }

            Device(
                name = it.name,
                type = deviceType,
                ipAddress = it.ipAddress,
                value = it.value ?: when(deviceType) {
                    DeviceType.SWITCH_D -> false
                    DeviceType.DIMMER -> 0
                    DeviceType.THERMOSTAT -> 20.0
                }
            )
        }.toMutableList()
    }

}
