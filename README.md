# VisionAid AI 👁️‍🗨️📱

**VisionAid AI** is an Android accessibility application designed to assist visually impaired users by combining **AI-based object detection, voice commands, navigation, and real-time alerts**.
The app uses a smartphone camera and AI model to detect objects in the environment and provide **audio feedback, distance estimation, vibration alerts, and voice-controlled navigation**.

This project aims to improve independence and safety for visually impaired individuals by transforming a smartphone into an intelligent vision assistant.

---

# 🚀 Features

### 🔍 Real-Time Object Detection

* Detects objects using a **TensorFlow Lite model (EfficientDet Lite)**.
* Displays bounding boxes on detected objects.
* Works in real-time using the phone camera.

### 📏 Distance Estimation

* Estimates the **distance between the user and detected objects**.
* Distance is calculated from object bounding box size.

Example output:

```
Person is 120 centimeters away
Chair is 80 centimeters away
```

---

### 🔊 Voice Feedback

* The app **speaks object names and their distance**.
* Uses Android **Text-to-Speech (TTS)**.
* Helps visually impaired users understand surroundings.

Example:

```
"Person is 1 meter away"
```

---

### 📳 Vibration Alerts

* When objects are too close, the phone **vibrates automatically**.
* Helps users react quickly to obstacles.

---

### 🎙️ Voice Assistant Navigation

Users can navigate using **voice commands**.

Example:

```
User: "Hospital"
App: Opens navigation to the hospital
```

The app:

1. Listens to voice commands
2. Recognizes the destination
3. Opens map navigation automatically

---

### 🗺️ Location & Navigation

* Fetches **current GPS location**.
* Opens navigation using map services.
* Designed for simple and quick navigation for visually impaired users.

---

### 🌍 Multi-Language Support

* Uses **MLKit Translation** to translate spoken information.
* Allows users to hear object names in their **native language**.

Example supported languages:

* English
* Hindi
* German
* Japanese

---

### 🎤 Voice Control System

The app includes an integrated **voice assistant system**:

Features:

* Auto voice listening
* Manual microphone button
* Voice command recognition
* Navigation activation

---

# 📱 Accessibility Focus

This app is designed with accessibility in mind:

✔ Voice-based interaction
✔ Minimal UI complexity
✔ Vibration feedback
✔ Clear audio responses
✔ Simple navigation system

---

# 🧠 Technologies Used

| Technology              | Purpose              |
| ----------------------- | -------------------- |
| Kotlin                  | Android development  |
| TensorFlow Lite         | AI object detection  |
| EfficientDet Lite       | Detection model      |
| CameraX                 | Camera streaming     |
| ML Kit Translator       | Language translation |
| Android Text-to-Speech  | Voice feedback       |
| Speech Recognition      | Voice commands       |
| GPS / Location Services | Navigation           |

---

# 📂 Project Structure

```
VisionAid-AI
│
├── MainActivity.kt
├── MapActivity.kt
├── OverlayView.kt
│
├── assets
│   └── efficientdet_lite1.tflite
│
├── res
│   ├── layout
│   │   ├── activity_main.xml
│   │   └── activity_map.xml
│   │
│   ├── drawable
│   └── mipmap
│
└── AndroidManifest.xml
```

---

# ⚙️ Installation

Clone the repository:

```
git clone https://github.com/yourusername/VisionAid-AI.git
```

Open the project in **Android Studio**.

Sync Gradle and run the app on an Android device.

---

# 📸 How It Works

1️⃣ Camera detects objects
2️⃣ AI model identifies them
3️⃣ Distance estimation is calculated
4️⃣ App speaks the result
5️⃣ Vibration warns if object is too close

Example:

```
Detected: Person
Distance: 90 cm
Action: Voice + Vibration alert
```

---

# 🛠 Future Improvements

Planned upgrades:

* Real-time navigation assistance
* Indoor obstacle detection
* AI route guidance
* Offline AI model optimization
* Custom voice assistant
* Wearable device integration

---

# 🎯 Project Goal

The goal of VisionAid AI is to **use artificial intelligence and mobile technology to improve mobility and independence for visually impaired individuals**.

This project demonstrates how AI, computer vision, and voice interaction can be combined to build practical accessibility solutions.

---

# 👨‍💻 Author

**Ashad Saifi**

Computer Science Student
AI & Accessibility Technology Enthusiast

---

# ⭐ Support

If you like this project, consider giving it a **⭐ star** on GitHub.

It helps the project reach more people and encourages further development.

---

# 📜 License

This project is licensed under the **MIT License**.
