<div align="center">
  <img src="app/src/main/res/mipmap-xxhdpi/ic_launcher.png" width="128" height="128" alt="Gang Green Logo">
  
  # Gang Green 🏈
  **The Ultimate Companion App for New York Jets Fans**

  [![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
  [![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-brightgreen.svg)](https://developer.android.com/jetpack/compose)
  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
</div>

<br>

**Gang Green** is a premium, open-source Android application built entirely in Kotlin and Jetpack Compose. Designed exclusively for New York Jets fans, it aggregates real-time ESPN data, live play-by-play Gamecasts, dynamic win probabilities, breaking news, and live roster updates into a single, highly-optimized dark mode interface.

---

## ✨ Features

- 🟢 **Live ESPN Gamecast**: Real-time play-by-play tracking, drive summaries, and current field position.
- 📈 **Animated Win Probability**: Beautifully animated canvas graphs showing live game momentum and Vegas odds.
- 📰 **Breaking News Feed**: Aggregates the latest Jets news via RSS, filtering out paywalls (like NY Post) so you only get accessible, high-quality content.
- 🏈 **Live Roster & Depth Charts**: Complete, up-to-date team roster.
- 🏥 **Injury Reports**: Dedicated tracking of player statuses and injuries.
- 🎨 **Aggressive Dark UI**: A bespoke Jets-inspired Kelly Green (`#0C2B1B`) dark mode theme built from the ground up.

---

## 📸 Screenshots



<div align="center">
  <img src="screenshots/live_tab.png" width="250" alt="Live Tab" />
  <img src="screenshots/news_tab.png" width="250" alt="News Tab" />
  <img src="screenshots/roster_tab.png" width="250" alt="Roster Tab" />
</div>

---

## 🛠 Tech Stack & Architecture

- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose (Material 3)
- **Asynchronous & Reactive:** Coroutines & StateFlow
- **Networking:** OkHttp3 & Retrofit (ESPN API endpoints)
- **Image Loading:** Coil
- **Serialization:** kotlinx.serialization

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Iguana (or newer)
- Android SDK 34+

### Building from Source
1. Clone the repository:
   ```bash
   git clone https://github.com/WiseManChris/Gang-Green-New-York-Jets-Fan-app.git
   ```
2. Open the project in Android Studio.
3. Sync Gradle and hit **Run**.

*(Note: The production Keystore is ignored by git for security purposes. If you are building for release, you will need to generate your own `ganggreen.jks` keystore and update `build.gradle.kts`.)*

---

## 🤝 Contributing
Contributions are always welcome! Whether it's adding new features, fixing bugs, or improving documentation, feel free to open a Pull Request. Let's build the best app for Jets Nation together.

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
