package pl.polsl.simon_go_manager.ui.devices

data class Device(
    val name: String,
    val type: DeviceType, // Using an enum for type safety
    val ipAddress: String
)

enum class DeviceType {
    TERMOSTAT,
    SWITCH_D,
    DIMMER
}