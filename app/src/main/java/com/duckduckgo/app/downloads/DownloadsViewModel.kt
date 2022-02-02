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

package com.duckduckgo.app.downloads

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duckduckgo.app.browser.R
import com.duckduckgo.app.downloads.DownloadsViewModel.Command.DisplayMessage
import com.duckduckgo.app.downloads.DownloadsViewModel.Command.DisplayUndoMessage
import com.duckduckgo.app.downloads.DownloadsViewModel.Command.OpenFile
import com.duckduckgo.app.downloads.DownloadsViewModel.Command.ShareFile
import com.duckduckgo.app.downloads.model.DownloadItem
import com.duckduckgo.app.downloads.model.DownloadsRepository
import com.duckduckgo.app.global.DispatcherProvider
import com.duckduckgo.app.global.plugins.view_model.ViewModelFactoryPlugin
import com.duckduckgo.di.scopes.AppScope
import com.squareup.anvil.annotations.ContributesMultibinding
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import javax.inject.Inject
import javax.inject.Provider

class DownloadsViewModel
@Inject
constructor(
    private val downloadsRepository: DownloadsRepository,
    private val dispatcher: DispatcherProvider
) : ViewModel(), DownloadsItemListener {

    data class ViewState(val downloadItems: List<DownloadViewItem> = listOf(DownloadViewItem.Empty))

    sealed class Command {
        data class DisplayMessage(@StringRes val messageId: Int, val arg: String = "") : Command()
        data class DisplayUndoMessage(@StringRes val messageId: Int, val arg: String = "", val items: List<DownloadItem> = emptyList()) : Command()
        data class OpenFile(val item: DownloadItem) : Command()
        data class ShareFile(val item: DownloadItem) : Command()
    }

    private val command = Channel<Command>(1, DROP_OLDEST)

    fun downloads(): StateFlow<ViewState> = channelFlow {
        withContext(dispatcher.io()) {
            downloadsRepository.getDownloadsAsFlow().collectLatest {
                send(ViewState(it.mapToDownloadViewItems()))
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        ViewState()
    )

    fun commands(): Flow<Command> {
        return command.receiveAsFlow()
    }

    fun deleteAllDownloadedItems() {
        viewModelScope.launch(dispatcher.io()) {
            val itemsToDelete = downloadsRepository.getDownloads()
            if (itemsToDelete.isNotEmpty()) {
                downloadsRepository.deleteAll()
                command.send(DisplayUndoMessage(messageId = R.string.downloadsAllFilesDeletedMessage, items = itemsToDelete))
            }
        }
    }

    fun delete(item: DownloadItem) {
        viewModelScope.launch(dispatcher.io()) {
            downloadsRepository.delete(item.downloadId)
            command.send(DisplayMessage(R.string.downloadsCannotOpenFileErrorMessage))
        }
    }

    fun insert(items: List<DownloadItem>) {
        viewModelScope.launch(dispatcher.io()) {
            downloadsRepository.insertAll(items)
        }
    }

    fun deleteFilesFromDisk(items: List<DownloadItem>) {
        items.forEach {
            File(it.filePath).delete()
        }
    }

    override fun onItemClicked(item: DownloadItem) {
        viewModelScope.launch { command.send(OpenFile(item)) }
    }

    override fun onShareItemClicked(item: DownloadItem) {
        viewModelScope.launch { command.send(ShareFile(item)) }
    }

    override fun onDeleteItemClicked(item: DownloadItem) {
        viewModelScope.launch(dispatcher.io()) {
            downloadsRepository.delete(item.downloadId)
            command.send(DisplayUndoMessage(messageId = R.string.downloadsFileDeletedMessage, arg = item.fileName, items = listOf(item)))
        }
    }

    private fun DownloadItem.mapToDownloadViewItem(): DownloadViewItem = DownloadViewItem.Item(this)

    private fun List<DownloadItem>.mapToDownloadViewItems(): List<DownloadViewItem> {
        if (this.isEmpty()) return emptyList()

        val itemViews = mutableListOf<DownloadViewItem>()
        var previousDate = timestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(this[0].createdAt), ZoneId.systemDefault()))

        this.forEachIndexed { index, downloadItem ->
            if (index == 0) {
                itemViews.add(DownloadViewItem.Header(previousDate))
                itemViews.add(downloadItem.mapToDownloadViewItem())
            } else {
                val thisDate = timestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(downloadItem.createdAt), ZoneId.systemDefault()))
                if (previousDate == thisDate) {
                    itemViews.add(downloadItem.mapToDownloadViewItem())
                } else {
                    itemViews.add(DownloadViewItem.Header(thisDate))
                    itemViews.add(downloadItem.mapToDownloadViewItem())
                    previousDate = thisDate
                }
            }
        }

        return itemViews
    }

    // Use fuzzy date from vpn once is extracted to a common module.
    private fun timestamp(date: LocalDateTime = LocalDateTime.now()): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(date)
    }
}

@ContributesMultibinding(AppScope::class)
class DownloadsViewModelFactory
@Inject
constructor(private val viewModel: Provider<DownloadsViewModel>) : ViewModelFactoryPlugin {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T? {
        with(modelClass) {
            return when {
                isAssignableFrom(DownloadsViewModel::class.java) -> viewModel.get() as T
                else -> null
            }
        }
    }
}
