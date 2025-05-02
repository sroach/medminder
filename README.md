# MedMinder

MedMinder is a cross-platform medication tracking application built with Kotlin Multiplatform, targeting Android, iOS, and Desktop platforms.

## Description

MedMinder helps you keep track of your medications and schedules. It provides a simple and intuitive interface to manage your medication regimen and ensure you never miss a dose.

## Features

- List your medications and schedules
- Track your medication schedules
- Record when you've taken your medication
- View your medication history
- Dark and light mode support

## Privacy

MedMinder stores all your data locally on your device. No data is sent to external servers.

## Project Structure

This is a Kotlin Multiplatform project with the following structure:

* `/composeApp` - Contains code shared across all Compose Multiplatform applications
  - `commonMain` - Common code for all targets
  - Other platform-specific folders (iosMain, androidMain, etc.)

* `/iosApp` - iOS application entry point

## Technology

MedMinder is built using [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) and Compose Multiplatform for the UI.

## Version

Current version: 1.0.0