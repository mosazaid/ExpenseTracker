Expense Tracker App - Complete Documentation
Overview
A modern Android expense tracking application built with Kotlin, Jetpack Compose, Room Database, and MVVM architecture with Repository pattern.
Features

Add/Edit Expenses & Income: Easy transaction entry with intuitive UI
Category Management: Predefined categories with ability to add custom ones
Account Types: Support for Cash and Bank accounts
Date/Time Selection: User-friendly date-time picker
History Views: Filter by Day, Week, Month, and Year
Statistics: Visual representation of spending patterns
Material Design 3: Modern UI following Google's design guidelines
Offline Support: All data stored locally using Room database

Architecture Patterns Used
1. MVVM (Model-View-ViewModel)

Model: Data layer with Room entities and repositories
View: Jetpack Compose UI screens
ViewModel: Business logic and state management

2. Repository Pattern

Abstraction layer between ViewModels and data sources
Centralized data access logic
Easy to test and maintain

3. Dependency Injection (Hilt)

Automatic dependency management
Improved testability
Reduced boilerplate code

4. Single Source of Truth

Room database as the primary data source
StateFlow for reactive UI updates

Project Structure
app/
├── src/main/java/com/expensetracker/
│   ├── data/
│   │   ├── database/
│   │   │   ├── entities/
│   │   │   ├── dao/
│   │   │   └── AppDatabase.kt
│   │   ├── repository/
│   │   └── di/
│   ├── domain/
│   │   ├── model/
│   │   └── repository/
│   ├── presentation/
│   │   ├── screens/
│   │   ├── components/
│   │   ├── viewmodel/
│   │   └── theme/
│   └── MainActivity.kt
└── build.gradle.kts
Key Components
Database Schema

Transaction: Main table for expenses/income
Category: Categories for transactions
Account: Cash/Bank account types

ViewModels

TransactionViewModel: Manages transaction operations
CategoryViewModel: Handles category management
HistoryViewModel: Manages filtered transaction history

Screens

AddTransactionScreen: Form for adding new transactions
HistoryScreen: List of transactions with filters
StatisticsScreen: Visual charts and summaries
CategoriesScreen: Manage transaction categories

Enhancement Ideas Implemented
1. Smart Category Suggestions

AI-powered category recommendations based on description
Learning from user patterns

2. Recurring Transactions

Set up monthly bills, salary, etc.
Automatic transaction creation

3. Budget Tracking

Set monthly budgets per category
Visual indicators for budget status

4. Export Functionality

Export data to CSV/Excel
Backup and restore features

5. Dark Theme Support

Automatic theme switching
Follows system preferences

6. Biometric Security

Secure app with fingerprint/face unlock
Protect sensitive financial data

7. Multi-Currency Support

Support for different currencies
Exchange rate conversion

8. Advanced Analytics

Spending trends
Category-wise analysis
Monthly comparisons

Installation & Setup
Prerequisites

Android Studio Arctic Fox or later
Kotlin 1.8+
Minimum SDK: 24 (Android 7.0)
Target SDK: 34 (Android 14)

Build Instructions

Clone the repository
Open in Android Studio
Sync project with Gradle files
Run the app on device/emulator

Gradle Dependencies
All required dependencies are included in the provided build.gradle.kts files.
Usage Guide

**Adding Transactions**
✅ Features included:

Amount input
Description input
Transaction type toggle (Income/Expense)
Category dropdown (filtered by type)
Account type selection (Cash/Bank)
Date picker
Save button with validation

**Viewing History**
 ✅ Features:

Filter chips: Day, Week, Month, Year
List of transactions from Room
Optional click-to-edit logic (navigation prepared)

**StatisticsScreen**
✅ Features:

Show total income, expense, and balance
Period range: Month by default
Simple bar chart (income vs expense)

**CategoriesScreen**
✅ Features:

View categories by type (Income, Expense)
Add/edit/delete categories
Choose color (basic)

Managing Categories

Go to Categories screen
Add new categories with icons and colors
Edit existing categories
Delete unused categories

Testing Strategy

Unit tests for ViewModels and Repository
Integration tests for Room database
UI tests for Compose screens
Instrumented tests for end-to-end flows

Performance Optimizations

Lazy loading for transaction lists
Database indexing for common queries
Image caching for category icons
Background operations using coroutines

Security Features

Local data encryption
Biometric authentication
No network requests (offline-first)
Secure backup mechanisms

Future Enhancements

Cloud Sync: Sync data across devices
AI Insights: Smart financial advice
Bill Reminders: Notification system
Photo Receipts: OCR for receipt scanning
Investment Tracking: Portfolio management
Family Sharing: Multi-user support

Troubleshooting
Common Issues

Database Migration: Handled automatically
Date Formatting: Follows system locale
Memory Usage: Optimized with pagination
Crash Recovery: Automatic state restoration

Performance Tips

Regular database cleanup
Limit transaction history display
Use appropriate image sizes for categories
Enable ProGuard for release builds

Contributing

Follow Kotlin coding conventions
Write tests for new features
Update documentation
Use conventional commit messages

.....................................................

✅ What is libs.versions.toml?
libs.versions.toml is a Gradle Version Catalog file. It's a centralized configuration file used to manage:

Versions of your dependencies
Library coordinates (group:name:version)
Plugin versions as well

✅ What does .toml mean?
.toml stands for Tom's Obvious, Minimal Language — it’s a configuration file format like .json or .yaml, but designed to be more human-readable and used mostly for settings and config data (especially in Gradle and Rust projects).

✅ What does libs.versions.toml do for you?
Instead of writing this in every build.gradle.kts:

implementation("androidx.core:core-ktx:1.12.0")
You write this once in libs.versions.toml:

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.12.0" }
And in your build file:

implementation(libs.androidx.core.ktx)
➡️ This makes upgrading, reusing, and keeping things clean much easier.