// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.data.recent

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFileDao {
  @Query("SELECT * FROM recent_files ORDER BY lastOpenedTimestamp DESC LIMIT 50")
  fun getAllRecentFiles(): Flow<List<RecentFile>>

  @Query("SELECT * FROM recent_files WHERE fileUri = :uri LIMIT 1")
  suspend fun getFileByUri(uri: String): RecentFile?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(file: RecentFile): Long

  @Update
  suspend fun update(file: RecentFile)

  @Query("DELETE FROM recent_files WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM recent_files")
  suspend fun clearAll()
}
