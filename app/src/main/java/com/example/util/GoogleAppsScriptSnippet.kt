package com.example.util

object GoogleAppsScriptSnippet {
    val CODE: String = """
/**
 * Daily Business Reporting Tool - Google Apps Script Backend
 * 
 * SETUP INSTRUCTIONS:
 * 1. Open Google Sheets (create a new blank sheet if needed).
 * 2. Click Extensions > Apps Script in the top menu.
 * 3. Delete any default code in Code.gs and paste this entire file.
 * 4. Click 'Save' (disk icon).
 * 5. Click 'Deploy' > 'New deployment'.
 * 6. Under 'Select type' (gear icon), choose 'Web app'.
 * 7. Set:
 *    - Description: Daily Business Reporting API
 *    - Execute as: Me
 *    - Who has access: Anyone
 * 8. Click 'Deploy', grant necessary permissions.
 * 9. Copy the generated 'Web app URL' and paste it into the Android App!
 */

function setupSheet() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("Transactions");
  if (!sheet) {
    sheet = ss.insertSheet("Transactions");
    sheet.appendRow(["ID", "Date", "Type", "Notes", "Amount", "Created At"]);
    sheet.getRange("A1:F1").setFontWeight("bold").setBackground("#F3F4F6");
    sheet.setFrozenRows(1);
  }
  return sheet;
}

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
    var sheet = setupSheet();

    var id = data.id || Utilities.getUuid();
    var date = data.date || Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "yyyy-MM-dd");
    var type = data.type || "Daily Income";
    var notes = data.notes || data.category || "";
    var amount = parseFloat(data.amount) || 0.0;
    var createdAt = new Date().toISOString();

    sheet.appendRow([id, date, type, notes, amount, createdAt]);

    return ContentService.createTextOutput(JSON.stringify({
      status: "success",
      message: "Transaction added successfully",
      id: id
    })).setMimeType(ContentService.MimeType.JSON);

  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: error.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

function doGet(e) {
  try {
    var sheet = setupSheet();
    var rows = sheet.getDataRange().getValues();
    var result = [];

    // Skip header row
    for (var i = 1; i < rows.length; i++) {
      var row = rows[i];
      if (row.length >= 5 && (row[1] || row[4])) {
        var rawDate = row[1];
        var formattedDate = rawDate instanceof Date 
          ? Utilities.formatDate(rawDate, Session.getScriptTimeZone(), "yyyy-MM-dd")
          : String(rawDate);

        result.push({
          id: String(row[0] || ""),
          date: formattedDate,
          type: String(row[2] || "Daily Income"),
          notes: String(row[3] || ""),
          amount: parseFloat(row[4]) || 0.0,
          createdAt: String(row[5] || "")
        });
      }
    }

    return ContentService.createTextOutput(JSON.stringify({
      status: "success",
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
