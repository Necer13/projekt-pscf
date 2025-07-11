package pl.polsl.simon_go_manager.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pl.polsl.simon_go_manager.ui.devices.Device

object DeviceStorage {

    private const val PREF_NAME = "device_prefs"
    private const val KEY_DEVICES = "device_list"

    fun saveDevices(context: Context, devices: List<Device>) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val json = Gson().toJson(devices)
        editor.putString(KEY_DEVICES, json)
        editor.apply()
    }

    fun loadDevices(context: Context): MutableList<Device> {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_DEVICES, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<Device>>() {}.type
        return Gson().fromJson(json, type)
    }
}
