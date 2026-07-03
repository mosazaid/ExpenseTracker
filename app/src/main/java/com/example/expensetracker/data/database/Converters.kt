package com.example.expensetracker.data.database

import androidx.room.TypeConverter
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.RecurrenceFrequency
import com.example.expensetracker.data.database.entities.TransactionType
import java.util.Date

class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromTransactionType(type: TransactionType): String {
        return type.name
    }

    @TypeConverter
    fun toTransactionType(type: String): TransactionType {
        return TransactionType.valueOf(type)
    }

    @TypeConverter
    fun fromAccountType(type: AccountType): String {
        return type.name
    }

    @TypeConverter
    fun toAccountType(type: String): AccountType {
        return AccountType.valueOf(type)
    }

    @TypeConverter
    fun fromNullableAccountType(type: AccountType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toNullableAccountType(type: String?): AccountType? {
        return type?.let { AccountType.valueOf(it) }
    }

    @TypeConverter
    fun fromRecurrenceFrequency(frequency: RecurrenceFrequency): String {
        return frequency.name
    }

    @TypeConverter
    fun toRecurrenceFrequency(frequency: String): RecurrenceFrequency {
        return RecurrenceFrequency.valueOf(frequency)
    }
}
