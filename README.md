<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Daily Report — POS, Expense Tracker & Balance Sheet

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-blue.svg)](https://developer.android.com/jetpack/compose)
[![Google Sheets](https://img.shields.io/badge/Cloud-Google%20Sheets%20API-34A853.svg)](https://sheets.google.com)
[![Offline First](https://img.shields.io/badge/Database-Room%20SQLite-orange.svg)](https://developer.android.com/training/data-storage/room)

**Daily Report** is a modern, offline-first business management application designed for salons, service providers, and small businesses. It combines an intuitive **Point of Sale (POS)**, real-time **Expense & Bill tracking**, a continuous **Day-by-Day Balance Sheet**, instant **Tax Invoices**, and automated **Google Sheets synchronization**.

> 📖 **Looking for in-depth documentation?** Check out **[USER_GUIDE.md](USER_GUIDE.md)** for exhaustive step-by-step operational workflows, troubleshooting, and screenshots guide.

---

## Key Features

- 🧾 **Point of Sale (POS)**: Fast service & product checkout with pre-loaded salon services (Threading, Waxing, Tinting, Facials, Hair Care), customizable item quantities, and on-the-fly product creation.
- 💸 **Expense & Bill Tracking**: Track daily operational expenses (supplies, refreshments) and fixed monthly bills (rent, utilities) with categorized notes.
- 📊 **Continuous Day-by-Day Balance Sheet**: See net cashflow for every day, continuous carry-over balance brought forward, and expandable transaction cards with live sync status badges.
- 📄 **Tax Invoices & Shareable Receipts**: Generate formatted tax receipts with business registration details (ABN/ACN, address, phone, email) and share directly via WhatsApp, SMS, or Bluetooth printer.
- 📑 **One-Tap PDF Export**: Generate professional A4 financial reports saved directly to your device's `Downloads` folder.
- ☁️ **Google Sheets Cloud Sync**: Real-time two-way synchronization into your personal Google Spreadsheet organized into clean monthly tabs (`Sheet1`, `Products`, `Sep26`, etc.).
- 📴 **100% Offline-First**: Works reliably without an internet connection using an internal Room SQLite database, automatically queuing and syncing records when back online.

---

## Quick Setup Guide

### 1. Install the APK
Download or copy `DailyReport.apk` located in the root directory to your Android phone or tablet, open the file, and tap **Install**.

### 2. Connect Your Google Spreadsheet (Takes 2 minutes)
1. Open [Google Sheets](https://sheets.google.com) and create a new blank spreadsheet.
2. In Google Sheets, click **Extensions > Apps Script**.
3. Replace any code in `Code.gs` with the code from **`Code.gs`** in this project (or tap **View & Copy Apps Script Code** inside the app setup screen).
4. Click **Deploy > New deployment**:
   - **Select type**: Web app
   - **Description**: `Daily Report Backend`
   - **Execute as**: `Me (your-email@gmail.com)`
   - **Who has access**: `Anyone`
5. Click **Deploy**, authorize access, and copy the Web App URL (ending in `/exec`).
6. Open the app, fill in your **Business Details** (Name, ABN, Address, Phone, Email), paste your **Web App URL**, and tap **Test Connection** followed by **Save & Open Dashboard**.

*(You can also tap "Continue in Offline-Only Mode" to test the app without setting up Google Sheets immediately).*

---

## How to Use the App

### 1. Recording Daily Income (POS)
1. From the Dashboard, tap **+ Daily Income**.
2. Tap items or search by name to add them to your cart.
3. Select payment method (**Cash** or **Card / EFTPOS**).
4. Tap **Save Daily Income**.
5. The **Tax Invoice** appears instantly with business details and line items. Tap **Share Invoice** to send it via WhatsApp, SMS, or email.

### 2. Logging Expenses & Bills
- Tap **- Expenses** to log inventory purchases, consumable supplies, and daily operational costs.
- Tap **- Bills** to log scheduled recurring overheads like shop rent and electricity.

### 3. Reviewing & Managing the Balance Sheet
- Tap **= Balance Sheet** to view total cumulative balance and day-by-day cashflow.
- **Expand Day**: Tap any date card to view all sales, expenses, and bills recorded on that day.
- **Edit**: Tap the **Pencil icon** to modify transaction amounts or notes.
- **Delete**: Tap the **'X' icon** to delete an accidental entry. Formulas and balances recalculate automatically.
- **Export PDF**: Tap **Download PDF** to export a formatted financial summary table to your device's `Downloads` folder.

---

## Managing Products in Google Sheets

You can edit services and prices directly in Google Sheets from your computer browser:
1. Open your Google Spreadsheet and go to the **`Products`** tab.
2. Update prices in **Column B** or add new service names in **Column A**.
3. In the Android app, tap the **Refresh icon** on the Dashboard — your new products and prices will sync automatically!

---

## Developer & Local Build Instructions

### Tech Stack
- **Framework**: Jetpack Compose (Material 3)
- **Language**: Kotlin
- **Architecture**: MVVM + Repository pattern with Coroutines & StateFlow
- **Persistence**: Room Database (SQLite) + Encrypted SharedPreferences
- **Networking**: HttpURLConnection with JSON payload serialization for Google Apps Script Web App
- **Reporting**: Android Canvas `PdfDocument` generation (A4 layout)

### Building Locally with Android Studio
1. Open [Android Studio](https://developer.android.com/studio) (Meerkat or newer).
2. Select **Open** and choose this project folder.
3. Allow Gradle to sync dependencies.
4. Run on an Android emulator or connected device (Android 8.0+ / API 26+).
5. Build output APKs are located at `app/build/outputs/apk/debug/app-debug.apk` or in the root directory as `DailyReport.apk`.
