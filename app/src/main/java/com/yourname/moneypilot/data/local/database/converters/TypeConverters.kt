package com.yourname.moneypilot.data.local.database.converters

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class LocalDateConverter {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? = dateString?.let { LocalDate.parse(it) }
}

class LocalDateTimeConverter {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /**
     * Section 12.0: All dates stored in UTC.
     * Converts LocalTime to UTC String before DB insertion.
     */
    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.atZone(java.time.ZoneId.systemDefault())
            ?.withZoneSameInstant(ZoneOffset.UTC)
            ?.toLocalDateTime()
            ?.format(formatter)
    }

    /**
     * Converts UTC String from DB back to System Local for UI.
     */
    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return dateTimeString?.let {
            LocalDateTime.parse(it, formatter)
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(java.time.ZoneId.systemDefault())
                .toLocalDateTime()
        }
    }
}
