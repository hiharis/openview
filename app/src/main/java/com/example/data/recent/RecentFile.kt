package com.example.data.recent

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_files")
data class RecentFile(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val fileName: String,
  val fileUri: String,
  val extension: String,
  val sizeBytes: Long,
  val lastOpenedTimestamp: Long = System.currentTimeMillis(),
  val lastPageOrScrollPosition: Int = 0,
)
