package com.yourname.moneypilot.data.local.database.converters

import androidx.room.TypeConverter
import com.yourname.moneypilot.data.local.database.entities.TransactionType

class TransactionTypeConverter {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return TransactionType.valueOf(value)
    }
}
