package com.example.util

object GoogleAppsScriptSnippet {
    val CODE: String = """
/**
 * Daily Business Reporting Tool - Google Apps Script Backend
 * 
 * FEATURES:
 * 1. Automatic Monthly Sheets: Groups transactions by month (e.g., Sep26, Oct26, Nov26, Jan27).
 *    Creates a new sheet automatically whenever a new month begins.
 * 2. Automatic Balance Carry-Over: Takes over ending balance from previous month (e.g., $2,540.00 from Sep26)
 *    and seamlessly continues balance calculation into the new month.
 * 3. Clean Columns:
 *    - Date
 *    - Type (Daily Income / Expense / Bill / Opening Balance)
 *    - Notes
 *    - Income
 *    - Expense & Bills
 *    - Balance (Auto-calculates all Income minus Expense & Bills)
 * 4. Hidden Metadata: ID and Created At columns are placed in columns G & H and automatically hidden.
 * 5. Auto-formatted currency ($#,##0.00) and frozen header rows.
 * 6. PDF Export API: Enables one-click downloading of the complete spreadsheet as a PDF.
 * 
 * DEPLOYMENT INSTRUCTIONS:
 * 1. In your Google Sheet, click Extensions > Apps Script.
 * 2. Delete any existing code in Code.gs and paste this entire code.
 * 3. Click 'Save' (floppy disk icon).
 * 4. Click 'Deploy' > 'New deployment' (or Manage deployments > Edit > New version).
 * 5. Click the gear icon next to 'Select type' and choose 'Web app'.
 * 6. Set:
 *    - Description: Monthly Business Reports API with Carry-Over Balance & PDF Export
 *    - Execute as: Me (<your email>)
 *    - Who has access: Anyone
 * 7. Click 'Deploy', authorize with your Google account, and copy the Web App URL (/exec).
 */

var MONTH_NAMES = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

/**
 * Derives the month sheet name (e.g., 'Sep26', 'Oct26', 'Jan27') from a date string (YYYY-MM-DD) or Date object.
 */
function getMonthSheetName(dateInput) {
  var d = new Date();
  if (dateInput) {
    if (dateInput instanceof Date) {
      d = dateInput;
    } else if (typeof dateInput === "string") {
      var parts = dateInput.trim().split("-");
      if (parts.length === 3) {
        var year = parseInt(parts[0], 10);
        var month = parseInt(parts[1], 10) - 1;
        var day = parseInt(parts[2], 10);
        d = new Date(year, month, day);
      } else {
        var parsed = new Date(dateInput);
        if (!isNaN(parsed.getTime())) d = parsed;
      }
    }
  }
  var monthAbbr = MONTH_NAMES[d.getMonth()];
  var year2Digits = String(d.getFullYear()).slice(-2);
  return monthAbbr + year2Digits; // e.g. Sep26, Oct26, Jan27
}

/**
 * Calculates the previous chronological month sheet name.
 * e.g., 'Oct26' -> 'Sep26', 'Jan27' -> 'Dec26'
 */
function getPreviousMonthSheetName(sheetName) {
  if (!sheetName || sheetName.length < 5) return null;
  var abbr = sheetName.slice(0, 3);
  var yr = parseInt(sheetName.slice(3), 10);
  var idx = MONTH_NAMES.indexOf(abbr);
  if (idx === -1) return null;

  var prevIdx = idx - 1;
  var prevYr = yr;
  if (prevIdx < 0) {
    prevIdx = 11;
    prevYr = (yr - 1 + 100) % 100;
  }
  var prevYrStr = (prevYr < 10 ? "0" : "") + prevYr;
  return MONTH_NAMES[prevIdx] + prevYrStr;
}

/**
 * Retrieves the ending balance from the last row of the previous month's sheet.
 */
function getLastMonthEndingBalance(ss, prevSheetName) {
  if (!prevSheetName) return 0.0;
  var prevSheet = ss.getSheetByName(prevSheetName);
  if (!prevSheet) return 0.0;
  var lastRow = prevSheet.getLastRow();
  if (lastRow <= 1) return 0.0;

  // Column F (6) is the Balance column
  var balanceVal = prevSheet.getRange(lastRow, 6).getValue();
  var num = parseFloat(balanceVal);
  return isNaN(num) ? 0.0 : num;
}

/**
 * Ensures an Opening Balance row is placed on row 2 if carrying over from a previous month.
 */
function checkAndInsertOpeningBalance(ss, sheet, sheetName) {
  if (sheet.getLastRow() === 1) {
    var prevSheetName = getPreviousMonthSheetName(sheetName);
    var prevBalance = getLastMonthEndingBalance(ss, prevSheetName);
    if (prevBalance !== 0) {
      var abbr = sheetName.slice(0, 3);
      var yr = parseInt(sheetName.slice(3), 10) + 2000;
      var mIdx = MONTH_NAMES.indexOf(abbr) + 1;
      var mStr = (mIdx < 10 ? "0" : "") + mIdx;
      var openDate = yr + "-" + mStr + "-01";

      sheet.appendRow([
        openDate,
        "Opening Balance",
        "Balance brought forward from " + prevSheetName,
        prevBalance > 0 ? prevBalance : "",
        prevBalance < 0 ? Math.abs(prevBalance) : "",
        '=SUM(D$2:D2)-SUM(E$2:E2)',
        'opening-' + sheetName,
        new Date().toISOString()
      ]);

      var formatRange = sheet.getRange(2, 4, 1, 3);
      formatRange.setNumberFormat("$#,##0.00");
      sheet.hideColumns(7, 2);
      return true;
    }
  }
  return false;
}

/**
 * Gets or creates the monthly sheet with properly formatted headers and hidden metadata columns.
 */
function getOrCreateMonthlySheet(ss, sheetName) {
  var sheet = ss.getSheetByName(sheetName);
  if (!sheet) {
    sheet = ss.insertSheet(sheetName);

    // Headers: visible columns (A-F), hidden columns (G-H)
    var headers = ["Date", "Type", "Notes", "Income", "Expense & Bills", "Balance", "ID", "Created At"];
    sheet.appendRow(headers);

    // Header styling
    var headerRange = sheet.getRange("A1:F1");
    headerRange.setFontWeight("bold")
               .setBackground("#1E3A8A")
               .setFontColor("#FFFFFF")
               .setHorizontalAlignment("center");

    sheet.setFrozenRows(1);

    // Column widths for readability
    sheet.setColumnWidth(1, 115); // Date
    sheet.setColumnWidth(2, 135); // Type
    sheet.setColumnWidth(3, 230); // Notes
    sheet.setColumnWidth(4, 130); // Income
    sheet.setColumnWidth(5, 145); // Expense & Bills
    sheet.setColumnWidth(6, 140); // Balance

    // Hide ID and Created At (columns G and H)
    sheet.hideColumns(7, 2);

    // Check and carry over previous month's ending balance
    checkAndInsertOpeningBalance(ss, sheet, sheetName);
  } else {
    // Ensure ID and Created At columns remain hidden
    if (sheet.getMaxColumns() >= 7) {
      sheet.hideColumns(7, 2);
    }
  }
  return sheet;
}

/**
 * Saves business profile information to Sheet1 of the spreadsheet.
 */
function saveBusinessProfileToSheet1(ss, data) {
  var sheet = ss.getSheetByName("Sheet1");
  if (!sheet) {
    sheet = ss.insertSheet("Sheet1", 0);
  } else {
    // Ensure Sheet1 is the first tab
    ss.setActiveSheet(sheet);
    ss.moveActiveSheet(1);
  }

  sheet.clear();

  // Header Banner
  var titleRange = sheet.getRange("A1:B1");
  titleRange.merge();
  titleRange.setValue("BUSINESS PROFILE & REGISTRATION DETAILS");
  titleRange.setFontWeight("bold");
  titleRange.setFontSize(14);
  titleRange.setBackground("#1E3A8A");
  titleRange.setFontColor("#FFFFFF");
  titleRange.setHorizontalAlignment("center");
  sheet.setRowHeight(1, 36);

  var rows = [
    ["1. Business Name", data.businessName || ""],
    ["2. ABN / ACN", data.abnAcn || ""],
    ["3. Business Address", data.businessAddress || ""],
    ["4. Phone / Mobile", data.phoneMobile || ""],
    ["5. Email Address", data.email || ""],
    ["Last Updated", Utilities.formatDate(new Date(), Session.getScriptTimeZone() || "GMT", "yyyy-MM-dd HH:mm:ss")]
  ];

  var dataRange = sheet.getRange(2, 1, rows.length, 2);
  dataRange.setValues(rows);
  dataRange.setFontSize(11);

  // Column styling
  var labelRange = sheet.getRange(2, 1, rows.length, 1);
  labelRange.setFontWeight("bold");
  labelRange.setBackground("#F3F4F6");

  sheet.setColumnWidth(1, 180);
  sheet.setColumnWidth(2, 400);

  return {
    status: "success",
    message: "Business Profile saved to Sheet1 successfully",
    businessName: data.businessName || ""
  };
}

/**
 * Updates an existing transaction's amount, notes, or type in the monthly sheets.
 */
function updateTransactionInSpreadsheet(ss, data) {
  var id = data.id ? String(data.id) : "";
  var date = data.date ? String(data.date) : "";
  var type = data.type ? String(data.type) : "";
  var newAmount = parseFloat(data.amount) || 0.0;
  var notes = data.notes || data.category;
  
  var targetSheet = null;
  var targetSheetName = "";

  if (date) {
    targetSheetName = getMonthSheetName(date);
    targetSheet = ss.getSheetByName(targetSheetName);
  }

  var foundRow = -1;
  var sheetToUpdate = null;

  // 1. Search in target sheet first if available
  if (targetSheet) {
    var lastRow = targetSheet.getLastRow();
    if (lastRow >= 2) {
      var values = targetSheet.getRange(2, 1, lastRow - 1, 8).getValues();
      for (var i = 0; i < values.length; i++) {
        var rowId = String(values[i][6] || ""); // Column G (ID)
        var rowDate = values[i][0] instanceof Date ? Utilities.formatDate(values[i][0], Session.getScriptTimeZone(), "yyyy-MM-dd") : String(values[i][0]);
        var rowType = String(values[i][1] || "");
        
        if (id && rowId && (rowId === id || id.indexOf(rowId) !== -1 || rowId.indexOf(id) !== -1)) {
          foundRow = i + 2;
          sheetToUpdate = targetSheet;
          break;
        } else if (!id && date && type && rowDate === date && rowType === type) {
          foundRow = i + 2;
          sheetToUpdate = targetSheet;
          break;
        }
      }
    }
  }

  // 2. If not found in targetSheet, search across all monthly sheets
  if (foundRow === -1) {
    var allSheets = ss.getSheets();
    for (var s = 0; s < allSheets.length; s++) {
      var curSheet = allSheets[s];
      if (curSheet.getName() === "Sheet1") continue;
      var curLastRow = curSheet.getLastRow();
      if (curLastRow < 2) continue;
      var curValues = curSheet.getRange(2, 1, curLastRow - 1, 8).getValues();
      for (var j = 0; j < curValues.length; j++) {
        var cRowId = String(curValues[j][6] || "");
        var cRowDate = curValues[j][0] instanceof Date ? Utilities.formatDate(curValues[j][0], Session.getScriptTimeZone(), "yyyy-MM-dd") : String(curValues[j][0]);
        var cRowType = String(curValues[j][1] || "");

        if (id && cRowId && (cRowId === id || id.indexOf(cRowId) !== -1 || cRowId.indexOf(id) !== -1)) {
          foundRow = j + 2;
          sheetToUpdate = curSheet;
          break;
        } else if (date && type && cRowDate === date && cRowType === type) {
          foundRow = j + 2;
          sheetToUpdate = curSheet;
          break;
        }
      }
      if (foundRow !== -1) break;
    }
  }

  if (sheetToUpdate && foundRow !== -1) {
    var isIncome = (type === "Daily Income" || type === "Opening Balance");
    if (!type) {
      var existingType = sheetToUpdate.getRange(foundRow, 2).getValue();
      isIncome = (existingType === "Daily Income" || existingType === "Opening Balance");
    }

    if (isIncome) {
      sheetToUpdate.getRange(foundRow, 4).setValue(newAmount);
      sheetToUpdate.getRange(foundRow, 5).setValue("");
    } else {
      sheetToUpdate.getRange(foundRow, 4).setValue("");
      sheetToUpdate.getRange(foundRow, 5).setValue(newAmount);
    }

    if (notes !== undefined && notes !== null && notes !== "") {
      sheetToUpdate.getRange(foundRow, 3).setValue(notes);
    }
    if (type) {
      sheetToUpdate.getRange(foundRow, 2).setValue(type);
    }

    // Recalculate balance formula
    sheetToUpdate.getRange(foundRow, 6).setFormula('=SUM(D$2:D' + foundRow + ')-SUM(E$2:E' + foundRow + ')');
    sheetToUpdate.getRange(foundRow, 4, 1, 3).setNumberFormat("$#,##0.00");

    return {
      status: "success",
      message: "Transaction updated in sheet " + sheetToUpdate.getName() + " row " + foundRow,
      sheet: sheetToUpdate.getName(),
      row: foundRow,
      newAmount: newAmount
    };
  } else {
    // Append as a new transaction if not found
    var fallbackDate = date || Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "yyyy-MM-dd");
    var sName = getMonthSheetName(fallbackDate);
    var sh = getOrCreateMonthlySheet(ss, sName);
    checkAndInsertOpeningBalance(ss, sh, sName);
    var nextRow = sh.getLastRow() + 1;
    var isInc = (type === "Daily Income" || type === "Opening Balance");
    var incVal = isInc ? newAmount : "";
    var expVal = !isInc ? newAmount : "";
    var balForm = '=SUM(D$2:D' + nextRow + ')-SUM(E$2:E' + nextRow + ')';

    sh.appendRow([
      fallbackDate,
      type || "Daily Income",
      notes || "",
      incVal,
      expVal,
      balForm,
      id || Utilities.getUuid(),
      new Date().toISOString()
    ]);
    sh.getRange(nextRow, 4, 1, 3).setNumberFormat("$#,##0.00");
    sh.hideColumns(7, 2);

    return {
      status: "success",
      message: "Transaction added with updated amount to " + sName,
      sheet: sName,
      newAmount: newAmount
    };
  }
}

/**
 * Handles incoming POST requests from the Android App.
 */
function doPost(e) {
  try {
    var contents = e.postData ? e.postData.contents : "";
    if (!contents) {
      return ContentService.createTextOutput(JSON.stringify({
        status: "error",
        message: "Empty payload"
      })).setMimeType(ContentService.MimeType.JSON);
    }

    var data = JSON.parse(contents);
    var ss = SpreadsheetApp.getActiveSpreadsheet();

    // Check if this is a request to save Business Profile information to Sheet1
    if (data.action === "save_profile" || (data.businessName !== undefined && data.type === undefined)) {
      var profileResult = saveBusinessProfileToSheet1(ss, data);
      return ContentService.createTextOutput(JSON.stringify(profileResult)).setMimeType(ContentService.MimeType.JSON);
    }

    // Check if this is a request to update an existing transaction amount
    if (data.action === "update_transaction" || data.action === "edit_transaction") {
      var updateResult = updateTransactionInSpreadsheet(ss, data);
      return ContentService.createTextOutput(JSON.stringify(updateResult)).setMimeType(ContentService.MimeType.JSON);
    }

    var id = data.id || Utilities.getUuid();
    var date = data.date || Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "yyyy-MM-dd");
    var type = data.type || "Daily Income";
    var notes = data.notes || data.category || "";
    var amount = parseFloat(data.amount) || 0.0;
    var createdAt = new Date().toISOString();

    // Determine target monthly sheet (e.g. Sep26, Oct26)
    var sheetName = getMonthSheetName(date);
    var sheet = getOrCreateMonthlySheet(ss, sheetName);

    // Ensure opening balance row exists if this is the first transaction of the month
    checkAndInsertOpeningBalance(ss, sheet, sheetName);

    var nextRow = sheet.getLastRow() + 1;

    // Income vs Expense & Bills separation
    var isIncome = (type === "Daily Income" || type === "Opening Balance");
    var incomeValue = isIncome ? amount : "";
    var expenseValue = !isIncome ? amount : "";

    // Automatic Balance formula:
    // Cumulative Sum of all Income (column D) minus all Expense & Bills (column E) up to current row
    var balanceFormula = '=SUM(D$2:D' + nextRow + ')-SUM(E$2:E' + nextRow + ')';

    sheet.appendRow([
      date,
      type,
      notes,
      incomeValue,
      expenseValue,
      balanceFormula,
      id,
      createdAt
    ]);

    // Format Income, Expense & Bills, and Balance as Currency ($#,##0.00)
    var formatRange = sheet.getRange(nextRow, 4, 1, 3);
    formatRange.setNumberFormat("$#,##0.00");

    // Re-verify hidden columns
    sheet.hideColumns(7, 2);

    return ContentService.createTextOutput(JSON.stringify({
      status: "success",
      message: "Transaction saved to " + sheetName,
      sheet: sheetName,
      id: id
    })).setMimeType(ContentService.MimeType.JSON);

  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: error.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * Handles GET requests:
 * 1. action=pdf: Returns complete spreadsheet PDF export URL.
 * 2. Default: Retrieves transactions across monthly sheets.
 */
function doGet(e) {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();

    // Check for PDF export request
    if (e && e.parameter && (e.parameter.action === "pdf" || e.parameter.format === "pdf")) {
      var ssUrl = ss.getUrl();
      var exportUrl = ssUrl.replace(/\/edit.*$/, '') + '/export?format=pdf&size=letter&portrait=true&fitw=true&gridlines=true';
      return ContentService.createTextOutput(JSON.stringify({
        status: "success",
        pdfUrl: exportUrl,
        spreadsheetUrl: ssUrl,
        title: ss.getName()
      })).setMimeType(ContentService.MimeType.JSON);
    }

    // Check for profile retrieval request
    if (e && e.parameter && e.parameter.action === "get_profile") {
      var profileSheet = ss.getSheetByName("Sheet1");
      var profileData = {};
      if (profileSheet && profileSheet.getLastRow() >= 6) {
        var vals = profileSheet.getRange(2, 1, 5, 2).getValues();
        profileData.businessName = vals[0][1] || "";
        profileData.abnAcn = vals[1][1] || "";
        profileData.businessAddress = vals[2][1] || "";
        profileData.phoneMobile = vals[3][1] || "";
        profileData.email = vals[4][1] || "";
      }
      return ContentService.createTextOutput(JSON.stringify({
        status: "success",
        profile: profileData
      })).setMimeType(ContentService.MimeType.JSON);
    }

    var sheets = ss.getSheets();
    var result = [];

    // Optional query param: ?month=Sep26
    var filterMonth = (e && e.parameter && e.parameter.month) ? e.parameter.month.trim() : null;

    for (var s = 0; s < sheets.length; s++) {
      var sheet = sheets[s];
      var name = sheet.getName();

      // Check if sheet name matches month format (e.g., Sep26, Oct26) or legacy 'Transactions'
      var isMonthSheet = /^[A-Z][a-z]{2}\d{2}${'$'}/.test(name);
      var isLegacySheet = (name === "Transactions");

      if (!isMonthSheet && !isLegacySheet) {
        continue;
      }

      if (filterMonth && name !== filterMonth) {
        continue;
      }

      var rows = sheet.getDataRange().getValues();
      if (rows.length <= 1) continue;

      var headerRow = rows[0];
      var isNewFormat = (headerRow.length >= 6 && headerRow[3] === "Income");

      for (var i = 1; i < rows.length; i++) {
        var row = rows[i];
        if (!row || row.length === 0) continue;

        if (isNewFormat) {
          // New format: [Date, Type, Notes, Income, Expense & Bills, Balance, ID, Created At]
          var rawDate = row[0];
          if (!rawDate) continue;

          var formattedDate = rawDate instanceof Date
            ? Utilities.formatDate(rawDate, Session.getScriptTimeZone(), "yyyy-MM-dd")
            : String(rawDate);

          var type = String(row[1] || "Daily Income");
          var notes = String(row[2] || "");
          var income = parseFloat(row[3]) || 0.0;
          var expense = parseFloat(row[4]) || 0.0;
          var balance = parseFloat(row[5]) || 0.0;
          var id = String(row[6] || "");
          var createdAt = String(row[7] || "");
          var amount = income > 0 ? income : expense;

          result.push({
            id: id,
            date: formattedDate,
            type: type,
            notes: notes,
            income: income,
            expense: expense,
            amount: amount,
            balance: balance,
            monthSheet: name,
            createdAt: createdAt
          });
        } else {
          // Legacy format: [ID, Date, Type, Notes, Amount, Created At]
          var legacyDate = row[1];
          if (!legacyDate) continue;

          var fDate = legacyDate instanceof Date
            ? Utilities.formatDate(legacyDate, Session.getScriptTimeZone(), "yyyy-MM-dd")
            : String(legacyDate);

          var legType = String(row[2] || "Daily Income");
          var legNotes = String(row[3] || "");
          var legAmount = parseFloat(row[4]) || 0.0;

          result.push({
            id: String(row[0] || ""),
            date: fDate,
            type: legType,
            notes: legNotes,
            income: legType === "Daily Income" ? legAmount : 0.0,
            expense: legType !== "Daily Income" ? legAmount : 0.0,
            amount: legAmount,
            monthSheet: name,
            createdAt: String(row[5] || "")
          });
        }
      }
    }

    return ContentService.createTextOutput(JSON.stringify({
      status: "success",
      count: result.length,
      data: result
    })).setMimeType(ContentService.MimeType.JSON);

  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: error.toString(),
      data: []
    })).setMimeType(ContentService.MimeType.JSON);
  }
}
""".trimIndent()
}
