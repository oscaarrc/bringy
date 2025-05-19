package com.project.taima.utils

import android.annotation.SuppressLint
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.project.taima.model.Customer
import org.apache.poi.openxml4j.exceptions.InvalidFormatException
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@SuppressLint("DefaultLocale")
fun exportFirestoreToExcel(callback: (Boolean, String?) -> Unit) {
    val db = FirebaseFirestore.getInstance()

    val daysOfWeek = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
    val workbook = XSSFWorkbook()
    val sheets = daysOfWeek.associateWith { workbook.createSheet(it) }

    val dataFormat = workbook.createDataFormat()
    val currencyStyle = workbook.createCellStyle()
    currencyStyle.dataFormat = dataFormat.getFormat("#,##0.00€")

    for (sheet in sheets.values) {
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("CALLE")
        headerRow.createCell(1).setCellValue("ALIAS")
        headerRow.createCell(2).setCellValue("SALDO")

        sheet.setColumnWidth(0, 7000)
        sheet.setColumnWidth(1, 7000)
        sheet.setColumnWidth(2, 4000)
    }

    db.collection("Customer").get().addOnSuccessListener { documents ->
        for (document in documents) {
            val alias = document.getString("alias") ?: "Sin Alias"
            val route = document.getString("route") ?: "Sin Ruta"
            val balance = document.getDouble("balance") ?: 0.0
            val routeDays = document.get("routeDay") as? List<String> ?: emptyList()

            for (day in routeDays) {
                val sheet = sheets[day] ?: continue
                val row = sheet.createRow(sheet.lastRowNum + 1)
                row.createCell(0).setCellValue(route)
                row.createCell(1).setCellValue(alias)
                val saldoCell = row.createCell(2)
                saldoCell.setCellValue(balance)
                saldoCell.cellStyle = currencyStyle
            }
        }

        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            var filePath = File(downloadsDir, "Datos_Taisma.xlsx")
            var count = 1

            while (filePath.exists()) {
                filePath = File(downloadsDir, "Datos_Taisma($count).xlsx")
                count++
            }

            FileOutputStream(filePath).use { outputStream ->
                workbook.write(outputStream)
            }
            workbook.close()

            Log.d("ExportExcel", "Archivo guardado en: ${filePath.absolutePath}")
            callback(true, filePath.absolutePath)
        } catch (e: Exception) {
            Log.e("ExportExcel", "Error al guardar el archivo Excel: ${e.message}")
            callback(false, null)
        }
    }.addOnFailureListener { exception ->
        Log.e("ExportExcel", "Error al obtener datos de Firestore: ${exception.message}")
        callback(false, null)
    }
}



