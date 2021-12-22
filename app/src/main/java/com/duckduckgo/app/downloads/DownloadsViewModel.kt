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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duckduckgo.app.downloads.DownloadsViewModel.Command.OpenFile
import com.duckduckgo.app.downloads.model.DownloadItem
import com.duckduckgo.app.downloads.model.DownloadsRepository
import com.duckduckgo.app.global.plugins.view_model.ViewModelFactoryPlugin
import com.duckduckgo.di.scopes.AppScope
import com.squareup.anvil.annotations.ContributesMultibinding
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel
@Inject
constructor(
    private val downloadsRepository: DownloadsRepository
) : ViewModel(), DownloadsItemListener {

    data class ViewState(val downloadItems: List<DownloadViewItem>)

    sealed class Command {
        data class DisplayMessage(val message: String) : Command()
        data class OpenFile(val item: DownloadItem) : Command()
        data class ShareFile(val filename: String) : Command()
        data class DeleteFile(val id: String) : Command()
    }

    private val command = Channel<Command>(1, DROP_OLDEST)

    fun downloads(): StateFlow<ViewState> = flow {
                downloadsRepository.getDownloads().collect {
                    emit(ViewState(downloadItems = it.mapToDownloadViewItems()))
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(),
                ViewState(downloadItems = listOf(DownloadViewItem.Empty)))

    fun commands(): Flow<Command> {
        return command.receiveAsFlow()
    }

    override fun onItemClicked(item: DownloadItem) {
        viewModelScope.launch { command.send(OpenFile(item)) }
    }

    private fun DownloadItem.mapToDownloadViewItem(): DownloadViewItem = DownloadViewItem.Item(this)

    private fun List<DownloadItem>.mapToDownloadViewItems(): List<DownloadViewItem> =
        this.map { it.mapToDownloadViewItem() }
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
