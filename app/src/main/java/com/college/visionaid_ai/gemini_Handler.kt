package com.college.visionaid_ai

import android.hardware.biometrics.BiometricPrompt
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch


class gemini_Handler (private val apiKey: String) {

    private val model = GenerativeModel(
        modelName = "models/gemini-pro",
        apiKey = apiKey//"AIzaSyBOBrRyEoWgbFHEcY1hzdR9ewSbGBrqgdU"
    )

    suspend fun ask(prompt: String): String {
        return try {
            val response = model.generateContent(prompt)
            response.text ?: "Sorry, i didn't understand"
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message}"

        }
    }
}
/*
// Gemini function

private fun askGemini(prompt: String){

    val apiKey = "AIzaSyBOBrRyEoWgbFHEcY1hzdR9ewSbGBrqgdU"

    val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    lifecycleScope.launch {
        try {

            val response = model.generateContent(prompt)

            val result = response.text

            speak(result ?: "i didn't understand")

        } catch (e: Exception) {

            speak("Ai error error coming ")
        }
    }
}*/
