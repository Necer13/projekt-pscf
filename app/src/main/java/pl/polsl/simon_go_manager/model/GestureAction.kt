package pl.polsl.simon_go_manager.model

data class GestureAction(
    val command: String,
    val description: String,
    val ipAddress: String

)

data class DefaultActions(
    val command: String,
    val description: String
)