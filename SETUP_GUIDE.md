# Quick Setup Guide

## Step-by-Step Setup Instructions

### 1. Google Cloud Console Setup (Required)

#### A. Create Google Cloud Project
1. Visit [Google Cloud Console](https://console.cloud.google.com/)
2. Click "Select a project" → "New Project"
3. Enter project name: "Newspaper Events Timeline"
4. Click "Create"

#### B. Enable Required APIs
1. In the Cloud Console, go to "APIs & Services" → "Library"
2. Search and enable:
   - **Google Drive API**
   - **Google Cloud Vision API** (optional, for better OCR)

#### C. Create OAuth 2.0 Credentials
1. Go to "APIs & Services" → "Credentials"
2. Click "Create Credentials" → "OAuth 2.0 Client ID"
3. If prompted, configure OAuth consent screen:
   - User Type: External
   - App name: Newspaper Events Timeline
   - User support email: Your email
   - Developer contact: Your email
   - Save and continue through all steps
4. Back to "Create OAuth 2.0 Client ID":
   - Application type: **Android**
   - Name: Newspaper Events Timeline Android
   - Package name: `com.newspaper.eventstimeline`
   - SHA-1 certificate fingerprint:

**Get SHA-1 Fingerprint:**

For **Debug builds** (development):
```bash
# On Mac/Linux
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# On Windows
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

Look for the SHA-1 line in the output and copy it.

5. Click "Create"
6. Download the credentials (optional, not needed for Android OAuth)

### 2. Get Gemini API Key (Required)

1. Visit [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Sign in with your Google account
3. Click "Create API Key"
4. Select your Google Cloud project (or create new one)
5. Copy the generated API key
6. **Save it securely** - you'll need it in the next step

### 3. Configure API Keys in the App

#### Update ArticleViewModel.kt
1. Open: `app/src/main/java/com/newspaper/eventstimeline/ui/viewmodel/ArticleViewModel.kt`
2. Find line 23:
   ```kotlin
   private val geminiService = GeminiService("YOUR_GEMINI_API_KEY")
   ```
3. Replace `YOUR_GEMINI_API_KEY` with your actual API key:
   ```kotlin
   private val geminiService = GeminiService("AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX")
   ```

#### Update EventViewModel.kt
1. Open: `app/src/main/java/com/newspaper/eventstimeline/ui/viewmodel/EventViewModel.kt`
2. Find line 19:
   ```kotlin
   private val geminiService = GeminiService("YOUR_GEMINI_API_KEY")
   ```
3. Replace `YOUR_GEMINI_API_KEY` with your actual API key:
   ```kotlin
   private val geminiService = GeminiService("AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX")
   ```

**⚠️ Security Warning**: For production apps, never hardcode API keys. Use:
- BuildConfig with environment variables
- Android Keystore
- Firebase Remote Config
- Secrets management service

### 4. Build and Run

1. **Open Android Studio**
2. **Open the project** (File → Open → Select project folder)
3. **Wait for Gradle sync** to complete
4. **Connect Android device** or **start emulator**
5. **Click Run** (▶️ button) or press `Shift + F10`

### 5. First Run Setup

1. **Grant Permissions**:
   - Allow camera access
   - Allow storage access (if prompted)

2. **Sign in to Google** (when uploading to Drive):
   - Click "Upload to Google Drive" in article details
   - Sign in with your Google account
   - Grant Drive permissions

3. **Test the App**:
   - Capture a test article
   - Wait for OCR to complete
   - Try analyzing articles to create events

## Troubleshooting

### "Sign in failed" Error
- **Check SHA-1**: Verify the SHA-1 in Google Cloud Console matches your keystore
- **App Package**: Ensure package name is exactly `com.newspaper.eventstimeline`
- **OAuth Consent**: Make sure OAuth consent screen is configured

### "API Key Invalid" Error
- **Copy carefully**: Ensure no extra spaces in API key
- **Enable APIs**: Verify Gemini API is enabled in Cloud Console
- **Billing**: Some APIs require billing to be enabled (though Gemini has free tier)

### OCR Not Working
- **Internet**: OCR requires internet connection
- **Image Quality**: Ensure image is clear and well-lit
- **Permissions**: Check app has storage permissions

### Build Errors
```bash
# Clear Gradle cache
./gradlew clean

# Invalidate caches in Android Studio
File → Invalidate Caches → Invalidate and Restart
```

## Optional Enhancements

### Better OCR for Urdu/Telugu
For production-quality OCR in Urdu and Telugu, integrate Google Cloud Vision API:

1. Enable **Cloud Vision API** in Google Cloud Console
2. Create a **Service Account**:
   - IAM & Admin → Service Accounts → Create Service Account
   - Grant "Cloud Vision API User" role
   - Create JSON key
3. Add Vision API dependency to `app/build.gradle.kts`:
   ```kotlin
   implementation("com.google.cloud:google-cloud-vision:3.20.0")
   ```
4. Update OCR service to use Vision API for Urdu/Telugu text

### Secure API Key Storage
1. Add to `local.properties` (not committed to git):
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```
2. Read in `build.gradle.kts`:
   ```kotlin
   android {
       defaultConfig {
           buildConfigField("String", "GEMINI_API_KEY", 
               "\"${project.findProperty("GEMINI_API_KEY")}\"")
       }
   }
   ```
3. Use in code:
   ```kotlin
   private val geminiService = GeminiService(BuildConfig.GEMINI_API_KEY)
   ```

## Testing the App

### Manual Test Checklist
- [ ] Capture a newspaper article
- [ ] Verify image is saved and displayed
- [ ] Check OCR processes automatically
- [ ] View article details
- [ ] Upload to Google Drive
- [ ] Capture multiple articles
- [ ] Analyze articles to create events
- [ ] View timeline with events
- [ ] Generate AI summary for an event
- [ ] Delete an article

### Test with Different Languages
- [ ] Capture English newspaper
- [ ] Capture Urdu newspaper (if available)
- [ ] Capture Telugu newspaper (if available)
- [ ] Verify language detection works

## Need Help?

- Check the main [README.md](README.md) for detailed documentation
- Review error logs in Android Studio Logcat
- Verify all API keys and configurations
- Ensure all Google Cloud APIs are enabled
- Check internet connectivity

## Next Steps

Once the app is running:
1. Capture your first newspaper article
2. Experiment with the OCR functionality
3. Collect several articles
4. Use the AI analysis to create an event timeline
5. Review and refine the generated events

Happy coding! 🚀
