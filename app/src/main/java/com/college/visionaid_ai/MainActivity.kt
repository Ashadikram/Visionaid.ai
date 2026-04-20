package com.college.visionaid_ai

import org.tensorflow.lite.task.vision.detector.ObjectDetector
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ops.ResizeOp
import  android.speech.tts.TextToSpeech
import java.util.*

import android.Manifest
import android.adservices.adid.AdId
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ForwardingImageProxy
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.ByteArrayOutputStream
import android.graphics.*
import android.hardware.biometrics.BiometricPrompt
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.ContactsContract
import android.speech.RecognizerIntent
import android.util.Log
import android.view.MotionEvent
import android.view.ViewOverlay
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.ToggleButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.android.material.switchmaterial.SwitchMaterial
import org.w3c.dom.Text

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.common.model.DownloadConditions

import android.speech.SpeechRecognizer
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.SharingCommand
import kotlinx.coroutines.launch
import java.util.Locale


@OptIn(androidx.camera.core.ExperimentalGetImage::class)

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var objectDetector: ObjectDetector

    private lateinit var overlay: OverlayView

    private lateinit var vibrator: Vibrator

    private lateinit var textToSpeech: TextToSpeech
    private var lastSpokenObject: String? = null
    private var lastSpeakTime = 0L

    private var lastVibratTime = 0L

    private var speechEnabled = true
    private var vibrationEnabled = true
    private var alertDistance = 50

    private var isBackCamera = true
    private lateinit var cameraProvider: ProcessCameraProvider

    private var isListening = false
    private var isDetectionEnabled = true
    private var isSpeechEnabled = true
    private var allowListning = true

    private var voiceMode = "normal"

    private var ListenMode = "none"

    private lateinit var btnbot: ImageButton

    private lateinit var translator: Translator
    private var selectedLanguageCode = TranslateLanguage.HINDI

    private lateinit var speechLauncher: ActivityResultLauncher<Intent>

    private lateinit var locationHandler: location_handler

    private lateinit var geminiHandler: gemini_Handler

    private lateinit var calculatorHandler: CalculatorHandler
    private lateinit var appOpenHandler: AppOpenHandler

    private fun speak(text: String, onDone: (() -> Unit)? = null){

        val utteranceId = "visionaid_tts"

        textToSpeech.setOnUtteranceProgressListener(object  : UtteranceProgressListener() {

            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                runOnUiThread {
                    onDone?.invoke()
                }
            }

            override fun onError(utteranceId: String?) {}
        })
        // If English selected, speak directly
        if (selectedLanguageCode == TranslateLanguage.ENGLISH ) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null,utteranceId)
            return
        }

        // Translate text first
        translator.translate(text)
            .addOnSuccessListener { translatedText ->
                textToSpeech.speak(translatedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            }
            .addOnFailureListener {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null,utteranceId)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // UI btn code
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Translation code
        initTranslator(selectedLanguageCode)

        val btnLanguage = findViewById<ImageButton>(R.id.btnLanguage)

        btnLanguage.setOnClickListener {
            showLanguageDialog()
        }


        val toggleSpeech = findViewById<SwitchMaterial>(R.id.toggleSpeech)
        val toggleVibration = findViewById<SwitchMaterial>(R.id.toggleVibration)
        val seekDistance = findViewById<SeekBar>(R.id.seekDistance)
        val tvDistanceLabel = findViewById<TextView>(R.id.tvDistanceLabel)

        toggleSpeech.isChecked = true
        toggleVibration.isChecked = true

        toggleSpeech.setOnCheckedChangeListener { _, isChecked ->
            speechEnabled = isChecked
        }

        toggleVibration.setOnCheckedChangeListener { _, isChecked ->
            vibrationEnabled = isChecked
        }

        seekDistance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                alertDistance = progress
                tvDistanceLabel.text = "Alert Distance: $alertDistance cm"
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        })

        val btnSwitchCamera = findViewById<ImageButton>(R.id.btnSwitchCamera)

        btnSwitchCamera.setOnClickListener {
            isBackCamera = !isBackCamera
            bindCameraUseCase()
        }

        //calculator function
        calculatorHandler = CalculatorHandler()
        appOpenHandler = AppOpenHandler(this)

        //Gemini handling function

        geminiHandler = gemini_Handler("AIzaSyAS3JccG_Mg8NPXWD5bqDVATfAc8dGIIag")

        // UI switch first ui to second ui

        val locationBtn = findViewById<ImageButton>(R.id.btnLocation)

        locationBtn.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

        // Vibration code

        overlay = findViewById(R.id.overlay)

        vibrator = getSystemService(Vibrator::class.java)
        if (!vibrator.hasVibrator()){
            Log.d("VIBRATION", "Device has no vibrator")
        }
        vibrateWarning()

        previewView = findViewById(R.id.previewView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        //Mic Button

        locationHandler = location_handler(this) { location ->

            speak("Navigating yo $location")
            openNavigation(location)
        }

        val micButton = findViewById<ImageButton>(R.id.btnMic)

        micButton.setOnClickListener {
            isListening = true
            allowListning = true
            speak("Voice command activated. where do you want to go")
            locationHandler.startListening()
        }

        locationHandler.startListening()


        // That's a map permission code
        if (ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
            ){
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),200
            )
        }

        //text to speech converter

        textToSpeech = TextToSpeech(this){
            if (it == TextToSpeech.SUCCESS){
                textToSpeech.language = Locale.ENGLISH
                speak("Where do you want to go")
            }
        }

        //Speech launcher code
        speechLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK){

                val resultList = result.data?.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                )

                val locationName = resultList?.getOrNull(0)?.lowercase()

                //val command = resultList?.getOrNull(0)

                if (!locationName.isNullOrEmpty()){
                    speak("Navigating to $locationName")
                    openNavigation(locationName)

             /*   if (command != null){
                    speak("You said $command")
                    askGemini(command)*/

                } else {
                    speak("Please say a destination")
                }
            }
        }
        //before there is map function
        //getCurrentLocation()


        // Bot listening button
        btnbot = findViewById<ImageButton>(R.id.btnbot)

        btnbot.setOnClickListener {

            speak("Listening to command")
            startVoiceCommand()
        }

        // voice search permission code

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE
            ),1
        )

        // voice permission launcher
       speechLauncher  = registerForActivityResult (ActivityResultContracts.StartActivityForResult()) {
            result ->

            if (result.resultCode == RESULT_OK) {

                val data = result.data
                val result =
                    data?.getStringArrayListExtra((RecognizerIntent.EXTRA_RESULTS))

                val command = result?.get(0)?.lowercase()

                if (command == null) {
                    speak("I didn't hear anything")
                    return@registerForActivityResult
                }
                // Send command to the processor
                processVoiceCommand(command)
            }
        }

        //Camera Permission Granted code

        if (ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        )== PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }

        val options = ObjectDetector.ObjectDetectorOptions.builder().setScoreThreshold(0.5f).setMaxResults(5).build()

        objectDetector = ObjectDetector.createFromFileAndOptions(
            this, "efficientdet_lite1.tflite", options
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                startCamera()
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {

        if (isListening){

            //stop listening logic
            isListening = false
            allowListning = false
            isDetectionEnabled = true

            textToSpeech.stop()
            speak("listening stopped")

        }
        return super.dispatchTouchEvent(ev)
    }

    //gemini function
    private fun askGemini(prompt: String){

        lifecycleScope.launch {

            try {
                speak("Thinking...")
                val result = geminiHandler.ask(prompt)
                Log.d("Gemini", "Response: $result")
                speak(result)
            } catch (e: Exception) {
                e.printStackTrace()
                speak("Error while using Ai")
            }
        }
    }

    // Voice Listening Function

    private fun startVoiceCommand(){

        allowListning = true

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
            RecognizerIntent.EXTRA_PROMPT, "Speak now"
        )

        speechLauncher.launch(intent)
    }

    // Voice Command process

    private fun processVoiceCommand(command: String?) {

        if (command == null) {
            speak("command not recognized")
            return
        }

        val text = command.lowercase()

        // step 1: if we are waiting for contact name
        if (voiceMode == "call") {

            voiceMode = "normal"

            val name = text
                .replace("call", "")
                .replace("whatsapp", "")
                .trim()

            callContact(name)

            return
        }

        // step 2: if we are waiting for whatsapp call
        if (voiceMode == "whatsapp"){

            voiceMode = "normal"

            val name = text.replace("call","").trim()

            callWhatsApp(name)

            return
        }

        // step 3 : if we are waiting for YouTube search
        if (voiceMode == "youtube"){

            voiceMode = "normal"

            searchYoutube(text)

            return

        // step 4 : if user wanna set location
        if (voiceMode == "location" && text.contains("map")){

            voiceMode = "normal"
            val place = text
                .replace("navigate to","")
                .replace("location","")
                .replace("map","")
            openNavigation(place)
            return
             }
        // step 5 : if user wanna use calculation

        if (voiceMode == "sara" && text.contains("sarah")){
            voiceMode = "normal"
            val cleand = text
                .replace("sara","").replace("sarah","").trim()
            askGemini(cleand)
        }
        }

        when {

            text.contains("whatsapp") && text.contains("whatsapp call") -> {

                voiceMode = "whatsapp"

                Handler(Looper.getMainLooper()).postDelayed({
                    speak("Who do you wanna call on whatsapp")
                }, 200)

                Handler(Looper.getMainLooper()).postDelayed({
                    startVoiceCommand()
                }, 1200)
            }


            text.contains("call") -> {

                voiceMode = "call"

                speak(" Who do you want to call ")
                startVoiceCommand()
            }

            text.contains("youtube") || text.contains("play video") -> {

                voiceMode = "youtube"

                speak("which YouTube video you wanna to watch")
                startVoiceCommand()
            }

            text.contains("location") || text.contains("map") || text.contains("set location") ->{

                val place = text
                    .replace("location","")
                    .replace("map","")
                    .replace("set location","")

                speak("Navigating to $place")
                //startListening()
                openNavigation(place)
            }

            text.contains("calculator") -> {
                val result = appOpenHandler.openCalculator()
                speak(result)
                return
            }

            text.contains("calculate") || text.contains("what is") || text.contains("sara what is") -> {
                val result = calculatorHandler.calculate(text)
                speak(result)
                return
            }

            text.contains("google") -> {
                val query = text.replace("google","").trim()
                searchOnGoogle(query)
                return
            }

            text.startsWith("open")  -> {
                val appName = text.replace("open", "").trim()
                openAnyApp(appName)
                return
            }

            text.contains("sara") || text.contains("sarah") || text.contains("hey") || text.contains("hello")->{

                val cleaned = text
                    .replace("sara","")
                    .replace("sarah","")
                    .replace("hey","")
                    .replace("hello","").trim()
                if (cleaned.isNotEmpty()) {
                    askGemini(cleaned)
                } else {
                    speak("Yes, how can i help you")
                }
                speak("hello...")
               // askGemini(text)
                startVoiceCommand()
            }

            else -> {
                searchOnGoogle(text)
                //askGemini(text)
                //speak("Command not recognized tho you missed something")
            }
        }
    }

    // Call contact function

    private  fun  callContact (name: String?){

        if (name == null || name.isEmpty()){
            speak("Contact name not detected")
            return
        }

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.READ_CONTACTS),1
            )

            speak("Please allow contacts permission")
            return
        }

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI

        val cursor = contentResolver.query(
            uri,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
            arrayOf("%$name%"),null
        )
        if (cursor != null && cursor.moveToFirst()) {

            val numberIndex =
                cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
            val phoneNumber = cursor.getString(numberIndex)

            val intent = Intent(Intent.ACTION_CALL)

            intent.data = Uri.parse("tel:$phoneNumber")

            startActivity(intent)

            speak("Calling $name")

            cursor.close()

        } else {

            speak("Contact not found ")
        }
    }

    //WhatsApp call function

    private fun  callWhatsApp(name: String?){

        if (name == null || name.isEmpty()) {
            speak("Contact name not detected")
            return
        }

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI

        val cursor = contentResolver.query(
            uri,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME}   LIKE ?",
            arrayOf("%$name%"),
            null
        )

        if (cursor != null && cursor.moveToFirst()) {

            val numberIndex = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            val phoneNumber = cursor.getString(numberIndex).replace(" ", "").replace("-","")

            val cleanNumber = phoneNumber.replace("+","")

            val intent = Intent(Intent.ACTION_VIEW)

            intent.data = Uri.parse("https://wa.me/$cleanNumber")

            intent.setPackage("com.whatsapp")

            startActivity(intent)

            speak("Opening WhatsApp call for $name")

            cursor.close()
        } else {
            speak("Contact not found")
        }
    }

    // YouTube Search Function

    private  fun  searchYoutube( query: String?){

        if (query == null) return

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/results?search_query=$query")
        )

        intent.setPackage("com.google.android.youtube")

        startActivity(intent)

        speak("Playing $query on YouTube")
    }

    private fun searchOnGoogle(query: String) {

        if (query.isEmpty()) {
            speak("what do you want to search?")
            return
        }
        //if (query == null) return

        try {
            val encodedQuery = Uri.encode(query)

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=$encodedQuery")
            )
            startActivity(intent)
            speak("Search Google for $query")
        } catch (e: Exception) {
        speak("Unable to search")
        }
    }
