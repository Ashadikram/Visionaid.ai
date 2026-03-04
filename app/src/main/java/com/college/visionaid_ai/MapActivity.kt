package com.college.visionaid_ai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.webkit.WebView
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.net.URL

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices


class MapActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_map)


        webView = findViewById<WebView>(R.id.web_main)

        webView.settings.javaScriptEnabled = true

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getLocation()
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(){

        if (ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION

        ) != PackageManager.PERMISSION_GRANTED
            ) {
            ActivityCompat.requestPermissions(
                this,arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),101
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->

            if (location != null){

                val lat = location.latitude
                val lon = location.longitude

                val mapUrl = "https://www.openstreetmap.org/#map=18/$lat/$lon"

                webView.loadUrl(mapUrl)
            } else {

                webView.loadUrl("https://www.openstreetmap.org")
            }
        }
    }
}