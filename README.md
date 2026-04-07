# Geonex_Location_Riminder-Mobile_Application-
Geonex is an Android app that uses geofencing to trigger location-based reminders. Users can set reminders on a map, and the app sends notifications when they enter the selected area. It supports offline storage, background tracking, and works even after device restart.

# 📍 GEONEX — Smart Geofencing Reminder App

**Never forget tasks at specific locations again.**  
Geonex lets you create location-based reminders that trigger automatically when you enter a defined area — even when the app is closed or your phone restarts.

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)](https://www.java.com/)
[![SQLite](https://img.shields.io/badge/SQLite-003B57?style=for-the-badge&logo=sqlite&logoColor=white)](https://www.sqlite.org/)
[![Google Maps](https://img.shields.io/badge/Google%20Maps-4285F4?style=for-the-badge&logo=googlemaps&logoColor=white)](https://developers.google.com/maps)

---

## 🚀 Features

### ✅ Phase 1 — MVP (Core Functionality)
- Create reminders with map-based location selection
- Automatic geofence creation around selected places
- Offline storage using **Room Database**
- Push notifications when you enter/exit a geofence
- Boot receiver — reminders work after phone restart

### ⚙️ Phase 2 — Enhanced Features
- Customizable radius (100m, 500m, 1km, custom)
- Recurring reminders (daily, weekly, monthly)
- Voice input for hands-free reminder creation
- Automatic category detection using keywords
- Search reminders by title or location
- Swipe actions (complete / delete)
- Manual dark mode toggle
- Statistics dashboard (total, completed, success rate)

### 🤖 Phase 3 — Smart Automation
- Background location tracking
- Geofence monitoring service
- Broadcast receiver integration (reboot, transitions)
- Notification automation with sound & vibration
- Battery-efficient background service optimization
- Error handling for GPS off / permission denied

### 🎨 Phase 4 — Final Polish
- Modern Material Design UI
- Edit, delete, enable/disable reminders
- Data backup & restore (JSON export/import)
- Performance optimizations
- Input validation & security improvements
- Splash screen + custom app icon

---

## 📱 Screenshots (Placeholder)

| Onboarding | Add Reminder | Home Screen | Notification |
|------------|--------------|-------------|---------------|
| 🖼️ Soon | 🖼️ Soon | 🖼️ Soon | 🖼️ Soon |

---

## 🛠️ Tech Stack

| Layer       | Technology |
|-------------|------------|
| Language    | Java       |
| UI          | Material Design, ViewPager2, RecyclerView |
| Database    | Room (SQLite) |
| Maps        | Google Maps SDK |
| Location    | Fused Location Provider, Geofencing API |
| Background  | BroadcastReceiver, WorkManager, Foreground Service |
| Async       | LiveData, ViewModel, Repository pattern |
| Permissions | ActivityResultLauncher |
| Notifications | NotificationCompat |

---

## 🏗️ Project Structure
com.geonex/
│
├── data/
│ ├── local/ // Room entities, DAO, Database
│ └── repository/ // ReminderRepository
│
├── ui/
│ ├── onboarding/ // 4-screen ViewPager2 tutorial
│ ├── home/ // Reminder list + search + swipe
│ ├── addreminder/ // Map + voice + category detection
│ └── main/ // MainActivity container
│
├── services/
│ ├── GeofenceHelper.java
│ ├── GeofenceBroadcastReceiver.java
│ └── BootReceiver.java
│
├── utils/
│ ├── NotificationHelper.java
│ ├── PermissionHelper.java
│ └── Constants.java
│
└── GeonexApplication.java


---

## 🔧 Installation & Setup

### Prerequisites
- Android Studio Hedgehog | 2023.3.1+
- Minimum SDK: API 24 (Android 7.0)
- Google Maps API Key

### Steps

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/geonex.git
cd geonex

2.Get a Google Maps API Key

Go to Google Cloud Console

Enable Maps SDK for Android

Create an API key

Add to local.properties:
MAPS_API_KEY=YOUR_KEY_HERE

3.Open in Android Studio → Build → Run

📦 Dependencies (Partial)
implementation 'com.google.android.gms:play-services-maps:18.2.0'
implementation 'com.google.android.gms:play-services-location:21.0.1'
implementation 'androidx.room:room-runtime:2.6.0'
implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
implementation 'com.google.android.material:material:1.11.0'

Full list: build.gradle (app level)

🧪 Testing Scenarios
Scenario	Expected Result
GPS OFF while creating reminder	Show dialog to enable GPS
Permission denied	Graceful fallback + explanation
Phone restart	Geofences re-registered automatically
Enter geofence area	Notification appears with title & location
Background app	Still triggers notification
Recurring reminder completed	Reactivates on next schedule
📄 Future Scope (Post-Phase 4)
Cloud backup (Firebase / Drive)

Share reminders with friends

Geofence sharing (family location alerts)

Wear OS integration

AI-based smart suggestion for radius & categories

👨‍💻 Author
Mayakannan.N
📧 mayakannan122004@gmail.com
🔗 https://www.linkedin.com/in/mayakannan2004/
🐙 https://github.com/mayakannan12/Geonex_Location_Riminder-Mobile_Application-

📃 License
This project is licensed under the MIT License — see the LICENSE file for details.
