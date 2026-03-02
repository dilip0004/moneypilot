package com.yourname.moneypilot.data.local.database.converters

import androidx.room.TypeConverter
import com.yourname.moneypilot.data.local.model.AccountType

class AccountTypeConverter {

    @TypeConverter
    fun fromAccountType(type: AccountType): String {
        return type.name
    }

    @TypeConverter
    fun toAccountType(value: String): AccountType {
        return try {
            AccountType.valueOf(value)
        } catch (e: Exception) {
            AccountType.BANK
        }
    }
}