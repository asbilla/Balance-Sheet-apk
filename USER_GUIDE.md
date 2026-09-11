# Daily Report — Comprehensive User Guide & Operations Manual

Welcome to **Daily Report**, an all-in-one business management, Point of Sale (POS), expense tracking, and day-by-day financial balance sheet application built for Android and seamlessly integrated with **Google Sheets**.

This guide covers all features, functions, setup steps, and workflows to help you manage your daily business operations, track income and expenses, print tax invoices, and export financial PDF reports.

---

## Table of Contents
1. [App Overview & Architecture](#1-app-overview--architecture)
2. [Step-by-Step Initial Setup](#2-step-by-step-initial-setup)
   - [Step 1: Create Your Google Spreadsheet](#step-1-create-your-google-spreadsheet)
   - [Step 2: Deploy Google Apps Script Backend](#step-2-deploy-google-apps-script-backend)
   - [Step 3: Configure In-App Business Profile & Link Sheet](#step-3-configure-in-app-business-profile--link-sheet)
3. [Dashboard & Quick Navigation](#3-dashboard--quick-navigation)
4. [Point of Sale (POS) & Daily Income](#4-point-of-sale-pos--daily-income)
   - [Product Catalog & Salon Services](#product-catalog--salon-services)
   - [Adding Items to Cart & Custom Amounts](#adding-items-to-cart--custom-amounts)
   - [Adding New Products on the Fly](#adding-new-products-on-the-fly)
   - [Payment Methods & Recording Income](#payment-methods--recording-income)
   - [Tax Invoice & Receipt Sharing](#tax-invoice--receipt-sharing)
5. [Logging Expenses & Operational Costs](#5-logging-expenses--operational-costs)
6. [Logging Bills & Fixed Overheads](#6-logging-bills--fixed-overheads)
7. [Day-by-Day Balance Sheet](#7-day-by-day-balance-sheet)
   - [Overall Cumulative Summary](#overall-cumulative-summary)
   - [Continuous Cumulative Carry-Over Balance](#continuous-cumulative-carry-over-balance)
   - [Daily Breakdown Cards & Transaction Expansion](#daily-breakdown-cards--transaction-expansion)
   - [Editing a Transaction](#editing-a-transaction)
   - [Deleting a Transaction](#deleting-a-transaction)
   - [Exporting PDF Balance Sheet](#exporting-pdf-balance-sheet)
8. [Managing Your Google Spreadsheet Backend](#8-managing-your-google-spreadsheet-backend)
   - [Sheet Structure Overview](#sheet-structure-overview)
   - [Managing Products Directly in Google Sheets](#managing-products-directly-in-google-sheets)
   - [Dynamic Monthly Tabs (e.g., Sep26, Oct26)](#dynamic-monthly-tabs-eg-sep26-oct26)
9. [Offline Mode & Data Synchronization](#9-offline-mode--data-synchronization)
10. [APK Installation, Updates & Auto-Versioning](#10-apk-installation-updates--auto-versioning)
11. [Troubleshooting & Frequently Asked Questions (FAQ)](#11-troubleshooting--frequently-asked-questions-faq)

---

## 1. App Overview & Architecture

**Daily Report** provides small businesses, salons, service providers, and retail stores with an intuitive tool to manage daily cashflow:

- **Point of Sale (POS)**: Fast service/item selection, shopping cart, cash and card toggles, and instant professional tax invoices.
- **Expense & Bill Tracker**: Log supplies, inventory, utilities, and salon rent with categorized notes.
- **Day-by-Day Balance Sheet**: See net cashflow for every day, continuous carry-over balance, and expandable transaction breakdowns.
- **Dual-Storage Engine**:
  - **Local Room Database**: High-speed, offline-first SQLite database stored safely on your Android device.
  - **Google Sheets Cloud Backend**: Real-time two-way synchronization into your own personal Google Spreadsheet.
- **PDF Export**: Generate professional tax-compliant PDF balance sheets saved to your device Downloads folder and viewable on any device.

---

## 2. Step-by-Step Initial Setup

Setting up your Google Sheets backend takes only 2–3 minutes. Follow these exact steps:

### Step 1: Create Your Google Spreadsheet
1. On your computer or mobile browser, go to [Google Sheets](https://sheets.google.com).
2. Create a new, blank spreadsheet.
3. Rename the spreadsheet at the top left to something descriptive, e.g., `Daily Business Report` or `Salon Income Ledger`.

### Step 2: Deploy Google Apps Script Backend
1. In your Google Sheet, click on **Extensions** in the top menu and select **Apps Script**.
2. A new tab will open showing the Apps Script code editor with an empty `myFunction()`. Delete any text currently in `Code.gs`.
3. Copy the complete backend script:
   - You can copy the code directly from the **`Code.gs`** file in the project repository, OR
   - Inside the Android app, go to the Setup screen, tap **View & Copy Apps Script Code**, and tap **Copy Full Code**.
4. Paste the entire code into `Code.gs` in the Google Apps Script editor.
5. Click the **Save** icon (diskette) or press `Ctrl + S` (or `Cmd + S` on Mac).
6. Click the blue **Deploy** button at the top right and choose **New deployment**.
7. In the configuration popup:
   - Click the gear icon next to "Select type" and choose **Web app**.
   - **Description**: Enter `Daily Report Backend`.
   - **Execute as**: Select **Me (your-email@gmail.com)**. *(Crucial)*
   - **Who has access**: Select **Anyone**. *(Crucial for the Android app to connect)*
8. Click **Deploy**.
9. If prompted, click **Authorize access**, choose your Google account, click **Advanced**, and select **Go to Daily Report Backend (unsafe)**, then click **Allow**.
10. Copy the generated **Web app URL**. It looks like:
    ```
    https://script.google.com/macros/s/AKfycb.../exec
    ```
    *(Note: Ensure the URL ends in `/exec`, NOT `/dev`)*.

### Step 3: Configure In-App Business Profile & Link Sheet
1. Open the **Daily Report** app on your Android device.
2. If this is your first time opening the app, you will see the **Sheet Integration Setup** screen. (You can also access this anytime by tapping the **Settings Gear** icon on the Dashboard).
3. Fill in your **Business Registration Details**:
   - **1st for Business Name**: (e.g., `Sunshine Beauty & Threading`)
   - **2nd for ABN/ACN**: (e.g., `ABN 12 345 678 901`)
   - **3rd for Business Address**: (e.g., `120 Collins St, Melbourne VIC 3000`)
   - **4th for Phone/Mobile**: (e.g., `+61 400 123 456`)
   - **5th for Email Address**: (e.g., `contact@sunshinebeauty.com`)
   *(These details appear at the top of your Google Sheet `Sheet1`, on customer Tax Invoices, and on generated PDF reports).*
4. In the **Web App URL** field, tap **Paste** to insert your Google Apps Script URL.
5. Tap **Test Connection**. Within 1–2 seconds, you will see a green checkmark stating:
   `"Connected! Google Sheets responded successfully."`
6. Tap **Save & Open Dashboard**.

> **Offline-Only Mode Option**: If you want to use the app immediately without internet or without linking Google Sheets yet, tap **"Continue in Offline-Only Mode (Configure Later)"** at the bottom of the setup screen.

---

## 3. Dashboard & Quick Navigation

The Dashboard is your operational command center:

```
+-----------------------------------------------------------+
|  [Salon Icon]  Sunshine Beauty & Threading        [Gear]  |
+-----------------------------------------------------------+
|  CUMULATIVE NET BALANCE                                   |
|  $2,450.00                              [ SURPLUS badge ] |
|  -------------------------------------------------------  |
|  Income: +$3,200.00 | Expenses: -$450.00 | Bills: -$300.00|
|  [Check] Sheets Connected (Synced just now)     [Refresh] |
+-----------------------------------------------------------+
|                                                           |
|  [ + Daily Income ]           [ - Expenses ]              |
|  Log sales & POS services     Supplies & daily spend      |
|                                                           |
|  [ - Bills ]                  [ = Balance Sheet ]         |
|  Rent, power, utilities       Day-by-day financial report |
|                                                           |
+-----------------------------------------------------------+
```

### Dashboard Elements:
- **Header**: Shows your Business Name and the **Settings Gear** button (tap anytime to edit your business details or update your Sheets URL).
- **Cumulative Net Balance Card**: Live summary of your total business financial position with a `SURPLUS` (green) or `DEFICIT` (red) badge.
- **Sync Status**: Displays whether the app is currently connected to Google Sheets, with timestamp of last sync.
- **Manual Refresh Icon**: One-tap synchronization that fetches the latest records from Google Sheets and reconciles local entries.
- **The 4 Primary Action Tiles**:
  1. **Daily Income** (Green): Opens the POS and sales entry screen.
  2. **Expenses** (Red): Log operational costs and consumable supplies.
  3. **Bills** (Orange): Log recurring utility, rental, or vendor bills.
  4. **Balance Sheet** (Blue): Browse daily balance ledgers, edit/delete entries, and download PDF reports.

---

## 4. Point of Sale (POS) & Daily Income

Tap **Daily Income** from the Dashboard to open the sales register.

### Product Catalog & Salon Services
The app comes pre-configured with popular beauty and threading salon services categorized for quick selection:
- **Threading**: Eyebrows ($10.00), Upper Lips ($5.00), Chin ($5.00), Forehead ($5.00), Sideburns ($12.00), Neck ($5.00), Full Face ($35.00).
- **Body Waxing**: Underarms ($15.00), Full Arms ($30.00), 1/2 Arms ($20.00), 3/4 Arms ($25.00), Full Legs ($45.00), 1/2 Legs w Knee ($25.00), 1/2 Legs Below Knee ($20.00), Back ($25.00), 1/2 Back ($20.00), Stomach ($20.00), Back of Neck ($15.00).
- **Facial Waxing**: Sideburns ($15.00), Chin ($7.00), Upper Lips ($7.00), Neck ($7.00).
- **Tinting**: Eyebrow ($12.00), Eyelash ($20.00), EBT+ELT ($30.00), Henna Herbal Henna + Oil ($25.00 - $30.00).
- **Herbal Facial**: 20min Clean Up (Cleanse, Scrub, Face Pack) ($20.00).
- **Hair Care**: Hair Oil Massage 10 min ($15.00), Hand Henna (from $10.00).

### Adding Items to Cart & Custom Amounts
1. **Filter by Category**: Tap any category tab at the top (e.g., `Threading`, `Body Waxing`, `Tinting`, `All`).
2. **Search**: Use the search bar to filter services by typing (e.g., typing "lip" filters instantly to Upper Lips).
3. **Add to Cart**: Tap on any service card or press the `+` button.
   - The item counter appears with `+` and `-` buttons to adjust quantities.
   - The order subtotal updates automatically at the bottom.
4. **Custom Ad-Hoc Amount**: If a customer purchases a custom package or provides a tip, enter the amount in the **"Custom / Manual Amount ($)"** field.

### Adding New Products on the Fly
Need to sell a new service or retail item not currently in the catalog?
1. Tap the **"+ Add Product"** button in the top right.
2. Enter:
   - **Product / Service Name** (e.g., `Hydro Facial Deluxe`)
   - **Price ($)** (e.g., `65.00`)
   - **Category** (e.g., `Facial`)
3. Tap **Save Product**.
4. The new item is saved to your local device catalog and synchronized directly to your Google Sheet's **"Products"** tab!

### Payment Methods & Recording Income
1. **Select Transaction Date**: Defaults to today's date. Tap the calendar icon if entering backdated sales.
2. **Choose Payment Method**: Tap **Cash** or **Card / EFTPOS**.
3. **Optional Customer Notes**: Add customer name or custom notes if desired.
4. Tap the large green button: **"Save Daily Income ($XX.XX)"**.
5. The sale is immediately committed to the local database, synced to your Google Sheet, and the **Tax Invoice Dialog** opens automatically!

### Tax Invoice & Receipt Sharing
Immediately upon completing an Income transaction, a professional **TAX INVOICE** dialog appears:
- Contains your Business Name, ABN/ACN, Address, Phone, and Email.
- Includes a unique Receipt Number and Date.
- Displays an itemized table with descriptions, quantities, unit prices, and line totals.
- Displays **GRAND TOTAL**.
- Tap **SHARE INVOICE**: Opens the standard Android share sheet to send the itemized invoice text directly via **WhatsApp**, **SMS**, **Email**, or to a **Bluetooth receipt printer**.

---

## 5. Logging Expenses & Operational Costs

Tap **Expenses** from the Dashboard to record business overheads and day-to-day costs (e.g., purchasing waxing wax, thread rolls, sanitizers, coffee/tea, retail goods).

### How to Log an Expense:
1. **Date**: Defaults to today. Tap to choose a different date if recording past receipts.
2. **Amount ($)**: Enter the dollar value (e.g., `45.50`).
3. **Category / Notes**: Describe what was purchased (e.g., `Waxing cartridges and strips`, `Disinfectant spray`, `Client refreshments`).
4. **Payment Method**: Select **Cash**, **Card**, or **Bank Transfer**.
5. Tap **Record Expense**.
6. The expense is recorded locally and appended to the monthly sheet under the **"Expense & Bills"** column, deducting from your running balance.

---

## 6. Logging Bills & Fixed Overheads

Tap **Bills** from the Dashboard to record scheduled recurring overheads (e.g., salon rent, electricity, water, internet, merchant terminal fees, insurance).

### How to Log a Bill:
1. **Date**: Select the payment date.
2. **Amount ($)**: Enter the bill amount (e.g., `550.00`).
3. **Bill Category / Vendor**: Specify the bill type (e.g., `Shop Rent - Week 37`, `Energy Australia Electricity`, `Telstra Broadband`).
4. Tap **Record Bill**.
5. The bill is logged, deducted from the daily balance, and reflected across your balance sheets and Google Sheets.

---

## 7. Day-by-Day Balance Sheet

Tap **Balance Sheet** from the Dashboard to open the financial ledger.

### Overall Cumulative Summary
At the top of the screen, an aggregate card shows:
- **Cumulative Net Balance**: Total income minus expenses and bills across all recorded transactions.
- **SURPLUS / DEFICIT Badge**: Color-coded indicator of overall profitability.
- **Total Income**, **Total Expenses**, and **Total Bills** summary totals.

### Continuous Cumulative Carry-Over Balance
The balance sheet automatically calculates day-by-day continuous running balance:
$$\text{Ending Cumulative Balance} = \text{Previous Carried Balance} + \text{Day Net Balance}$$
When viewing any day card, you can clearly see:
- *From previous:* the carry-over balance brought forward from prior days.
- *Day net:* net income/expense for this specific date.
- *Balance:* final cumulative balance at the end of that day.

### Daily Breakdown Cards & Transaction Expansion
- Each recorded date is represented by a clean card sorted newest date first.
- The card displays 3 category breakdown pills:
  - **Income** (Green)
  - **Expenses** (Red)
  - **Bills** (Orange)
- **Tap any Date Card** to expand and reveal all individual transactions recorded on that day.
- Each transaction shows:
  - Service/Item name and category note.
  - Transaction type badge.
  - Amount (+Green for income, -Red for expense/bill).
  - Sync indicator (Green checkmark = synced with Google Sheets; Orange cloud = pending sync).

### Editing a Transaction
Made a typing mistake or need to correct an amount?
1. Tap the date card to expand its transactions.
2. Tap the **Pencil (Edit)** icon next to the transaction.
3. A modal dialog opens showing the current recorded details.
4. Enter the corrected **New Amount ($)** and update the **Notes / Category**.
5. Tap **Save & Update Sheet**.
6. The app updates the local SQLite database and immediately sends an update command to Google Sheets, automatically refreshing the running balances.

### Deleting a Transaction
Need to remove an accidental duplicate or test entry?
1. Tap the date card to expand its transactions.
2. Tap the **'X' (Delete)** icon next to the transaction.
3. A confirmation dialog appears asking:
   `"Are you sure you want to delete this transaction of $XX.XX on YYYY-MM-DD?"`
4. Tap **Delete**.
5. The transaction is permanently deleted from your local device database and removed from Google Sheets, with formulas recalculating automatically.

### Exporting PDF Balance Sheet
Need to print a report for your accountant, tax return, or weekly filing?
1. In the Balance Sheet screen, tap **"Download PDF (Whole Spreadsheet)"** or tap the **PDF icon** in the top bar.
2. The app automatically creates a clean, A4-formatted PDF containing:
   - Your Business Header (Name, ABN/ACN, Address, Phone, Email).
   - Report generation date and time.
   - Financial Summary Box (Total Income, Expenses, Bills, and Net Balance).
   - Complete itemized Day-by-Day table with Date, Income, Expenses, Bills, Day Net, and Running Cumulative Balance.
3. The PDF is saved directly to your device's `Downloads` folder as:
   `DailyReport_BalanceSheet_YYYYMMDD_HHMMSS.pdf`
4. The app immediately prompts to open the PDF in your preferred viewer (Google Drive PDF Viewer, Adobe Acrobat, Print Service, etc.).
5. If connected to Google Sheets, it also triggers Google Sheets native PDF export.

---

## 8. Managing Your Google Spreadsheet Backend

Your Google Spreadsheet is organized into clean, structured sheets designed to be human-readable and formula-driven.

### Sheet Structure Overview

| Sheet Name | Description |
| :--- | :--- |
| **`Sheet1`** | **Business Profile & Registration Details**: Stores your Business Name, ABN/ACN, Address, Phone, Email, and formatted "Last Updated" timestamp. |
| **`Products`** | **POS Catalog**: Contains `Product / Item Name`, `Price`, and `Category`. Used to populate POS quick suggestions. |
| **`MmmYY`** *(e.g. `Sep26`, `Oct26`)* | **Monthly Financial Ledgers**: Created dynamically for each calendar month. Contains formatted tables with running balance formulas. |

### Managing Products Directly in Google Sheets
You don't need to configure everything in the app — you can manage your salon services directly from your computer:
1. Open your Google Spreadsheet in a browser.
2. Switch to the **`Products`** tab.
3. You will see 3 columns:
   - **Column A**: `Product / Item Name`
   - **Column B**: `Price`
   - **Column C**: `Category`
4. You can edit existing prices, rename services, or add new rows at the bottom.
5. The next time you open the POS screen in the app or tap the Refresh button, the app will automatically load your new products and prices!

### Dynamic Monthly Tabs (e.g., Sep26, Oct26)
- The backend automatically organizes transactions into monthly sheets based on the transaction date (e.g. `Sep26` for September 2026).
- **Opening Balance Carry-Over**: When a new month begins, row 2 of the new monthly sheet automatically carries forward the ending balance from the previous month with the label `Opening Balance: Balance brought forward from MmmYY`.
- **Formulas**: Column F uses native Google Sheets formulas (`=SUM(D$2:D2)-SUM(E$2:E2)`) so your balance stays mathematically exact.
- **Hidden ID Columns**: Columns G and H store transaction IDs and timestamps; they are kept hidden to ensure your spreadsheet remains clean and easy to print.

---

## 9. Offline Mode & Data Synchronization

You never have to worry about spotty Wi-Fi or mobile reception:

1. **Offline Recording**: You can record sales, expenses, and bills without an active internet connection. All entries are saved locally in the device's encrypted Room SQLite database.
2. **Visual Status Badges**:
   - In the Balance Sheet, transactions that have been synced to Google Sheets show a **Green Checkmark**.
   - Any transactions recorded offline show an **Orange Cloud** icon indicating they are queued.
3. **Automatic & Manual Syncing**:
   - As soon as your device reconnects to Wi-Fi or mobile data, queued transactions are synchronized to Google Sheets.
   - You can tap the **Refresh Icon** on the Dashboard or Balance Sheet at any time to force an immediate two-way synchronization.

---

## 10. APK Installation, Updates & Auto-Versioning

### Locating the APK
The latest compiled Android package is located directly in the root directory:
```
/DailyReport.apk
```

### Installing on Your Android Device:
1. Download or transfer `DailyReport.apk` to your Android phone or tablet.
2. Tap the downloaded file.
3. If prompted by Android security, select **Settings** and toggle **"Allow from this source"** (standard for direct APK installs).
4. Tap **Install** (or **Update** if updating an existing install).

### Automatic Versioning:
- The app uses an automated build-timestamp versioning system.
- Every time modifications are made, Gradle automatically sets:
  - `versionCode` = Unique numeric timestamp.
  - `versionName` = `1.0.YYYYMMDD.HHMM` (e.g., `1.0.20260910.1704`).
- Upgrading to a newer APK automatically preserves all your local data, business profiles, and cached transactions.

---

## 11. Troubleshooting & Frequently Asked Questions (FAQ)

#### Q: The app displays "Connection failed" when testing the Web App URL.
**A:** Check the following:
1. Make sure your Web App URL ends with `/exec` (e.g., `https://script.google.com/macros/s/.../exec`). If it ends in `/dev`, it will not work on external devices.
2. In Google Apps Script, click **Deploy > Manage deployments**. Verify that:
   - **Execute as** is set to **Me**.
   - **Who has access** is set to **Anyone**. (If set to "Only myself", Android cannot connect).
3. If you modified `Code.gs`, remember to click **Deploy > New deployment** (or edit the active deployment to "New version") to apply changes.

#### Q: How do I change my Business Name, Address, or ABN?
**A:** On the Dashboard, tap the **Settings Gear** icon in the top right corner. Update your business information and tap **Save & Open Dashboard**. Your Google Sheet `Sheet1` will be updated immediately.

#### Q: How do I back up my data?
**A:** Your data is already backed up! Every transaction synced to Google Sheets lives securely in your Google Drive. Even if you lose or replace your Android phone, installing the app on a new phone and entering your Web App URL will restore your transactions and products.

#### Q: Can I run the app on multiple phones/tablets for the same salon?
**A:** Yes! Simply install the APK on each staff member's device and enter the same Google Apps Script Web App URL during setup. All devices will sync into the same Google Sheet.

#### Q: Where are exported PDF files stored?
**A:** Exported PDFs are stored in your device's standard **`Downloads`** folder (e.g., `Internal Storage / Download`). You can also share them immediately using the system share sheet.

---

*Daily Report — Streamlined Business Financial Reporting & POS for Android & Google Sheets.*
