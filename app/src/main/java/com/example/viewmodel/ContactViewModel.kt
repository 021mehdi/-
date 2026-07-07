package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Contact
import com.example.data.ContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class ContactViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ContactRepository

    init {
        val contactDao = AppDatabase.getDatabase(application).contactDao()
        repository = ContactRepository(contactDao)
    }

    val contacts: StateFlow<List<Contact>> = repository.allContacts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    // Filter states
    private val _sDate = MutableStateFlow("")
    val sDate = _sDate.asStateFlow()

    private val _sDepartment = MutableStateFlow("")
    val sDepartment = _sDepartment.asStateFlow()

    private val _sPosition = MutableStateFlow("")
    val sPosition = _sPosition.asStateFlow()

    private val _sAdvancedSearch = MutableStateFlow("")
    val sAdvancedSearch = _sAdvancedSearch.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Contact>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    fun updateSDate(value: String) { _sDate.value = value }
    fun updateSDepartment(value: String) { _sDepartment.value = value }
    fun updateSPosition(value: String) { _sPosition.value = value }
    fun updateSAdvancedSearch(value: String) { _sAdvancedSearch.value = value }

    fun setCurrentIndex(index: Int) {
        val total = contacts.value.size
        if (total > 0) {
            _currentIndex.value = (index + total) % total
        } else {
            _currentIndex.value = 0
        }
    }

    fun nextContact() {
        if (contacts.value.isNotEmpty()) {
            setCurrentIndex(currentIndex.value + 1)
        }
    }

    fun prevContact() {
        if (contacts.value.isNotEmpty()) {
            setCurrentIndex(currentIndex.value - 1)
        }
    }

    fun insertContact(contact: Contact, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insert(contact)
            // Move index to newly added contact
            val size = contacts.value.size
            if (contact.id == 0L) {
                _currentIndex.value = size
            }
            onComplete()
        }
    }

    fun updateContact(contact: Contact, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.update(contact)
            onComplete()
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            repository.delete(contact)
            // Adjust index if needed
            val size = contacts.value.size - 1
            if (_currentIndex.value >= size) {
                _currentIndex.value = maxOf(0, size - 1)
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.deleteAll()
            _currentIndex.value = 0
        }
    }

    // Positions for search drop-down mapping from contacts list flow
    val uniquePositions: StateFlow<List<String>> = contacts.map { list ->
        list.map { it.position.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun performSearch() {
        val rawDate = _sDate.value.trim()
        val deptQuery = normalizeText(_sDepartment.value)
        val posQuery = _sPosition.value.trim()
        val advQuery = normalizeText(_sAdvancedSearch.value)

        val parsedSearchDate = normalizeSearchDate(rawDate)

        val results = contacts.value.filter { c ->
            // Date filter
            val dateMatch = if (parsedSearchDate.isNotEmpty()) {
                c.persianDate == parsedSearchDate
            } else true

            // Department filter
            val deptMatch = if (deptQuery.isNotEmpty()) {
                normalizeText(c.department).contains(deptQuery)
            } else true

            // Position filter
            val posMatch = if (posQuery.isNotEmpty()) {
                c.position.trim() == posQuery
            } else true

            // Advanced filter
            val advMatch = if (advQuery.isNotEmpty()) {
                val combinedText = normalizeText("${c.fullName} ${c.position} ${c.department} ${c.description} ${c.mobile} ${c.officePhone}")
                combinedText.contains(advQuery)
            } else true

            dateMatch && deptMatch && posMatch && advMatch
        }
        _searchResults.value = results
    }

    fun clearSearchFilters() {
        _sDate.value = ""
        _sDepartment.value = ""
        _sPosition.value = ""
        _sAdvancedSearch.value = ""
        _searchResults.value = emptyList()
    }

    // Jump to contact by ID
    fun jumpToContact(id: Long) {
        val index = contacts.value.indexOfFirst { it.id == id }
        if (index != -1) {
            _currentIndex.value = index
        }
    }

    // Helper functions for CSV Import/Export
    fun getCsvData(): String {
        return exportContactsToCsvString(contacts.value)
    }

    fun importCsvData(csvContent: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val importedList = parseContactsFromCsv(csvContent)
            var addedCount = 0
            importedList.forEach { contact ->
                repository.insert(contact)
                addedCount++
            }
            onComplete(addedCount)
        }
    }

    // --- Core helper implementations ---

    fun normalizePersianDigits(input: String): String {
        val persianDigits = listOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val arabicDigits = listOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        var result = input
        for (i in 0..9) {
            // Replace Char to Char to avoid overload matching issues
            result = result.replace(persianDigits[i], '0' + i)
            result = result.replace(arabicDigits[i], '0' + i)
        }
        return result
    }

    fun normalizeText(input: String): String {
        var v = normalizePersianDigits(input)
        v = v.replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('آ', 'ا')
            .trim()
            .lowercase()
        return v
    }

    fun normalizeSearchDate(dateStr: String): String {
        if (dateStr.isBlank()) return ""
        var cleaned = dateStr.trim()
        cleaned = cleaned.replace("-", "/")
            .replace("_", "/")
            .replace(".", "/")
        cleaned = normalizePersianDigits(cleaned)
        val parts = cleaned.split('/')
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            val d = parts[2].toIntOrNull()
            if (y != null && m != null && d != null) {
                if (y in 1300..1500 && m in 1..12 && d in 1..31) {
                    return String.format(Locale.US, "%d/%02d/%02d", y, m, d)
                }
            }
        }
        return ""
    }

    fun isValidPersianDate(dateStr: String): Boolean {
        if (dateStr.isBlank()) return true
        val parts = dateStr.split('/')
        if (parts.size != 3) return false
        val y = parts[0].toIntOrNull() ?: return false
        val m = parts[1].toIntOrNull() ?: return false
        val d = parts[2].toIntOrNull() ?: return false

        if (y < 1300 || y > 1500) return false
        if (m < 1 || m > 12) return false

        val maxDay = when {
            m <= 6 -> 31
            m <= 11 -> 30
            else -> {
                val isLeap = (y % 4 == 0) && (y % 100 != 0 || y % 400 == 0)
                if (isLeap) 30 else 29
            }
        }
        return d in 1..maxDay
    }

    fun normalizePersianDate(dateStr: String): String {
        if (dateStr.isBlank()) return ""
        val parts = dateStr.split('/')
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            val d = parts[2].toIntOrNull()
            if (y != null && m != null && d != null) {
                return String.format(Locale.US, "%d/%02d/%02d", y, m, d)
            }
        }
        return dateStr
    }

    // Robust mathematical converter: Gregorian to Solar Hijri date
    fun getCurrentPersianDate(): String {
        val now = java.util.Calendar.getInstance()
        val gy = now.get(java.util.Calendar.YEAR)
        val gm = now.get(java.util.Calendar.MONTH) + 1
        val gd = now.get(java.util.Calendar.DAY_OF_MONTH)

        val g_d_m = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) (gy + 1) else gy
        var days = 355666 + (365 * gy) + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400 + gd + g_d_m[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        var jm = days / 31
        days %= 31
        var jd = days + 1
        if (jm > 6) {
            jm -= 7
            jd = days + 1
            jm += 7
        }
        if (jm > 6) {
            jm -= 7
            jd = days + 1
        }
        if (jm > 5) {
            jd = days + 1
            jm -= 6
        }
        val finalJm = jm + 1
        return String.format(Locale.US, "%04d/%02d/%02d", jy, finalJm, jd)
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return if (clean.contains(",") || clean.contains("\n") || clean.contains("\r") || clean.contains("\"")) {
            "\"$clean\""
        } else {
            clean
        }
    }

    fun exportContactsToCsvString(contacts: List<Contact>): String {
        val sb = StringBuilder()
        sb.append("\uFEFF") // UTF-8 BOM
        sb.append("تاریخ ثبت (شمسی),نام و نام خانوادگی,سمت,قسمت,توضیحات,شماره تماس اول,شماره تماس دوم\n")
        contacts.forEach { c ->
            sb.append(escapeCsv(c.persianDate)).append(",")
            sb.append(escapeCsv(c.fullName)).append(",")
            sb.append(escapeCsv(c.position)).append(",")
            sb.append(escapeCsv(c.department)).append(",")
            sb.append(escapeCsv(c.description)).append(",")
            sb.append(escapeCsv(c.mobile)).append(",")
            sb.append(escapeCsv(c.officePhone)).append("\n")
        }
        return sb.toString()
    }

    fun parseContactsFromCsv(csvContent: String): List<Contact> {
        val list = mutableListOf<Contact>()
        val lines = csvContent.replace("\r", "").split('\n')
        var isFirst = true
        lines.forEach { line ->
            if (line.isBlank()) return@forEach
            // Skip the BOM + header or simple header
            if (isFirst) {
                isFirst = false
                if (line.contains("نام") || line.contains("fullName")) {
                    return@forEach
                }
            }
            val fields = parseCsvLine(line)
            if (fields.size >= 2) {
                var pDate = fields.getOrNull(0) ?: ""
                // Strip BOM if present at the very beginning of parsing
                if (pDate.startsWith("\uFEFF")) {
                    pDate = pDate.substring(1)
                }
                val name = fields.getOrNull(1) ?: ""
                val pos = fields.getOrNull(2) ?: ""
                val dept = fields.getOrNull(3) ?: ""
                val desc = fields.getOrNull(4) ?: ""
                val mob = fields.getOrNull(5) ?: ""
                val office = fields.getOrNull(6) ?: ""

                if (name.isNotBlank()) {
                    var finalDate = pDate.trim()
                    if (!isValidPersianDate(finalDate)) {
                        finalDate = getCurrentPersianDate()
                    } else {
                        finalDate = normalizePersianDate(finalDate)
                    }
                    list.add(
                        Contact(
                            fullName = name.trim(),
                            position = pos.trim(),
                            department = dept.trim(),
                            description = desc.trim(),
                            mobile = mob.trim(),
                            officePhone = office.trim(),
                            persianDate = finalDate
                        )
                    )
                }
            }
        }
        return list
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    currentField.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(currentField.toString().trim())
                currentField = StringBuilder()
            } else {
                currentField.append(c)
            }
            i++
        }
        result.add(currentField.toString().trim())
        return result
    }
}
