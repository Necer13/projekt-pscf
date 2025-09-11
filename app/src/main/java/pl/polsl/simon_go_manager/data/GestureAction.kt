package pl.polsl.simon_go_manager.data

import pl.polsl.simon_go_manager.model.GestureAction

object DefaultActions {

    val actions: List<GestureAction> = listOf(
        // SWITCH
        GestureAction("/s/0", "Wyłącz oba przekaźniki"),
        GestureAction("/s/1", "Włącz oba przekaźniki"),
        GestureAction("/s/2", "Zmień stan obu przekaźników"),
        GestureAction("/s/0/0", "Wyłącz przekaźnik 1"),
        GestureAction("/s/0/1", "Włącz przekaźnik 1"),
        GestureAction("/s/1/0", "Wyłącz przekaźnik 2"),
        GestureAction("/s/1/1", "Włącz przekaźnik 2"),

        // DIMMER
        GestureAction("/s/00", "Jasność 0%"),
        GestureAction("/s/19", "Jasność 10%"),
        GestureAction("/s/33", "Jasność 20%"),
        GestureAction("/s/4D", "Jasność 30%"),
        GestureAction("/s/66", "Jasność 40%"),
        GestureAction("/s/7F", "Jasność 50%"),
        GestureAction("/s/99", "Jasność 60%"),
        GestureAction("/s/B3", "Jasność 70%"),
        GestureAction("/s/CC", "Jasność 80%"),
        GestureAction("/s/E6", "Jasność 90%"),
        GestureAction("/s/FF", "Jasność 100%"),
        GestureAction("/s/inc/19", "Zwiększ jasność o 10%"),
        GestureAction("/s/dec/19", "Zmniejsz jasność o 10%"),

        // THERMOSTAT
        GestureAction("/s/0", "Wyłącz termostat"),
        GestureAction("/s/1", "Włącz termostat"),
        GestureAction("/s/1/t/2150", "Ustaw temperaturę 21.5°C"),
        GestureAction("/s/1/t/2000", "Ustaw temperaturę 20.0°C"),
        GestureAction("/s/t/inc/0A", "Temperatura +1°C"),
        GestureAction("/s/t/dec/0A", "Temperatura -1°C"),
        GestureAction("/s/3", "Tryb BOOST – szybkie grzanie")
    )
}
