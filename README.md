#  Water Level Monitoring System — ESP32 + Android TV Overlay

##  Overview
This project implements a **real-time IoT-based water level monitoring system** that measures the water level inside a tank using an **ESP32 microcontroller** and an **ultrasonic sensor**.  
The measured data is transmitted wirelessly over Wi-Fi to an **Android TV application**, which displays the live tank level as **horizontal bars** in an on-screen overlay — allowing users to monitor their water level without interrupting TV playback.

---

##  Objectives
- Design and implement a **wireless tank-level monitoring system** using ESP32 and ultrasonic sensing.  
- Develop an **Android TV overlay app** that displays the current water level in both percentage and graphical form.  
- Demonstrate **real-time IoT data communication** using HTTP (JSON) between embedded hardware and Android.  
- Evaluate system **efficiency, responsiveness, and user experience** for home automation scenarios.

---

##  System Architecture

###  Hardware (ESP32 Node)
- **Sensor:** Ultrasonic sensor (HC-SR04T or JSN-SR04M) measures the distance from the sensor to the water surface.  
- **Controller:** ESP32 calculates the water height and fill percentage based on tank depth.  
- **Server:** ESP32 runs a lightweight web server providing live readings in JSON format


Software (Android TV App)
Developed in Kotlin using MVVM architecture (ViewModel + Repository + Retrofit).
Runs as an overlay service, displaying 7 horizontal bars that light up according to the water level percentage.
Fetches updated data from the ESP32 at regular intervals.
Automatically handles errors, disconnections, and data refresh.

Communication
Protocol: HTTP (JSON response)
Frequency: Configurable update interval
Network: Local Wi-Fi connection between ESP32 and Android TV

Working Principle
The ultrasonic sensor sends and receives a pulse to measure distance from the sensor to the water surface.
The ESP32 converts the measured distance into height and percentage based on calibrated tank dimensions.
The data is served via ESP32’s internal web server as a JSON object.
The Android TV overlay app periodically fetches the JSON data, parses it, and updates the UI in real time.
The water level is represented visually as horizontal bars.

Components Used
Component	Description
ESP32 Dev Board	Wi-Fi microcontroller for sensor reading and HTTP server
Ultrasonic Sensor (HC-SR04T / JSN-SR04M)	Measures water level distance
Power Supply (5V)	Powers ESP32 and sensor
Android TV / Android Device	Displays the overlay app with live readings

Software Stack
Layer	Tools / Frameworks
Embedded	Arduino IDE / PlatformIO
Communication	HTTP (JSON)
Android Development	Android Studio, Kotlin
Architecture	MVVM (ViewModel + LiveData + Repository)
Networking	Retrofit + OkHttp
UI Framework	Android Jetpack Components

Features
🔹 Real-time water level visualization (0–100%)

🔹 Horizontal bar overlay display (7 bars)

🔹 Automatic periodic data refresh

🔹 JSON-based local Wi-Fi communication

🔹 Error handling and reconnection logic

🔹 Minimal permissions and background service operation

🔹 Customizable refresh rate and IP configuration

Results
Achieved stable and accurate readings within ±2% error after calibration.
Overlay remains active on Android TV without interrupting playback.
System latency (sensor-to-display) under 500 ms on local network.
Low power consumption, suitable for 24/7 operation.

Applications
Smart home and apartment water management
Industrial tank monitoring
Agriculture and irrigation level control
General-purpose IoT liquid-level monitoring


🧑‍💻 Author & Development Notes
This project was conceptualized and designed by Dhruvkumar Shingala and Mukeshbhai Shingala.
App code was written with AI-assisted development (Gemini), followed by manual debugging, optimization, and integration with real hardware.
All system logic, architecture design, calibration, and final testing were performed manually.

<img width="201" height="119" alt="water level project simulated v3 4" src="https://github.com/user-attachments/assets/67583c9e-7302-4a5f-a5a7-03d122a81771" />

Image 1: Development Simulation – Android Studio
Shows the Water-Level Monitoring App running in the Android Studio emulator during testing and development. Used for validating the overlay layout, data updates, and UI behavior.

![water level project real run v3 4](https://github.com/user-attachments/assets/05604ae7-7bc9-46fb-9fa8-1edff12009e1)

Image 2: Deployed on Real Android TV
Displays the final live implementation on an actual Android TV, receiving real-time water-level data from the ESP32 and showing it as dynamic horizontal bars on the screen.

![water level project webpage](https://github.com/user-attachments/assets/5ba9f849-c1d9-4ce1-b38c-47f8abb4179e)

Image 3: Web Dashboard Interface (ESP32 Hosted Page)
Displays the ESP32-hosted webpage visualizing the water tank level in real time.
Shows distance, fill level, and percentage, includes a “Measure Now” button for manual updates, and auto-refreshes every 30 seconds.
This interface allows monitoring from any device connected to the same Wi-Fi network.

Overlay View	ESP32 Server Output

🧰 Build Instructions
ESP32 Setup
Open the .ino file in Arduino IDE.
Configure your Wi-Fi credentials in the code.
Upload the sketch to the ESP32 board.
Open Serial Monitor to verify the assigned IP address.
Android TV Setup
Clone this repository:

bash
Copy code
git clone https://github.com/DHRUV7044/water_level_data_feTCH.git
Open in Android Studio.

Update the base URL in BuildConfig.BASE_URL.
Build and run the app on your Android TV or emulator.
Grant overlay permission when prompted.


🌐 Contact
Author: Dhruv Shingala
GitHub: @DHRUV7044
LinkedIn: [www.linkedin.com/in/dhruv-shingala]
Email: [dhruve.shingala@gmail.com]

