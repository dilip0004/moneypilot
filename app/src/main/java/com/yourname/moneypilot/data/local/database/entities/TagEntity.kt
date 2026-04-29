package com.yourname.moneypilot.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "tags")
@Serializable
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Int
)

@Entity(tableName = "transaction_tag_cross_ref", primaryKeys = ["transactionId", "tagId"])
@Serializable
data class TransactionTagCrossRef(
    val transactionId: String, // Changed to String to match TransactionEntity ID type
    val tagId: Long
)
