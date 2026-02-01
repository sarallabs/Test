# Newspaper Events Timeline

An Android application for capturing, organizing, and analyzing newspaper articles in multiple languages (Urdu, Telugu, and English) using OCR technology and AI-powered event timeline generation.

## Features

### 📸 Camera Capture
- High-quality camera capture using CameraX
- Capture newspaper articles with metadata (newspaper name, publication date)
- Mark articles as event invitations
- Add custom notes to articles

### 🔍 OCR Processing
- Automatic text recognition using ML Kit
- Support for multiple languages (English, Urdu, Telugu)
- Google Drive integration for backup and storage
- Real-time OCR status tracking

### 🤖 AI-Powered Analysis
- Gemini AI integration for intelligent event extraction
- Automatic event timeline creation from multiple articles
- AI-generated summaries connecting related articles
- Event categorization and key person identification

### 📊 Event Timeline
- Chronological display of events
- Link multiple articles to single events
- View event details with related articles
- Filter and search capabilities

### 💾 Local Storage
- Room database for offline-first architecture
- Efficient article and event management
- Cross-reference between articles and events

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + XML Views
- **Architecture**: MVVM with Repository pattern
- **Database**: Room (SQLite)
- **Camera**: CameraX
- **OCR**: Google ML Kit
- **AI**: Google Gemini API
- **Cloud Storage**: Google Drive API
- **Async**: Kotlin Coroutines + Flow
- **Image Loading**: Coil
- **Material Design**: Material 3 (Material You)

## Prerequisites

Before building and running the app, you need to set up the following:

### 1. Google Cloud Console Setup

#### Enable APIs:
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the following APIs:
   - Google Drive API
   - Google Cloud Vision API (optional, for advanced OCR)

#### Create OAuth 2.0 Credentials:
1. Go to **APIs & Services** > **Credentials**
2. Click **Create Credentials** > **OAuth 2.0 Client ID**
3. Select **Android** as application type
4. Get your app's SHA-1 fingerprint:
   ```bash
   # For debug builds
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
5. Enter your package name: `com.newspaper.eventstimeline`
6. Enter your SHA-1 fingerprint
7. Click **Create**

### 2. Get Gemini API Key

1. Go to [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Create a new API key
3. Copy the API key

### 3. Configure the App

#### Update Gemini API Key:
1. Open `app/src/main/java/com/newspaper/eventstimeline/ui/viewmodel/ArticleViewModel.kt`
2. Replace `YOUR_GEMINI_API_KEY` with your actual Gemini API key:
   ```kotlin
   private val geminiService = GeminiService("your-actual-api-key-here")
   ```

3. Open `app/src/main/java/com/newspaper/eventstimeline/ui/viewmodel/EventViewModel.kt`
4. Replace `YOUR_GEMINI_API_KEY` with your actual Gemini API key:
   ```kotlin
   private val geminiService = GeminiService("your-actual-api-key-here")
   ```

**Note**: For production apps, store API keys securely using:
- Android Keystore
- BuildConfig fields with environment variables
- Remote configuration services

## Building the App

### Requirements:
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 24 or higher (minimum)
- Android SDK 34 (target)
- JDK 17

### Build Steps:

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd NewspaperEventsTimeline
   ```

2. **Open in Android Studio**:
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the project directory

3. **Sync Gradle**:
   - Wait for Gradle sync to complete
   - Resolve any dependency issues

4. **Configure signing** (for release builds):
   - Create a keystore or use existing one
   - Update `app/build.gradle.kts` with signing configuration

5. **Build and Run**:
   - Connect an Android device or start an emulator
   - Click **Run** (▶️) or press `Shift + F10`

## Usage Guide

### Capturing Articles

1. **Launch the app** and grant camera permissions
2. **Tap the camera FAB** (Floating Action Button) at the bottom right
3. **Position the newspaper article** in the camera viewfinder
4. **Tap capture** to take a photo
5. **Fill in metadata**:
   - Newspaper name (optional)
   - Publication date (optional)
   - Mark as event invitation if applicable
   - Add notes (optional)
6. **Save the article** - OCR processing starts automatically

### Viewing Articles

1. **Main screen** shows all captured articles in a grid
2. **Tap an article** to view details
3. **Use tabs** to filter:
   - All articles
   - Event invitations
   - Pending OCR

### Managing Articles

1. **Article Details Screen**:
   - View full-size image
   - Read OCR extracted text
   - Process OCR if not done
   - Upload to Google Drive for backup
   - Delete article

### Creating Event Timeline

1. **Tap the analyze FAB** (magnifying glass icon)
2. **Confirm analysis** - Gemini AI will:
   - Analyze all articles with OCR text
   - Identify distinct events
   - Group related articles
   - Extract event details
   - Create chronological timeline

