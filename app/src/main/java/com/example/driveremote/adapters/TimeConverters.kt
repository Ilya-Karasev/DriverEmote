package com.example.driveremote.adapters

import androidx.room.TypeConverter

class TimeConverters {
    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        return value?.split(",") ?: emptyList()
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String {
        return list?.joinToString(",") ?: ""
    }
}
