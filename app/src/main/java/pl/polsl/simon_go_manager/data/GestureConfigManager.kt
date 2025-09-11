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
        }.toString()

        prefs.edit {
            putString(gestureName, json)
        }
    }

    fun getGestureAction(gestureName: String): GestureAction? {
        val json = prefs.getString(gestureName, null) ?: return null
        val obj = JSONObject(json)
        return GestureAction(
            command = obj.getString("command"),
            description = obj.getString("description")
        )
    }

    fun getAllGestures(): Map<String, GestureAction> {
        return prefs.all.mapNotNull { (key, value) ->
            if (value is String) {
                val obj = JSONObject(value)
                key to GestureAction(
                    command = obj.getString("command"),
                    description = obj.getString("description")
                )
            } else null
        }.toMap()
    }
}