fun importExcelToFirestore(filePath: String, progressBar: ProgressBar, loadingText: TextView, callback: (Boolean, String?) -> Unit) {
    progressBar.visibility = View.VISIBLE
    progressBar.isIndeterminate = true
    loadingText.visibility = View.VISIBLE
    loadingText.text = "Cargando..."

    val db = FirebaseFirestore.getInstance()

    try {
        // Deletes all documents in the “Transactions” collection
        db.collection("Transactions").get()
            .addOnSuccessListener { transactionDocuments ->
                val transactionBatch = db.batch()
                for (document in transactionDocuments) {
                    transactionBatch.delete(document.reference)
                }
                transactionBatch.commit().addOnSuccessListener {
                    Log.d("ImportExcel", "Colección 'Transacciones' vaciada exitosamente.")

                    // Delete all the documents in the “Customer” collection
                    db.collection("Customer").get()
                        .addOnSuccessListener { customerDocuments ->
                            val customerBatch = db.batch()
                            for (document in customerDocuments) {
                                customerBatch.delete(document.reference)
                            }
                            customerBatch.commit().addOnSuccessListener {
                                Log.d("ImportExcel", "Colección 'Customer' vaciada exitosamente.")

                                addCustomersFromExcel(filePath, db) { success, message ->
                                    progressBar.visibility = View.GONE
                                    loadingText.visibility = View.GONE
                                    callback(success, message)
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            progressBar.visibility = View.GONE
                            loadingText.visibility = View.GONE
                            Log.e("ImportExcel", "Error al obtener documentos de 'Customer': ${e.message}")
                            callback(false, "Error al eliminar los documentos de 'Customer'.")
                        }
                }.addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    loadingText.visibility = View.GONE
                    Log.e("ImportExcel", "Error al vaciar la colección 'Transacciones': ${e.message}")
                    callback(false, "Error al vaciar la colección 'Transacciones'.")
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                loadingText.visibility = View.GONE
                Log.e("ImportExcel", "Error al obtener documentos de 'Transacciones': ${e.message}")
                callback(false, "Error al obtener los documentos de 'Transacciones'.")
            }
    } catch (e: InvalidFormatException) {
        progressBar.visibility = View.GONE
        loadingText.visibility = View.GONE
        Log.e("ImportExcel", "Error de formato en el archivo Excel: ${e.message}")
        callback(false, "Error de formato en el archivo Excel.")
    } catch (e: Exception) {
        progressBar.visibility = View.GONE
        loadingText.visibility = View.GONE
        Log.e("ImportExcel", "Error al importar el archivo Excel: ${e.message}")
        callback(false, "Error al importar el archivo Excel.")
    }
}



fun addCustomersFromExcel(filePath: String, db: FirebaseFirestore, callback: (Boolean, String?) -> Unit) {
    try {
        val file = File(filePath)
        val fis = FileInputStream(file)
        val workbook = WorkbookFactory.create(fis)

        val customersMap = mutableMapOf<String, Customer>()
        val totalSheets = workbook.numberOfSheets
        var totalRows = 0

        for (sheetIndex in 0 until totalSheets) {
            val sheet = workbook.getSheetAt(sheetIndex)
            totalRows += sheet.physicalNumberOfRows - 1
        }
        val tasks = mutableListOf<Task<DocumentReference>>()

        for (sheetIndex in 0 until totalSheets) {
            val sheet = workbook.getSheetAt(sheetIndex)
            val routeDay = sheet.sheetName

            for (rowIndex in 1 until sheet.physicalNumberOfRows) {
                val row = sheet.getRow(rowIndex)

                val route = row.getCell(0)?.stringCellValue ?: "Sin Ruta"
                val alias = row.getCell(1)?.stringCellValue ?: "Sin Alias"
                val balance = row.getCell(2)?.numericCellValue ?: 0.0

                if (customersMap.containsKey(route)) {
                    customersMap[route]?.routeDay = (customersMap[route]?.routeDay ?: listOf()) + routeDay
                } else {
                    customersMap[route] = Customer(
                        route = route,
                        alias = alias,
                        balance = balance,
                        routeDay = listOf(routeDay),
                        position = emptyMap<String, Int>().toMutableMap() // Empty map for "position"
                    )
                }
            }
        }

        // Saves customers in Firestore
        customersMap.values.forEach { customer ->
            customer.routeDay = customer.routeDay.distinct()

            val task = db.collection("Customer").add(customer)
                .addOnSuccessListener {
                    Log.d("ImportExcel", "Cliente agregado con éxito: ${customer.route}")
                }
                .addOnFailureListener { e ->
                    Log.e("ImportExcel", "Error al agregar cliente: ${e.message}")
                }

            tasks.add(task)
        }

        // Waits for all tasks to be completed.
        Tasks.whenAllComplete(tasks).addOnCompleteListener {
            fis.close()
            if (it.isSuccessful) {
                callback(true, "Importación completada con éxito.")
            } else {
                callback(false, "Error al importar los clientes.")
            }
        }

    } catch (e: InvalidFormatException) {
        Log.e("ImportExcel", "Error de formato en el archivo Excel: ${e.message}")
        callback(false, "Error de formato en el archivo Excel.")
    } catch (e: Exception) {
        Log.e("ImportExcel", "Error al importar el archivo Excel: ${e.message}")
        callback(false, "Error al importar el archivo Excel.")
    }
}