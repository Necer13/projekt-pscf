package pl.polsl.simon_go_manager.data

import pl.polsl.simon_go_manager.model.DefaultActions
import pl.polsl.simon_go_manager.model.GestureAction
import pl.polsl.simon_go_manager.ui.devices.DeviceType

object DefaultActions {

    val actions: List<DefaultActions> = listOf(
        // SWITCH
        DefaultActions("/s/0", "Wyłącz oba przekaźniki"),
        DefaultActions("/s/1", "Włącz oba przekaźniki"),
        DefaultActions("/s/2", "Zmień stan obu przekaźników"),
        DefaultActions("/s/0/0", "Wyłącz przekaźnik 1"),
        DefaultActions("/s/0/1", "Włącz przekaźnik 1"),
        DefaultActions("/s/1/0", "Wyłącz przekaźnik 2"),
        DefaultActions("/s/1/1", "Włącz przekaźnik 2"),

        // DIMMER
        DefaultActions("/s/00", "Jasność 0%"),
        DefaultActions("/s/19", "Jasność 10%"),
        DefaultActions("/s/33", "Jasność 20%"),
        DefaultActions("/s/4D", "Jasność 30%"),
        DefaultActions("/s/66", "Jasność 40%"),
        DefaultActions("/s/7F", "Jasność 50%"),
        DefaultActions("/s/99", "Jasność 60%"),
        DefaultActions("/s/B3", "Jasność 70%"),
        DefaultActions("/s/CC", "Jasność 80%"),
        DefaultActions("/s/E6", "Jasność 90%"),
        DefaultActions("/s/FF", "Jasność 100%"),
        DefaultActions("/s/inc/19", "Zwiększ jasność o 10%"),
        DefaultActions("/s/dec/19", "Zmniejsz jasność o 10%"),

        // THERMOSTAT
        DefaultActions("/s/0", "Wyłącz termostat"),
        DefaultActions("/s/1", "Włącz termostat"),
        DefaultActions("/s/1/t/2150", "Ustaw temperaturę 21.5°C"),
        DefaultActions("/s/1/t/2000", "Ustaw temperaturę 20.0°C"),
        DefaultActions("/s/t/inc/50", "Temperatura +0.5°C"),
        DefaultActions("/s/t/dec/50", "Temperatura -0.5°C"),
        DefaultActions("/s/3", "Tryb BOOST – szybkie grzanie")
    )
    fun getActionsForDeviceType(type: DeviceType): List<DefaultActions> {
        return when (type) {
            DeviceType.SWITCH_D -> actions.filter { it.command.startsWith("/s/") && !it.command.contains("t") }
            DeviceType.DIMMER -> actions.filter { it.description.contains("Jasność") || it.description.contains("jasność") }
            DeviceType.THERMOSTAT -> actions.filter { it.command.contains("t") || it.description.contains("termostat") || it.description.contains("BOOST") }
        }
    }
}