/*
    private fun openAnyApp(appName: String) {

        val packageName = getPackageNameFromAppName(appName)

        if (packageName.isEmpty()) {
            speak("App not found")
            return
        }
        try {
            val packageManager = packageManager

            val intent = packageManager.getLaunchIntentForPackage(
                getPackageNameFromAppName(appName)
            )

            if (intent != null) {
                startActivity(intent)
                speak("opening $appName")
            } else {
                // if app not found -> play Store
                speak("App not found , searching on play Store")

            }
        } catch (e: Exception) {
            speak("Unable to open $appName")
            e.printStackTrace()
        }
    } */

    private fun openAnyApp(appName: String) {

        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val apps = pm.queryIntentActivities(intent, 0)

        for (app in apps) {
            val name = app.loadLabel(pm).toString()

            val cleanAppName = appName
                .replace("open","")
                .replace("app","")
                .replace("application","")
                .replace("please","").trim()

            if (name.lowercase().contains(cleanAppName.lowercase())) {

                try {
                    val launchIntent = Intent(Intent.ACTION_MAIN)
                    launchIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                    launchIntent.component = ComponentName(
                        app.activityInfo.packageName,
                        app.activityInfo.name
                    )
                    startActivity(launchIntent)
                    speak("Opening $name sir")
                    return
                } catch (e: Exception) {
                    speak("new kind of problem is come")
                    e.printStackTrace()
                }
            }
        }
        // If not found Search on playStore
        speak("App not found, searching on Play store")

        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://search?q=$appName")
            )
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/search?q=$appName")
            )
            startActivity(intent)
        }
    }

    private fun getPackageNameFromAppName(appName: String): String {

        val pm = packageManager
        val apps = pm.getInstalledApplications(0)

        for (app in apps) {
            val name = pm.getApplicationLabel(app).toString()

            if (name.lowercase().contains(appName.lowercase())) {
                return  app.packageName
            }
        }

        return "" // not Found
    }

    // Language Translation code

    private fun initTranslator(targetLanguage: String){

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(targetLanguage).build()

        translator = Translation.getClient(options)

        val conditions = DownloadConditions.Builder().requireWifi().build()

        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                Log.d("Translator","Language model downloaded")
            }
            .addOnFailureListener {
                Log.d("Translator","Download failed")
            }
    }

    private fun showLanguageDialog(){

        val language = arrayOf("English","Hindi","Urdu", "German", "Japanese", "Chinese")

        AlertDialog.Builder(this).setTitle("Select Language")
            .setItems(language) { _, which ->

                when (which){

                    0 -> {
                        selectedLanguageCode = TranslateLanguage.ENGLISH
                        textToSpeech.language = Locale.US
                    }

                    1 -> {
                        selectedLanguageCode = TranslateLanguage.HINDI
                        textToSpeech.language = Locale("HINDI","IN")
                        initTranslator(selectedLanguageCode)
                    }

                    2 -> {
                        selectedLanguageCode = TranslateLanguage.URDU
                        textToSpeech.language = Locale("ur", "PK")
                        initTranslator(selectedLanguageCode)

                    }
                    3 -> {
                        selectedLanguageCode = TranslateLanguage.GERMAN
                        textToSpeech.language = Locale.GERMANY
                        initTranslator(selectedLanguageCode)

                    }
                    4 -> {
                        selectedLanguageCode = TranslateLanguage.JAPANESE
                        textToSpeech.language = Locale.JAPAN
                        initTranslator(selectedLanguageCode)

                    }
                    5 -> {
                        selectedLanguageCode = TranslateLanguage.CHINESE
                        textToSpeech.language = Locale.CHINA
                        initTranslator(selectedLanguageCode)

                    }
                }

                initTranslator(selectedLanguageCode)
                speak("Language changed to $selectedLanguageCode")
            }.show()
    }

    // map code

    private fun getCurrentLocation(){

        if (ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),200
            )
            return
        }

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        if (location != null){
            val lat = location.latitude
            val lng = location.longitude

           // speak("Your current location is latitude $lat and longitude $lng")
        }
    }

    private fun openNavigation(locationName: String){

        speak("Navigating to $locationName")

        val uri = Uri.parse("google.navigation:q=$locationName")

        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        speak("Navigation started. To return , press the home button and reopen VisionAid ai")

        startActivity(intent)
    }

    private fun startListening(){

        if (!allowListning) {
            speak("listening not working on this device")
            return
        } // prevent restart

        isListening = true
        isDetectionEnabled = false

        textToSpeech.stop()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_RESULTS, Locale.getDefault()
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_PROMPT, //EXTRA_RESULTS
            "speak now"
        )
        speechLauncher.launch(intent)
    }

    private fun stopListening(){
        isListening = false
        isDetectionEnabled = true
    }

    // Vibration Warning Function

    private fun vibrateWarning(){

        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    300,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator.vibrate(300)
        }
    }

    private fun imageProxyTOBitmap(imageProxy: ImageProxy): Bitmap {

        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(
            nv21,
            ImageFormat.NV21,
            imageProxy.width,
            imageProxy.height,
            null
        )

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            Rect(0,0, imageProxy.width, imageProxy.height),
            100, out
        )
        val imageByte = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageByte, 0, imageByte.size)
    }

    // Starting a camera for object detection

    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCase()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val imageAnalyzer = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()

            imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                val bitmap = imageProxyTOBitmap(imageProxy)

                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply {
                        postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                    } , true
                )
                val tensorImage = TensorImage.fromBitmap(rotatedBitmap)
                val results = objectDetector.detect(tensorImage)

                val distanceList = mutableListOf<Float>()

                for (detection in results) {

                    val category = detection.categories.firstOrNull()
                    val box = detection.boundingBox

                    if (category != null) {

                        val boxArea = box.width() * box.height()
                        val distance = overlay.getDistance(boxArea.toFloat())

                        distanceList.add(distance)

                        val distanceCm = distance * 100
                        val currentTime = System.currentTimeMillis()
                        val distanceText = "${distanceCm.toInt()} centimeters away"

                          if (!isListening && speechEnabled && distanceCm <= alertDistance){
                            if (category.label != lastSpokenObject || currentTime - lastSpeakTime > 4000) {

                                if (selectedLanguageCode == TranslateLanguage.ENGLISH) {

                                    speak("${category.label} is ${distanceCm.toInt()} centimeters away")

                                } else if (selectedLanguageCode == TranslateLanguage.HINDI) {

                                    translator.translate(category.label).addOnSuccessListener {
                                        trnslatedLabel ->
                                        translator.translate(distanceText).addOnSuccessListener {
                                            translatedDistance ->
                                            speak("$trnslatedLabel $translatedDistance")

                                        }
                                    }

                                } else if (selectedLanguageCode == TranslateLanguage.GERMAN) {

                                    translator.translate("${category.label} is ${distanceCm.toInt()} centimeters away")
                                        .addOnSuccessListener { translatedText ->
                                            speak(translatedText)
                                        }

                                } else if (selectedLanguageCode == TranslateLanguage.JAPANESE) {
                                    translator.translate("${category.label} is ${distanceCm.toInt()} centimeters away")
                                        .addOnSuccessListener { translatedText ->
                                            speak(translatedText)
                                        }

                                } else if (selectedLanguageCode == TranslateLanguage.CHINESE) {
                                    translator.translate("${category.label} is ${distanceCm.toInt()} centimeters away")
                                        .addOnSuccessListener { translatedText ->
                                            speak(translatedText)
                                        }
                                } else {
                                    translator.translate("${category.label} is ${distanceCm.toInt()} centimeters away")
                                        .addOnSuccessListener { translatedText ->
                                            speak(translatedText)
                                        }
                                }

                                lastSpokenObject = category.label
                                lastSpeakTime = currentTime
                            }
                        }

                        if (vibrationEnabled && distanceCm <= 20){

                            if (currentTime - lastVibratTime > 1500){
                                Log.d("VIBRATION_TEST", "vibration triggered")
                                vibrateWarning()
                                lastVibratTime = currentTime
                            }
                            vibrateWarning()
                            lastVibratTime = currentTime
                        }

                        android.util.Log.d(
                            "DETECTION",
                            "Detected : ${category.label} Confidence: ${category.score}"
                        )
                    }
                }
                runOnUiThread { overlay.setResults(results, rotatedBitmap.width, rotatedBitmap.height, distanceList ) }
                imageProxy.close()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(imageProxy: ImageProxy){

        val bitmap = imageProxyTOBitmap(imageProxy)
        val rotatedBitmap = Bitmap.createBitmap(
            bitmap,
            0, 0,
            bitmap.width,
            bitmap.height,
            Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            },true
        )

        val tensorImage = TensorImage.fromBitmap(rotatedBitmap)
        val results = objectDetector.detect(tensorImage)
        val currentTime = System.currentTimeMillis()

        val distanceList = mutableListOf<Float>()

        for (detection in results){
            val box = detection.boundingBox
            val boxArea = box.width() * box.height()
            val distance = overlay.getDistance(boxArea.toFloat())
            distanceList.add(distance)

            val distanceCm = distance * 100

            val category = detection.categories.firstOrNull()

            // That's for speaking

            if (category != null){

                if (speechEnabled && distanceCm <= alertDistance){

                    if (category.label != lastSpokenObject || currentTime - lastSpeakTime > 4000){
                        speak("${category.label} is ${distanceCm.toInt()} centimeters away")

                        lastSpokenObject = category.label
                        lastSpeakTime = currentTime
                    }
                }

                // That's for vibration
                if (vibrationEnabled && distanceCm <= 20 && currentTime - lastVibratTime > 2000){
                    vibrateWarning()
                    lastVibratTime = currentTime
                }
            }
        }

        runOnUiThread {
            overlay.setResults(
                results,
                rotatedBitmap.width,
                rotatedBitmap.height,
                distanceList
            )
        }
        imageProxy.close()
    }

    private fun bindCameraUseCase(){
        val cameraSelector = if (isBackCamera) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider (previewView.surfaceProvider)
        }

        val imageAnalyzer = ImageAnalysis.Builder().build().also {
            it.setAnalyzer (cameraExecutor){ imageProxy ->
                processImage(imageProxy)
            }
        }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
    }

    private fun allPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        textToSpeech.stop()
        textToSpeech.shutdown()
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

