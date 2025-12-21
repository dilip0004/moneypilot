package com.yourname.moneypilot.data.local.database.dao

import androidx.room.*
import com.yourname.moneypilot.data.local.database.entities.TagEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM tags")
    fun getAllTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionTagCrossRef(crossRef: TransactionTagCrossRef)

    @Transaction
    @Query("""
        SELECT tags.* FROM tags 
        JOIN transaction_tag_cross_ref ON tags.id = transaction_tag_cross_ref.tagId 
        WHERE transaction_tag_cross_ref.transactionId = :transactionId
    """)
    fun getTagsForTransaction(transactionId: Long): Flow<List<TagEntity>>
}
