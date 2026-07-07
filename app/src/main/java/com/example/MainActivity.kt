package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.example.ui.PhonebookApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ContactViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: ContactViewModel

    // Document creation contract for CSV Export
    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            try {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    val csvString = viewModel.getCsvData()
                    // Writing UTF-8 BOM so Microsoft Excel can open Persian text with correct encoding
                    outputStream.write(csvString.toByteArray(Charsets.UTF_8))
                    Toast.makeText(this, "خروجی با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "خطا در ذخیره فایل: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Document selection contract for CSV Import
    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                contentResolver.openInputStream(it)?.use { inputStream ->
                    val csvString = inputStream.bufferedReader(Charsets.UTF_8).use { reader -> reader.readText() }
                    viewModel.importCsvData(csvString) { addedCount ->
                        Toast.makeText(this, "$addedCount مخاطب با موفقیت اضافه شدند", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "خطا در خواندن فایل: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[ContactViewModel::class.java]
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PhonebookApp(
                    viewModel = viewModel,
                    onExportCsv = {
                        val fileName = "Phonebook_" + viewModel.getCurrentPersianDate().replace("/", "-") + ".csv"
                        createDocumentLauncher.launch(fileName)
                    },
                    onImportCsv = {
                        openDocumentLauncher.launch("*/*")
                    }
                )
            }
        }
    }
}
