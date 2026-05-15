# ಕೌಶಲ್ಯ ಕರ್ನಾಟಕ — Kaushalya Karnataka
### Android App Development using GenAI — Self Employment Platform
**MindMatrix VTU Internship Program | Project Title: 92**

> A hyper-local **"Blue-Collar Portfolio"** platform that turns skilled laborers into micro-entrepreneurs by giving them a digital identity, verified work showcase, and direct customer connect.

---


## 📌 Problem Statement

Skilled local laborers — electricians, plumbers, carpenters — in small towns have **no way to showcase their "Verified Work"** or publish fixed price lists. People find them only through word-of-mouth, which is inconsistent and leads to unemployment periods for the workers.

**Kaushalya Karnataka** solves this by acting as a **"Skill Showcase"** for local entrepreneurs. Every worker can list their **"Service Cards"** — for example, *Fan Repair: ₹200* — and build a **"Trust Economy"** by allowing neighbors to give **"Thumbs Up" ratings** to local service providers.

---

## 🎯 Impact Goals

- 🏆 **Entrepreneurship** — Turning laborers into micro-entrepreneurs
- 🤝 **Dignity of Labor** — Professionalizing local services through digital profiles
- 💰 **Local Economy** — Keeping money circulating within the local community

---

## ✨ Features & App Usage / User Flow

### Worker Portfolio
- Workers can **upload photos of previous work** (e.g., a finished cabinet, wiring job)
- Set up a professional profile with name, bio, category, location, and profile photo

### Service List (Service Cards)
- Workers **add services with fixed prices** or *"Starting at"* prices (e.g., *Fan Repair: ₹200*)
- Services are editable by the worker at any time ✅ *(Success Criteria met)*

### Hire Me
- Customers can **send a hire request** (simulated notification) directly to the worker
- Request status tracked — Pending → Accepted → Completed

### Review Wall / Rating System
- Local residents **post short text feedback** on worker profiles
- App implements a **simple "Rating" logic — Average Stars** ✅ *(Success Criteria met)*
- Ratings are calculated from all submitted reviews

### Search & Filter
- **Search bar allows filtering by Category** (e.g., Electrician, Plumber, Carpenter) ✅ *(Success Criteria met)*
- Filter chips: All, Electrician, Plumber, Carpenter, Painter, Cleaner, Gardener, Other

### Real-time Chat
- Customers and workers can **chat in real-time** after a hire request is accepted

### Bilingual Support
- Full UI in **English** and **ಕನ್ನಡ (Kannada)** — toggle anytime from the home screen

---

## 🛠️ Technical Implementation

### UI
- **CardView-based layout** for worker service cards (implemented using Jetpack Compose Cards / GlassCard components)
- Galaxy-themed dark UI with glassmorphism design

### Images
- **ImagePicker** used to allow workers to document and upload their work photos (portfolio) and profile pictures
- Images stored in **Firebase Storage**

### Database
- **Firebase Firestore** used as the real-time database — customers can see worker profiles in real-time ✅
- Data collections: `users`, `requests`, `chats`, `messages`, `reviews`, `services`, `portfolio`

### Authentication
- **Google Sign-In** via Firebase Authentication

---

## 🧱 Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM (ViewModel + StateFlow) |
| Navigation | Navigation Compose |
| Authentication | Firebase Auth (Google Sign-In) |
| Database | Firebase Firestore (real-time) |
| Storage | Firebase Storage (images) |
| Image Loading | Coil (coil-compose) |
| Image Picker | Android Photo Picker / Activity Result API |
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Android 15 (API 35) |
| Build | Gradle (Kotlin DSL) |

---

## 📁 Folder Structure

