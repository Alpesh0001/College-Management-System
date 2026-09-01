# 🎓 College Management System

An Android application for managing college operations including student enrollment, faculty management, attendance tracking, fee payment, timetables, study materials, and assignments.

## Project Structure

This project contains **three Android application modules**:

| Module | Package | Description |
|--------|---------|-------------|
| `app` | `com.example.collagemanagmentsystem` | **Student** app — login, fees, timetable, materials |
| `collegemanagementfaculty` | `com.example.collegemanagementsystemfaculty` | **Faculty** app — attendance, assignments, materials |
| `collegemanagementsystemadmin` | `com.example.collegemanagementsystemadmin` | **Admin** app — manage students, faculty, courses, fees |

## Technology Stack

- **Language:** Kotlin
- **Build System:** Gradle with Kotlin DSL (`.kts`)
- **Backend / Database:** Firebase Firestore
- **Authentication:** Firebase Authentication
- **Image Upload:** ImgBB API
- **File / PDF Upload:** Cloudinary
- **Payment Gateway:** Razorpay
- **Email / OTP:** Gmail SMTP via JavaMail (Android)

---

## ⚙️ Configuration

### Prerequisites

- Android Studio (latest stable version)
- Android SDK 26+
- A Firebase project with Firestore and Authentication enabled
- API keys for: Razorpay, ImgBB, Cloudinary, Gmail App Password

### Setup Instructions

**1. Clone the repository**

```bash
git clone https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git
cd YOUR_REPO_NAME
```

**2. Open in Android Studio**

Open Android Studio → `File → Open` → select the project folder.

**3. Create your local configuration file**

Copy the example file and rename it:

```bash
# Windows (Command Prompt)
copy local.properties.example local.properties

# Windows (PowerShell)
Copy-Item local.properties.example local.properties

# macOS / Linux
cp local.properties.example local.properties
```

**4. Fill in your real values**

Open `local.properties` and replace every placeholder with your actual credentials:

```properties
# Android SDK path (set automatically by Android Studio)
sdk.dir=C:\Users\YourUsername\AppData\Local\Android\Sdk

# Gmail SMTP (for OTP email)
SMTP_SENDER_EMAIL=your_gmail@gmail.com
SMTP_SENDER_PASSWORD=your_gmail_app_password

# Razorpay
RAZORPAY_KEY_ID=rzp_test_YourKey

# ImgBB
IMGBB_API_KEY=your_key

# Cloudinary
CLOUDINARY_CLOUD_NAME=your_cloud
CLOUDINARY_API_KEY=your_key
CLOUDINARY_API_SECRET=your_secret
CLOUDINARY_UPLOAD_PRESET=your_preset
```

> **Important:** `local.properties` is already in `.gitignore` and will **never** be committed to Git.
> Only `local.properties.example` (which contains no real secrets) is committed.

**5. Sync Gradle**

In Android Studio: `File → Sync Project with Gradle Files`

**6. Add Firebase configuration**

Each module requires a `google-services.json` file from your Firebase project:
- Download from [Firebase Console](https://console.firebase.google.com/) → Project Settings → Your Apps
- Place the file in each module directory:
  - `app/google-services.json`
  - `collegemanagementfaculty/google-services.json`
  - `collegemanagementsystemadmin/google-services.json`

**7. Build and Run**

Select the desired module (app / collegemanagementfaculty / collegemanagementsystemadmin) from the Android Studio run configuration and press Run.

---

## 🔑 Required API Keys

Refer to `local.properties.example` for the complete list of required variables with instructions on where to obtain each key.

| Variable | Service | Where to Get |
|----------|---------|-------------|
| `SMTP_SENDER_EMAIL` | Gmail (SMTP OTP) | Your Gmail address |
| `SMTP_SENDER_PASSWORD` | Gmail App Password | [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords) |
| `RAZORPAY_KEY_ID` | Razorpay payments | [dashboard.razorpay.com](https://dashboard.razorpay.com/app/keys) |
| `IMGBB_API_KEY` | Image hosting | [api.imgbb.com](https://api.imgbb.com/) |
| `CLOUDINARY_CLOUD_NAME` | File/PDF upload | [cloudinary.com/console](https://cloudinary.com/console) |
| `CLOUDINARY_API_KEY` | File/PDF upload | [cloudinary.com/console](https://cloudinary.com/console) |
| `CLOUDINARY_API_SECRET` | File/PDF upload | [cloudinary.com/console](https://cloudinary.com/console) |
| `CLOUDINARY_UPLOAD_PRESET` | File/PDF upload | Cloudinary Dashboard → Settings → Upload |

---

## 🔒 Security Notes

- **`local.properties`** — local only, never committed. Contains all your real API keys and credentials.
- **`google-services.json`** — standard Firebase client configuration. The API key inside it is an Android-restricted key intended for client use. It is safe to commit per [Firebase documentation](https://firebase.google.com/docs/android/setup).
- **Razorpay** — Use `rzp_test_*` keys during development. Switch to `rzp_live_*` only for production.
- **Gmail App Password** — This credential is embedded in the compiled APK, which is a known limitation of client-side SMTP. Consider moving email sending to your own backend for production use.
- **Cloudinary `api_secret`** — This is a private credential embedded in the APK. It is embedded in the compiled binary and could theoretically be extracted. For production, consider moving Cloudinary uploads through a backend proxy.

---

## 📱 Features

### Student Module (`app`)
- Login / Registration with OTP email verification
- View timetable, study materials, assignments
- Fee payment via Razorpay
- View attendance records
- Change password / profile

### Faculty Module (`collegemanagementfaculty`)
- Login / Registration
- Mark attendance
- Upload study materials (PDF via Cloudinary)
- Create and manage assignments
- View student lists

### Admin Module (`collegemanagementsystemadmin`)
- Manage students, faculty, courses, subjects, divisions
- Manage fee records
- View and manage all data

---

## 📄 License

This project is for educational purposes.
