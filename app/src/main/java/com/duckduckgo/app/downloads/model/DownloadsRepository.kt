/*
 * Copyright (c) 2021 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.app.downloads.model

import com.duckduckgo.app.downloads.db.DownloadEntity
import com.duckduckgo.app.downloads.db.DownloadsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface DownloadsRepository {
    fun insert(downloadItem: DownloadItem): Long
    fun update(downloadId: Long, downloadStatus: Int, contentLength: Long)
    fun getDownloads(): Flow<List<DownloadItem>>
    fun getDownloadItem(downloadId: Long): Flow<DownloadItem>
}

class DefaultDownloadsRepository(private val downloadsDao: DownloadsDao) : DownloadsRepository {

    override fun insert(downloadItem: DownloadItem): Long {
        return downloadsDao.insert(downloadItem.mapToDownloadEntity())
    }

    override fun update(downloadId: Long, downloadStatus: Int, contentLength: Long) {
        downloadsDao.update(downloadId, downloadStatus, contentLength)
    }

    override fun getDownloads(): Flow<List<DownloadItem>> {
        return downloadsDao.getDownloads().distinctUntilChanged().map { it.mapToDownloadItems() }
    }

    override fun getDownloadItem(downloadId: Long): Flow<DownloadItem> {
        return downloadsDao.getDownloadItem(downloadId).distinctUntilChanged().map { it.mapToDownloadItem() }
    }

    private fun DownloadEntity.mapToDownloadItem(): DownloadItem =
        DownloadItem(
            id = this.id,
            downloadId = this.downloadId,
            fileName = this.fileName,
            contentLength = this.contentLength,
            filePath = this.filePath,
            createdAt = this.createdAt,
        )

    private fun List<DownloadEntity>.mapToDownloadItems(): List<DownloadItem> =
        this.map { it.mapToDownloadItem() }

    private fun DownloadItem.mapToDownloadEntity(): DownloadEntity =
        DownloadEntity(
            id = this.id,
            downloadId = this.downloadId,
            fileName = this.fileName,
            contentLength = this.contentLength,
            filePath = this.filePath,
            createdAt = this.createdAt,
        )
}
