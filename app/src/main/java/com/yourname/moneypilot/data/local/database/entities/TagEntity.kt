package com.yourname.moneypilot.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Int
)

@Entity(tableName = "transaction_tag_cross_ref", primaryKeys = ["transactionId", "tagId"])
data class TransactionTagCrossRef(
    val transactionId: Long,
    val tagId: Long
)
