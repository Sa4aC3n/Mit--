# دليل ميت غمر — Dalil Mit Ghamr

A community business directory Android app for Mit Ghamr, Egypt — built with
Kotlin, Jetpack Compose, and Firebase.

## Features

- 🔍 Browse and search local businesses by category
- 📍 GPS-based location display
- 🔥 Real-time data via Cloud Firestore
- 👤 User authentication (Firebase Auth)
- 🛡️ Role-based admin panel with data management
- 📊 Excel / CSV import and export for business data
- 🌐 Full Arabic RTL UI

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Backend | Firebase (Firestore, Auth, Functions) |
| Build | Gradle (Kotlin DSL) |
| Min SDK | Android 7.0 (API 24) |

## Project Structure

```
app/src/main/
├── java/com/example/   # Kotlin source (MVVM architecture)
│   ├── data/           # Repositories, models, Firebase
│   ├── ui/             # Compose screens and components
│   └── util/           # Excel import/export helpers
├── assets/             # Runtime assets (fonts, etc.)
└── res/                # Android resources

functions/              # Firebase Cloud Functions (Node.js)
firestore.rules         # Firestore security rules
tools/                  # Development-only utilities
└── categories/         # Category seed data (Excel)
```

## Getting Started

1. Clone the repo and open in Android Studio.
2. Add your `google-services.json` to `app/` (from Firebase Console).
3. Copy `.env.example` to `.env` and fill in your credentials.
4. Run `./gradlew assembleDebug` to build.

## Branch

Active development branch: `final-stabilization`

## License

Private — all rights reserved.
