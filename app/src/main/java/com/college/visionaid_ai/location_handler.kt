package com.college.visionaid_ai

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class location_handler (
    private val activity: AppCompatActivity,
    private val onLocationReceived: (String) -> Unit
){
    private val speechLauncher = activity.registerForActivityResult (
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == AppCompatActivity.RESULT_OK) {

            val resultList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

            val location = resultList?.getOrNull(0)?.lowercase()

            if (!location.isNullOrEmpty()){
                onLocationReceived(location)
            }
        }
    }

    fun startListening(){

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Locale.getDefault()
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            "Speaking Destination"
        )

        speechLauncher.launch(intent)
    }
}