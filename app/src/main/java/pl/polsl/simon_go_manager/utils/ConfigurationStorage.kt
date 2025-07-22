package pl.polsl.simon_go_manager.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import DeviceConfiguration
import java.io.File

object ConfigurationStorage {
    private const val FILE_NAME = "advanced_config.json"

    fun save(context: Context, configs: List<DeviceConfiguration>) {
        val file = File(context.filesDir, FILE_NAME)
        val json = Gson().toJson(configs)
        file.writeText(json)
    }

    fun load(context: Context): List<DeviceConfiguration> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        val json = file.readText()
        val type = object : TypeToken<List<DeviceConfiguration>>() {}.type
        return Gson().fromJson(json, type)
    }
}
