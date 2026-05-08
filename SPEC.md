# Transparent Keyboard Android App

## Project Overview
- **Project Name**: TransparentKeyboard
- **Type**: Android Input Method Editor (IME)
- **Core Functionality**: A simple virtual keyboard that appears transparent over other apps

## Technology Stack
- **Language**: Kotlin
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)
- **Build Tool**: Gradle with Kotlin DSL

## Features
- Transparent keyboard background
- Basic QWERTY layout (A-Z keys)
- Space bar and Enter key
- Backspace key
- Keyboard toggles on demand

## UI/UX Specification
- Keyboard layout: Standard QWERTY (3 rows + bottom row)
- Key style: Semi-transparent white keys with dark text
- Background: Fully transparent (#00000000)
- Key press: Ripple effect with slight opacity change

## Implementation
1. Create Android project structure
2. Implement KeyboardView with custom key drawing
3. Create InputMethodService to handle keyboard input
4. Configure AndroidManifest for IME