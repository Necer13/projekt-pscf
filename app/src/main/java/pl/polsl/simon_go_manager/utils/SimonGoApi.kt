package pl.polsl.simon_go_manager.utils

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.* // Keep this for OkHttpClient, Request, Call
import java.io.IOException

object SimonGoApi {
    private val client = OkHttpClient()
    // Handler do wykonywania kodu na głównym wątku UI
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    fun sendCommand(ip: String, endpoint: String, onComplete: ((Boolean, String?) -> Unit)? = null) {
        val url = "http://$ip/$endpoint"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback { // Make sure this is okhttp3.Callback
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SimonGoApi", "Błąd podczas wysyłania komendy: $url", e)
                // Wywołaj onComplete na głównym wątku
                mainThreadHandler.post {
                    onComplete?.invoke(false, "Błąd sieciowy: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) { // Corrected type here
                val successful = response.isSuccessful
                val responseMessage = if (successful) {
                    "Sukces"
                } else {
                    "Niepowodzenie - Kod: ${response.code}"
                }
                Log.i("SimonGoApi", "Odpowiedź dla $url: $responseMessage (Kod: ${response.code})")

                // Zawsze zamykaj ciało odpowiedzi, nawet jeśli go nie czytasz
                // aby zwolnić zasoby.
                response.body?.close() // Use response.body?.close() for okhttp3.Response

                // Wywołaj onComplete na głównym wątku
                mainThreadHandler.post {
                    onComplete?.invoke(successful, responseMessage)
                }
            }
        })
    }
}
