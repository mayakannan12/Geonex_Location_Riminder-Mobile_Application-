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