### Viewing Timeline

1. **Tap the timeline FAB** (clock icon)
2. **Browse events** chronologically
3. **View event details**:
   - Title and description
   - Date and location
   - Key persons involved
   - Related articles count
4. **Generate AI summary** for comprehensive event overview

### Google Drive Integration

1. **First time**: Sign in to Google when prompted
2. **Upload articles** individually from article details
3. **Automatic backup** option can be enabled
4. **View uploaded files** in your Google Drive

## Multi-Language Support

The app supports text recognition in:
- **English**: Full ML Kit support
- **Urdu**: AI-assisted language detection
- **Telugu**: AI-assisted language detection
- **Mixed languages**: Handles articles with multiple scripts

**Note**: For better Urdu and Telugu OCR accuracy, consider integrating Google Cloud Vision API which has better support for these scripts.

## Project Structure

```
app/
├── src/main/
│   ├── java/com/newspaper/eventstimeline/
│   │   ├── data/
│   │   │   ├── api/           # API services (Drive, OCR, Gemini)
│   │   │   ├── local/         # Room database (DAO, Database, Converters)
│   │   │   ├── model/         # Data models (Article, Event, etc.)
│   │   │   └── repository/    # Repository layer
│   │   ├── ui/
│   │   │   ├── adapter/       # RecyclerView adapters
│   │   │   ├── capture/       # Camera capture screen
│   │   │   ├── details/       # Article details screen
│   │   │   ├── timeline/      # Events timeline screen
│   │   │   ├── viewmodel/     # ViewModels
│   │   │   └── MainActivity.kt
│   │   └── NewspaperApp.kt    # Application class
│   ├── res/
│   │   ├── layout/            # XML layouts
│   │   ├── values/            # Strings, colors, themes
│   │   └── xml/               # File paths, backup rules
│   └── AndroidManifest.xml
├── build.gradle.kts
└── proguard-rules.pro
```

## Database Schema

### Articles Table
- ID (Primary Key)
- Image path
- Thumbnail path
- OCR text
- Newspaper name
- Publication date
- Capture date
- Language
- Is event invitation
- Drive file ID
- OCR status
- Event ID
- Tags
- Notes

### Events Table
- ID (Primary Key)
- Title
- Description
- Event date
- Created date
- Category
- Location
- Key persons
- Summary (AI-generated)
- AI generated flag

### Article-Event Cross Reference
- Article ID (Foreign Key)
- Event ID (Foreign Key)

## Permissions

The app requires the following permissions:

- **CAMERA**: To capture newspaper articles
- **INTERNET**: For OCR and AI services
- **READ_MEDIA_IMAGES** (Android 13+): To access captured images
- **READ_EXTERNAL_STORAGE** (Android 12 and below): To access images
- **ACCESS_NETWORK_STATE**: To check connectivity

## Known Limitations

1. **ML Kit OCR**: Limited accuracy for Urdu and Telugu scripts
   - Consider using Google Cloud Vision API for production
2. **Gemini API**: Requires active internet connection
3. **Storage**: Large number of high-res images can consume significant storage
4. **API Costs**: Google APIs have usage quotas and may incur costs

## Improvements for Production

1. **Security**:
   - Use Android Keystore for API keys
   - Implement certificate pinning
   - Add ProGuard rules for obfuscation

2. **Performance**:
   - Implement image compression
   - Add pagination for large article lists
   - Background sync for OCR processing

3. **Features**:
   - Cloud backup/sync across devices
   - Export timeline as PDF
   - Share events and articles
   - Search and advanced filtering
   - Offline AI model for basic OCR

4. **Testing**:
   - Unit tests for repositories and ViewModels
   - Integration tests for database
   - UI tests with Espresso
   - OCR accuracy tests

5. **Accessibility**:
   - Content descriptions for screen readers
   - High contrast mode
   - Text scaling support

## Troubleshooting

### OCR not working
- Check internet connectivity
- Verify ML Kit dependencies are installed
- Check camera image quality

### Google Sign-in fails
- Verify OAuth credentials in Google Cloud Console
- Check SHA-1 fingerprint matches
- Ensure Google Play Services is updated

### Gemini API errors
- Verify API key is correct
- Check API quotas in Google Cloud Console
- Ensure internet connectivity

### Camera not opening
- Grant camera permissions
- Check device has rear camera
- Try restarting the app

## License

This project is created for educational and demonstration purposes.

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## Support

For issues, questions, or suggestions, please open an issue on the GitHub repository.

## Acknowledgments

- Google ML Kit for OCR capabilities
- Google Gemini for AI-powered analysis
- Material Design team for beautiful UI components
- Android Jetpack for modern architecture components
