package pl.polsl.simon_go_manager.ui.devices

data class Device(
    val name: String,
    val type: DeviceType,
    val ipAddress: String,
    var value: Any
)

enum class DeviceType {
    THERMOSTAT,
    SWITCH_D,
    DIMMER
}

data class DeviceDTO(
    val name: String,
    val type: String?,
    val ipAddress: String,
    val value: Any?
)