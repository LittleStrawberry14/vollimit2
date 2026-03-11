# Volume Limit 🎧

![App Icon](app/src/main/res/drawable/ic_launcher_new.png)

A lightweight, efficient Android application to enforce a maximum volume limit on your device.

## The Story Behind the App
This app was **vibecoded** with love and a bit of frustration! 😅

It was originally created for my parents because my little sister has a habit of cranking the volume to the absolute maximum while watching YouTube Shorts. To save everyone's ears (and the phone's speakers), I built this tool to keep the volume at a safe, parent-approved level.

## Features
- **Strict Volume Limiting**: Set a percentage and the app will ensure the media volume never exceeds it.
- **Event-Driven & Efficient**: Uses Android's `ContentObserver` to listen for volume changes. It stays completely idle until you press a volume button, making it extremely battery and RAM friendly.
- **Persistent Service**: Runs as a foreground service with a low-priority notification to ensure Android doesn't kill it.
- **Battery Optimization Aware**: Includes a prompt to ignore battery optimizations for 100% reliability.
- **Multilingual**: Supports both **English** and **Arabic**, automatically following your device's language.
- **Visual Feedback**: Clean, Material 3 UI with smooth animations and scale-based button feedback.

## F-Droid Compatibility
This app is designed to be fully open-source friendly:
- **No Proprietary Binary Blobs**: All icons and resources are local.
- **No Tracking/Analytics**: 100% private.
- **Standard Build System**: Uses standard Gradle/Kotlin DSL.
- **Optimized Resources**: All images have been optimized and resized using FFmpeg for minimum footprint.

## Credits
- **Vibecoded by Gemini CLI** for the developer.
- Originally made for my parents to survive the YouTube Shorts volume wars.

---
*Note: This repository is currently private and will be moved to a public, open-source license soon.*
