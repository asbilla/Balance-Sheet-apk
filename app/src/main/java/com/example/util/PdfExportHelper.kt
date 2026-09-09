package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.ui.balancesheet.DateBalanceSummary
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportHelper {

    /**
     * Downloads or exports the whole balance sheet into a clean PDF document.
     * Can open the official Google Sheets PDF export or generate a local high-quality PDF.
     */
    fun exportLocalPdf(
        context: Context,
        summaries: List<DateBalanceSummary>,
        overallIncome: Double,
        overallExpenses: Double,
        overallBills: Double,
        overallNet: Double,
        profile: com.example.data.pref.BusinessProfile = com.example.data.pref.BusinessProfile()
    ): File? {
        try {
            val pdfDoc = PdfDocument()
            val pageWidth = 595 // Standard A4 points width
            val pageHeight = 842 // Standard A4 points height

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDoc.startPage(pageInfo)
            var canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(30, 58, 138) // Deep Blue
                textSize = 18f
                isFakeBoldText = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 9.5f
            }

            val metaPaint = Paint().apply {
                color = Color.rgb(100, 116, 139)
                textSize = 8.5f
            }

            val headerBgPaint = Paint().apply {
                color = Color.rgb(238, 242, 255)
            }

            val tableHeaderPaint = Paint().apply {
                color = Color.rgb(30, 58, 138)
                textSize = 10f
                isFakeBoldText = true
            }

            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 9f
            }

            val boldTextPaint = Paint().apply {
                color = Color.BLACK
                textSize = 9f
                isFakeBoldText = true
            }

            val greenPaint = Paint().apply {
                color = Color.rgb(22, 101, 52)
                textSize = 9f
                isFakeBoldText = true
            }

            val redPaint = Paint().apply {
                color = Color.rgb(153, 27, 27)
                textSize = 9f
                isFakeBoldText = true
            }

            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 0.5f
            }

            var y = 36f

            // Document & Business Header (All 5 Business Profile details)
            val displayName = if (profile.businessName.isNotBlank()) profile.businessName else "DAILY BUSINESS REPORT"
            canvas.drawText(displayName, 30f, y, titlePaint)
            y += 15f

            // Business info line 1: ABN/ACN & Email
            val infoLine1 = buildString {
                if (profile.abnAcn.isNotBlank()) append("ABN/ACN: ${profile.abnAcn}    ")
                if (profile.email.isNotBlank()) append("Email: ${profile.email}")
            }
            if (infoLine1.isNotBlank()) {
                canvas.drawText(infoLine1, 30f, y, subtitlePaint)
                y += 13f
            }

            // Business info line 2: Address & Phone
            val infoLine2 = buildString {
                if (profile.businessAddress.isNotBlank()) append("Address: ${profile.businessAddress}    ")
                if (profile.phoneMobile.isNotBlank()) append("Phone/Mobile: ${profile.phoneMobile}")
            }
            if (infoLine2.isNotBlank()) {
                canvas.drawText(infoLine2, 30f, y, subtitlePaint)
                y += 13f
            }

            val timeStamp = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("BALANCE SHEET FINANCIAL STATEMENT  •  Generated on: $timeStamp", 30f, y, metaPaint)
            y += 12f
            canvas.drawLine(30f, y, (pageWidth - 30).toFloat(), y, linePaint)
            y += 12f

            // Summary Totals Box
            canvas.drawRect(30f, y, (pageWidth - 30).toFloat(), y + 45f, headerBgPaint)
            y += 18f
            canvas.drawText(
                "Total Income: $${String.format(Locale.US, "%,.2f", overallIncome)}    " +
                        "Total Expenses: $${String.format(Locale.US, "%,.2f", overallExpenses)}    " +
                        "Total Bills: $${String.format(Locale.US, "%,.2f", overallBills)}",
                40f,
                y,
                boldTextPaint
            )
            y += 16f
            val netText = "Overall Net Balance: $${String.format(Locale.US, "%,.2f", overallNet)}"
            val netPaint = if (overallNet >= 0) greenPaint else redPaint
            canvas.drawText(netText, 40f, y, netPaint)
            y += 30f

            // Table Columns: Date (60), Type (80), Notes (140), Income (70), Exp/Bills (70), Running Balance (80)
            fun drawTableHeader(currY: Float) {
                canvas.drawRect(30f, currY, (pageWidth - 30).toFloat(), currY + 20f, headerBgPaint)
                canvas.drawText("Date", 35f, currY + 14f, tableHeaderPaint)
                canvas.drawText("Type", 100f, currY + 14f, tableHeaderPaint)
                canvas.drawText("Notes / Category", 175f, currY + 14f, tableHeaderPaint)
                canvas.drawText("Income", 310f, currY + 14f, tableHeaderPaint)
                canvas.drawText("Expense/Bills", 385f, currY + 14f, tableHeaderPaint)
                canvas.drawText("Balance", 475f, currY + 14f, tableHeaderPaint)
            }

            drawTableHeader(y)
            y += 26f

            // Loop through dates
            // Sort summaries chronologically for ledger display
            val sortedAsc = summaries.sortedBy { it.date }

            for (dateSum in sortedAsc) {
                // Check if page overflow
                if (y > pageHeight - 60) {
                    pdfDoc.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDoc.startPage(pageInfo)
                    canvas = page.canvas
                    y = 40f
                    drawTableHeader(y)
                    y += 26f
                }

                // Date separator bar
                canvas.drawRect(30f, y - 4f, (pageWidth - 30).toFloat(), y + 16f, Paint().apply { color = Color.rgb(243, 244, 246) })
                val displayDateSum = try {
                    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    parser.parse(dateSum.date)?.let { formatter.format(it) } ?: dateSum.date
                } catch (_: Exception) {
                    dateSum.date
                }
                val dateTitle = "$displayDateSum   (Day Net: $${String.format(Locale.US, "%,.2f", dateSum.netBalance)} | Cumulative: $${String.format(Locale.US, "%,.2f", dateSum.cumulativeBalance)})"
                canvas.drawText(dateTitle, 35f, y + 10f, boldTextPaint)
                y += 22f

                for (tx in dateSum.transactions) {
                    if (y > pageHeight - 50) {
                        pdfDoc.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDoc.startPage(pageInfo)
                        canvas = page.canvas
                        y = 40f
                        drawTableHeader(y)
                        y += 26f
                    }

                    val displayTxDate = try {
                        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        parser.parse(tx.date)?.let { formatter.format(it) } ?: tx.date
                    } catch (_: Exception) {
                        tx.date
                    }
                    canvas.drawText(displayTxDate, 35f, y, textPaint)
                    canvas.drawText(tx.type, 100f, y, textPaint)

                    val shortNotes = if (tx.category.length > 22) tx.category.take(20) + ".." else tx.category
                    canvas.drawText(shortNotes, 175f, y, textPaint)

                    val isInc = tx.type == "Daily Income"
                    val incStr = if (isInc) "$${String.format(Locale.US, "%,.2f", tx.amount)}" else "-"
                    val expStr = if (!isInc) "$${String.format(Locale.US, "%,.2f", tx.amount)}" else "-"

                    canvas.drawText(incStr, 310f, y, if (isInc) greenPaint else textPaint)
                    canvas.drawText(expStr, 385f, y, if (!isInc) redPaint else textPaint)
                    canvas.drawText("$${String.format(Locale.US, "%,.2f", dateSum.cumulativeBalance)}", 475f, y, boldTextPaint)

                    canvas.drawLine(30f, y + 5f, (pageWidth - 30).toFloat(), y + 5f, linePaint)
                    y += 18f
                }
                y += 8f
            }

            pdfDoc.finishPage(page)

            // Save to Downloads folder or app external files
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val fileName = "Business_Balance_Sheet_${SimpleDateFormat("ddMMyyyy_hhmmss_a", Locale.getDefault()).format(Date())}.pdf"
            val file = File(downloadsDir, fileName)

            val outputStream = FileOutputStream(file)
            pdfDoc.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDoc.close()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Opens the saved PDF file using an Intent chooser
     */
    fun openPdfFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open Balance Sheet PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Saved to Downloads: ${file.name}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Launches a browser / system download for Google Sheets PDF URL
     */
    fun openSheetsPdfDownload(context: Context, pdfUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open download URL: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
