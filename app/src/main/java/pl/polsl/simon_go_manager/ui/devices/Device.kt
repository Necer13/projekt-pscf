package pl.polsl.simon_go_manager.ui.devices

data class Device(
    val name: String,
    val type: DeviceType,
    val ipAddress: String
)

enum class DeviceType {
    TERMOSTAT,
    SWITCH_D,
    DIMMER
}