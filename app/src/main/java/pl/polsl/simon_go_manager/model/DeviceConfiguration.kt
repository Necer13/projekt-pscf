//Thumb_Up
//Thumb_Down
//Victory
//Closed_Fist
//Open_Palm
//Pointing_Up
enum class GestureAction(val displayName: String) {
    TOGGLE_LIGHT("Włącz/Wyłącz światło"),
    DIM_LIGHT("Ściemnij światło"),
    SET_TEMPERATURE("Ustaw temperaturę");

    override fun toString(): String = displayName

}

data class DeviceConfiguration(
    val deviceName: String,
    val gesture: String,
    val action: GestureAction
)
