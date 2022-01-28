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

package com.duckduckgo.app.downloads.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.duckduckgo.app.downloads.model.DownloadStatus.STARTED

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var downloadId: Long,
    var downloadStatus: Int = STARTED,
    var fileName: String,
    var contentLength: Long = 0,
    var filePath: String,
    var createdAt: Long = System.currentTimeMillis(),
)