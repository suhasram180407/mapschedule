# Map Schedule

An Android application and home-screen widget that maps custom hand-drawn gestures to launch installed applications.

---

## Overview

Map Schedule enables users to draw custom vector shapes or characters and associate them with specific installed applications on their Android device. 

The application integrates with the Android launcher through a resizable home-screen widget. Tapping the widget opens an in-place sketch surface matching the widget's current dimensions and position. Drawing a registered gesture identifies the corresponding application and launches it immediately.

---

## Features

- **Gesture-to-App Mapping**: Create, edit, and delete 1-to-1 mappings between custom hand-drawn drawings and installed applications.
- **In-Place Widget Overlay**: Tapping the widget activates an interactive sketch pad directly aligned with the widget's screen position and size, whether shrunk, expanded, or repositioned.
- **Vector Gesture Matching**: Geometric normalization, bounding-box alignment, and trajectory comparison evaluate stroke count, path curvature, direction, and endpoint distances.
- **Triple-Tap Navigation**: Triple-tapping the widget surface directly opens the main management screen.
- **Master Toggle**: A master ON/OFF control in the main interface pauses all gesture launching without modifying existing mappings.
- **Local & Offline**: All drawing data and application mappings are persisted locally using atomic JSON file storage in private app storage. No internet connection or network permissions are required.
- **Dismiss on Outside Tap**: Tapping anywhere outside the interactive drawing area dismisses the sketch overlay cleanly without window transitions.

---

## Architecture & Implementation

### 1. Drawing & Gesture Recognition (`com.suhas.mapschedule.drawing`)
- **`DrawingCanvas`**: A Jetpack Compose canvas capturing raw pointer events into vector strokes (`List<DrawingStroke>`). Distinguishes quick taps from drag gestures.
- **`DrawingNormalization`**: Translates and scales stroke coordinates to a uniform reference grid while preserving aspect ratio and centering geometry.
- **`DrawingFingerprint`**: Extracts structural vector features (start/end points, bounding ratios, stroke sequencing) for preliminary filtering.
- **`GestureMatcher`**: Compares normalized live input strokes against stored references using Hausdorff distance, endpoint proximity, and trajectory angle consistency. Returns `Matched`, `Ambiguous`, or `NoMatch`.
- **`WidgetOverlayBoundsCalculator`**: Calculates exact pixel coordinates and dimensions for the transparent drawing overlay based on launcher `sourceBounds` and widget row/column spans.
- **`LiveDrawingActivity`**: A lightweight, translucent activity hosting the interactive drawing canvas directly over the widget.

### 2. Home-Screen Widget (`com.suhas.mapschedule.widget`)
- **`MapScheduleWidgetProvider`**: An `AppWidgetProvider` handling system broadcast events, configuration updates, and launcher resizing options.
- **`MapScheduleWidgetUpdater`**: Updates `RemoteViews` with launcher-specific dimension bundles (`OPTION_APPWIDGET_MIN_WIDTH`, `OPTION_APPWIDGET_MIN_HEIGHT`).

### 3. Data & Storage (`com.suhas.mapschedule.data`, `repository`)
- **`MappingStorage`**: Reads and writes mapping records atomically via JSON serialization in app-private storage.
- **`MappingRepository`**: In-memory synchronized cache providing thread-safe CRUD operations, validation checks, and state broadcasts.
- **`InstalledAppsProvider`**: Queries the Android `PackageManager` using `<queries>` intent filters to list launchable applications, icons, and package identifiers.

### 4. User Interface (`com.suhas.mapschedule.ui`)
- **`MainScreen`**: Displays active mappings with vector previews, action controls (Edit/Delete), master toggle, and an action button to add new mappings.
- **`AddMappingScreen` / `EditMappingScreen`**: Dedicated screens for drawing gestures, selecting target applications, and saving validated mappings.
- **`AppSelectionDialog`**: Filterable list dialog of installed applications with application names and icons.

---

## Project Structure

```
app/src/main/java/com/suhas/mapschedule/
├── MainActivity.kt                       # Main application entry point
├── data/
│   ├── InstalledAppsProvider.kt          # Installed application discovery
│   └── MappingStorage.kt                 # Atomic JSON persistence
├── drawing/
│   ├── DrawingCanvas.kt                  # Compose vector input canvas
│   ├── DrawingFingerprint.kt             # Structural feature extraction
│   ├── DrawingNormalization.kt           # Bounding box & aspect ratio normalization
│   ├── DrawingRenderer.kt                # Vector-to-bitmap rendering utilities
│   ├── DrawingSerializer.kt              # Coordinate serialization / deserialization
│   ├── DrawingThumbnail.kt               # UI thumbnail generation
│   ├── GestureMatcher.kt                 # Geometric stroke comparison & scoring
│   ├── LiveDrawingActivity.kt            # In-place interactive sketch overlay
│   └── WidgetOverlayBoundsCalculator.kt  # Adaptive widget screen coordinate calculations
├── model/
│   ├── AppMapping.kt                     # Application mapping data class
│   ├── DrawingStroke.kt                  # Vector stroke and point representations
│   └── WidgetConfiguration.kt            # Widget state models
├── repository/
│   └── MappingRepository.kt              # Single source of truth for mappings
├── ui/
│   ├── AddMappingScreen.kt               # New mapping creation screen
│   ├── AppSelectionDialog.kt             # Installed app picker dialog
│   ├── EditMappingScreen.kt              # Existing mapping modification screen
│   └── MainScreen.kt                     # Primary dashboard and mapping list
├── viewmodel/
│   └── MapScheduleViewModel.kt           # UI state holder & lifecycle management
└── widget/
    ├── MapScheduleWidgetProvider.kt      # AppWidgetProvider implementation
    ├── MapScheduleWidgetUpdater.kt       # RemoteViews builder & updates
    └── WidgetConfigurationActivity.kt    # Widget setup configuration flow
```

---

## Technical Specifications

| Parameter | Value |
|---|---|
| **Language** | Kotlin 2.3.20 |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Minimum SDK** | API 26 (Android 8.0 Oreo) |
| **Compile / Target SDK** | API 36 (Android 16) |
| **Java Compatibility** | Java 17 |
| **Build System** | Gradle 9.6.0 / AGP 9.0.1 |
| **Network Access** | None (`android.permission.INTERNET` not requested) |

---

## Building and Testing

### Prerequisites
- JDK 17 or higher
- Android SDK with platform API 36 and build-tools installed

### Build Debug APK
```bash
./gradlew assembleDebug
```
The output APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Run Unit Tests
```bash
./gradlew test
```
The test suite covers:
- Coordinate normalization and aspect ratio preservation.
- Stroke serialization and deserialization integrity.
- Gesture matching accuracy, ambiguity resolution, and rejection thresholds.
- Dynamic widget bounds calculations across varying screen positions and spans.
- Atomic storage persistence and repository state management.

---

## License

This project is maintained for personal and development use under the repository license.
