package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val position: String = "",
    val department: String = "",
    val description: String = "",
    val mobile: String = "",
    val officePhone: String = "",
    val persianDate: String = ""
)
