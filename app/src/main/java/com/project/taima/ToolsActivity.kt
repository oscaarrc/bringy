package com.project.taima

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.project.taima.utils.importExcelToFirestore
import com.project.taima.utils.exportFirestoreToExcel
import java.io.File
import java.io.IOException

class ToolsActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var loadingText: TextView
    private lateinit var loadingLayout: LinearLayout

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            importExcelAndUpdateFirestore(it)
        } ?: showToast("No se seleccionó ningún archivo.")
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tools)

        // Inicialización de los elementos del layout
        progressBar = findViewById(R.id.progressBar)
        loadingText = findViewById(R.id.loadingText)
        loadingLayout = findViewById(R.id.loadingLayout)

        setupUI()
        setupBackButton()
    }

    private fun setupUI() {
        findViewById<LinearLayout>(R.id.exportSection).setOnClickListener { showExportDialog() }
        findViewById<LinearLayout>(R.id.importSection).setOnClickListener { openFilePicker() }
    }

    private fun setupBackButton() {
        findViewById<ImageView>(R.id.backIconToools).setOnClickListener {
            val intent = Intent(this@ToolsActivity, HomeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showExportDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exportar a Excel")
            .setMessage("¿Deseas exportar los datos a un archivo Excel?")
            .setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
                performExport()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                showToast("Operación cancelada")
            }
            .show()
    }

    private fun performExport() {
        showLoading(true)
        exportFirestoreToExcel { success, filePath ->
            showLoading(false)
            if (success && filePath != null) {
                showToast("Archivo guardado en la carpeta Descargas.")
            } else {
                showToast("Error al exportar los datos.")
            }
        }
    }

    private fun openFilePicker() {
        getContent.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    }

    @SuppressLint("Recycle")
    private fun importExcelAndUpdateFirestore(uri: Uri) {
        showLoading(true)

        try {
            val inputStream = contentResolver.openInputStream(uri)
            inputStream?.let { _ ->
                val tempFile = createTempFileFromUri(uri)
                if (tempFile != null) {
                    importExcelToFirestore(tempFile.absolutePath, progressBar, loadingText) { success, message ->

                        showLoading(false)
                        if (success) {
                            showToast("Datos importados con éxito.")
                        } else {
                            showToast("Error al importar los datos: $message")
                        }
                    }
                } else {
                    showLoading(false)
                    showToast("No se pudo crear el archivo temporal.")
                }
            } ?: run {
                showLoading(false)
                showToast("No se pudo abrir el archivo.")
            }
        } catch (e: Exception) {
            showLoading(false)
            e.printStackTrace()
            showToast("Error al importar el archivo: ${e.message}")
        }
    }

    private fun createTempFileFromUri(uri: Uri): File? {
        val inputStream = contentResolver.openInputStream(uri)
        inputStream?.let { stream ->
            val tempFile = File(cacheDir, getFileName(uri))
            try {
                tempFile.outputStream().use { outputStream ->
                    stream.copyTo(outputStream)
                }
                return tempFile
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun getFileName(uri: Uri): String {
        var fileName = ""
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.let {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                fileName = it.getString(columnIndex)
            }
            it.close()
        }
        return fileName
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            loadingLayout.visibility = View.VISIBLE
        } else {
            loadingLayout.visibility = View.GONE
        }
    }

}
