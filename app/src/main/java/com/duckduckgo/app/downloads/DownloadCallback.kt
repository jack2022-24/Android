/*
 * Copyright (c) 2022 DuckDuckGo
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

package com.duckduckgo.app.downloads

import androidx.annotation.StringRes
import com.duckduckgo.app.browser.R
import com.duckduckgo.app.downloads.model.DownloadStatus
import com.duckduckgo.app.downloads.model.DownloadsRepository
import com.duckduckgo.app.global.DispatcherProvider
import com.duckduckgo.di.scopes.AppScope
import dagger.SingleInstanceIn
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

interface DownloadCallback {
    suspend fun onStart(fileName: String)
    suspend fun onSuccess(downloadId: Long, contentLength: Long)
    suspend fun onFailure(downloadId: Long)
    fun commands(): Flow<FileDownloadCallback.DownloadCommand>
}

@SingleInstanceIn(AppScope::class)
class FileDownloadCallback @Inject constructor(
    private val downloadsRepository: DownloadsRepository,
    private val dispatcher: DispatcherProvider
) : DownloadCallback {

    sealed class DownloadCommand {
        class ShowDownloadStartedMessage(@StringRes val messageId: Int, val fileName: String) : DownloadCommand()
        class ShowDownloadSuccessMessage(@StringRes val messageId: Int, val fileName: String, val filePath: String) : DownloadCommand()
        class ShowDownloadFailedMessage(@StringRes val messageId: Int) : DownloadCommand()
    }

    private val command = Channel<DownloadCommand>(1, BufferOverflow.DROP_OLDEST)

    override suspend fun onStart(fileName: String) {
        command.send(DownloadCommand.ShowDownloadStartedMessage(R.string.downloadsDownloadStartedMessage, fileName))
    }

    override suspend fun onSuccess(downloadId: Long, contentLength: Long) {
        downloadsRepository.update(
            downloadId = downloadId,
            downloadStatus = DownloadStatus.FINISHED,
            contentLength = contentLength
        )
        downloadsRepository.getDownloadItem(downloadId).let {
            command.send(DownloadCommand.ShowDownloadSuccessMessage(R.string.downloadsDownloadFinishedMessage, it.fileName, it.filePath))
        }
    }

    override suspend fun onFailure(downloadId: Long) {
        command.send(DownloadCommand.ShowDownloadFailedMessage(R.string.downloadsDownloadErrorMessage))
    }

    override fun commands(): Flow<DownloadCommand> {
        return command.receiveAsFlow()
    }
}