```
KaushalyaKarnataka/
├── app/
│   └── src/
│       └── main/
│           ├── java/com/kaushalyakarnataka/app/
│           │   ├── data/
│           │   │   ├── Models.kt              # UserProfile, JobRequest, ServiceItem, ReviewEntry
│           │   │   ├── KaushalyaRepository.kt # Firebase Auth, Firestore, Storage operations
│           │   │   └── FirestoreMappers.kt    # Firestore document ↔ Kotlin data class mapping
│           │   ├── ui/
│           │   │   ├── I18n.kt                # Bilingual strings (English + Kannada)
│           │   │   ├── MainViewModel.kt       # App state, auth observer
│           │   │   ├── screens/
│           │   │   │   ├── LoginScreen.kt         # Google Sign-In
│           │   │   │   ├── OnboardingScreen.kt    # Role selection (Customer / Worker)
│           │   │   │   ├── HomeScreen.kt          # Browse workers, search, category filter
│           │   │   │   ├── WorkerProfileScreen.kt # Service cards, portfolio, reviews, hire button
│           │   │   │   ├── DashboardScreen.kt     # Worker: active jobs, new requests
│           │   │   │   ├── RequestsScreen.kt      # Accept / reject job requests
│           │   │   │   ├── MyHiresScreen.kt       # Customer: track hire status
│           │   │   │   ├── ChatsScreen.kt         # Chat list
│           │   │   │   ├── ChatDetailScreen.kt    # Real-time messaging
│           │   │   │   └── ProfileScreen.kt       # Edit profile, manage services & portfolio
│           │   │   ├── components/
│           │   │   │   ├── GalaxyBackground.kt    # Animated background
│           │   │   │   └── UiComponents.kt        # GlassCard, KKTextField, ScreenLoading
│           │   │   └── theme/
│           │   │       ├── Theme.kt
│           │   │       └── Type.kt
│           │   ├── FirebaseBootstrap.kt
│           │   ├── KaushalyaApplication.kt
│           │   └── MainActivity.kt
│           ├── res/
│           │   ├── values/strings.xml
│           │   ├── values/themes.xml
│           │   └── drawable/
│           └── AndroidManifest.xml
├── build.gradle.kts
├── settings.gradle.kts
├── local.properties          # (gitignored) contains WEB_CLIENT_ID
├── screenshots/              # App screenshots
└── README.md
```

---

## ✅ Success Criteria Verification

| Criteria (from Project Brief) | Status |
|---|---|
| "Service Card" must be editable by the worker at any time | ✅ Implemented — Edit services from Profile screen |
| Search bar must allow filtering by "Category" (e.g., Electrician) | ✅ Implemented — Category filter chips on Home screen |
| App must implement a simple "Rating" logic (Average stars) | ✅ Implemented — Average calculated from all reviews |

---

## ⚙️ Installation & Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- A Firebase project (free tier works)
- Android device or emulator (Android 8.0+)

### Step 1 — Clone the Repository

```bash
git clone https://github.com/<your-username>/KaushalyaKarnataka.git
cd KaushalyaKarnataka
```

### Step 2 — Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com/) and create a project.
2. Add an Android app with package name: `com.kaushalyakarnataka.app`
3. Download `google-services.json` and place it inside the `app/` folder.
4. Enable in Firebase Console:
   - **Authentication** → Google Sign-In
   - **Firestore Database**
   - **Storage**

### Step 3 — Configure Google Sign-In

Add your **Web Client ID** to `local.properties`:

```properties
WEB_CLIENT_ID=your-web-client-id.apps.googleusercontent.com
```

### Step 4 — Build and Run

```bash
./gradlew assembleDebug
```

Or open in **Android Studio** and click **Run ▶**.

---

## 🔧 How to Use

1. Open the app → **Sign in with Google**
2. First launch → **Select your role**: Customer or Worker
3. Fill in your **name, phone number, and location**
4. **If you are a Worker:**
   - Add your work category (e.g., Electrician)
   - Go to Profile → Add **Service Cards** with name and price (₹)
   - Upload **portfolio photos** of your past work
5. **If you are a Customer:**
   - Browse workers on the Home screen
   - Use the **search bar or category filter** to find the right worker
   - Tap a worker → view their **Service Cards, portfolio, and reviews**
   - Tap **Hire Now** to send a request
   - Once accepted → **Chat or Call** the worker
   - After completion → **Rate the worker** (Average star rating)

---


## 📄 License

This project is licensed under the [MIT License](LICENSE).
