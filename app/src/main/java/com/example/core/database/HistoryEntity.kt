package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processing_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val toolType: String,
    val originalSizeBytes: Long,
    val resultSizeBytes: Long,
    val filePath: String,
    val fileUri: String,
    val pageCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
) {
    val formattedSize: String
        get() {
            val bytes = if (resultSizeBytes > 0) resultSizeBytes else originalSizeBytes
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> String.format("%.2f MB", mb)
                kb >= 1.0 -> String.format("%.1f KB", kb)
                else -> "$bytes B"
            }
        }
}
