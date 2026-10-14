package com.example.data.recent

import kotlinx.coroutines.flow.Flow

class RecentFileRepository(private val dao: RecentFileDao) {
  val allRecentFiles: Flow<List<RecentFile>> = dao.getAllRecentFiles()

  suspend fun getFileByUri(uri: String): RecentFile? = dao.getFileByUri(uri)

  suspend fun saveRecentFile(file: RecentFile): Long = dao.insertOrUpdate(file)

  suspend fun deleteById(id: Long) = dao.deleteById(id)

  suspend fun clearAll() = dao.clearAll()
}
