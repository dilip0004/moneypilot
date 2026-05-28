package com.yourname.moneypilot.domain.usecase.transaction

import android.content.Context
import com.yourname.moneypilot.data.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject

/**
 * Enterprise Architecture v5.0 Compliance: Section 13.1 (Excel Format Support).
 * Generates an XML-based Excel file (SpreadsheetML) containing the full ledger.
 */
class ExportToExcelUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(): File? {
        val transactions = transactionRepository.getAllTransactionsWithDetails().first()
        if (transactions.isEmpty()) return null

        val fileName = "MoneyPilot_Export_${System.currentTimeMillis()}.xml"
        val file = File(context.cacheDir, fileName)

        file.bufferedWriter().use { out ->
            // Excel XML Header
            out.write("<?xml version=\"1.0\"?>\n")
            out.write("<?mso-application progid=\"Excel.Sheet\"?>\n")
            out.write("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
            out.write(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n")
            out.write(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n")
            out.write(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n")
            
            out.write(" <Styles>\n")
            out.write("  <Style ss:ID=\"Header\">\n")
            out.write("   <Font ss:Bold=\"1\"/>\n")
            out.write("   <Interior ss:Color=\"#E0E0E0\" ss:Pattern=\"Solid\"/>\n")
            out.write("  </Style>\n")
            out.write(" </Styles>\n")

            out.write(" <Worksheet ss:Name=\"Ledger\">\n")
            out.write("  <Table>\n")
            
            // Header Row
            out.write("   <Row ss:StyleID=\"Header\">\n")
            listOf("Date", "Type", "Source", "Wallet", "Category", "Subcategory", "Amount", "Note", "Refund").forEach {
                out.write("    <Cell><Data ss:Type=\"String\">$it</Data></Cell>\n")
            }
            out.write("   </Row>\n")

            // Data Rows
            transactions.forEach { detail ->
                val tx = detail.transaction
                out.write("   <Row>\n")
                out.write("    <Cell><Data ss:Type=\"String\">${tx.dateTime}</Data></Cell>\n")
                out.write("    <Cell><Data ss:Type=\"String\">${tx.type.name}</Data></Cell>\n")
                out.write("    <Cell><Data ss:Type=\"String\">${tx.transactionSourceType}</Data></Cell>\n")
                out.write("    <Cell><Data ss:Type=\"String\">${detail.walletFrom?.name ?: detail.walletTo?.name ?: "N/A"}</Data></Cell>\n")
                out.write("    <Cell><Data ss:Type=\"String\">${detail.category?.name ?: "Uncategorized"}</Data></Cell>\n")
                out.write("    <Cell><Data ss:Type=\"String\">${detail.subcategory?.name ?: ""}</Data></Cell>\n")
                out.write("    <Cell><Data ss:Type=\"Number\">${tx.amount}</Data></Cell>\n")
                out.write("    <Cell><Data ss:Type=\"String\">${(tx.note ?: "").replace("<", "&lt;").replace(">", "&gt;")}</Data></Cell>\n")
                out.write("    <Cell><Data ss:Type=\"String\">${if (tx.isRefund) "Yes" else "No"}</Data></Cell>\n")
                out.write("   </Row>\n")
            }

            out.write("  </Table>\n")
            out.write(" </Worksheet>\n")
            out.write("</Workbook>\n")
        }
        return file
    }
}
