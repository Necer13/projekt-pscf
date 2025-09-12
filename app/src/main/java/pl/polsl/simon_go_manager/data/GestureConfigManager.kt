package pl.polsl.simon_go_manager.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject
import pl.polsl.simon_go_manager.model.GestureAction

class GestureConfigManager(context: Context) {

    private val prefs = context.getSharedPreferences("gesture_config", Context.MODE_PRIVATE)

    fun saveGestureAction(gestureName: String, action: GestureAction) {
        val json = JSONObject().apply {
            put("command", action.command)
            put("description", action.description)
            put("ipAddress", action.ipAddress)
        }.toString()

        prefs.edit {
            putString(gestureName, json)
        }
    }

    fun getGestureAction(gestureName: String): GestureAction? {
        val json = prefs.getString(gestureName, null) ?: return null
        val obj = JSONObject(json)
        val ipAddress = if (obj.has("ipAddress")) obj.getString("ipAddress") else ""
        return GestureAction(
            command = obj.getString("command"),
            description = obj.getString("description"),
            ipAddress = ipAddress
        )
    }

    fun getAllGestures(): Map<String, GestureAction> {
        val result = mutableMapOf<String, GestureAction>()

        for ((key, value) in prefs.all) {
            val jsonString = value as? String ?: continue
            val obj = JSONObject(jsonString)
            val command = obj.getString("command")
            val description = obj.getString("description")
            val ipAddress = if (obj.has("ipAddress")) obj.getString("ipAddress") else ""
            result[key] = GestureAction(command, description, ipAddress)
        }

        return result
    }

    fun removeGesture(gestureName: String) {
        prefs.edit {
            remove(gestureName)
        }
    }
}
