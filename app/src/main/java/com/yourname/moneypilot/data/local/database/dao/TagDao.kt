package com.yourname.moneypilot.data.local.database.dao

import androidx.room.*
import com.yourname.moneypilot.data.local.database.entities.TagEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTags(tags: List<TagEntity>)

    @Query("SELECT * FROM tags")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags")
    suspend fun getAllTagsList(): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionTagCrossRef(crossRef: TransactionTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCrossRefs(crossRefs: List<TransactionTagCrossRef>)

    @Query("SELECT * FROM transaction_tag_cross_ref")
    suspend fun getAllCrossRefs(): List<TransactionTagCrossRef>

    @Transaction
    @Query("""
        SELECT tags.* FROM tags 
        JOIN transaction_tag_cross_ref ON tags.id = transaction_tag_cross_ref.tagId 
        WHERE transaction_tag_cross_ref.transactionId = :transactionId
    """)
    fun getTagsForTransaction(transactionId: Long): Flow<List<TagEntity>>

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()

    @Query("DELETE FROM transaction_tag_cross_ref")
    suspend fun deleteAllCrossRefs()
}
