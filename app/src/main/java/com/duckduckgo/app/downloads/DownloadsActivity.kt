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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.StringRes
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.duckduckgo.app.browser.R
import com.duckduckgo.app.browser.databinding.ActivityDownloadsBinding
import com.duckduckgo.app.downloads.DownloadsViewModel.Command.DisplayMessage
import com.duckduckgo.app.downloads.DownloadsViewModel.Command.DisplayUndoMessage
import com.duckduckgo.app.downloads.DownloadsViewModel.Command.OpenFile
import com.duckduckgo.app.downloads.DownloadsViewModel.Command.ShareFile
import com.duckduckgo.app.global.DuckDuckGoActivity
import com.duckduckgo.mobile.android.ui.viewbinding.viewBinding
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

class DownloadsActivity : DuckDuckGoActivity() {

    private val viewModel: DownloadsViewModel by bindViewModel()
    private val binding: ActivityDownloadsBinding by viewBinding()

    private lateinit var downloadsAdapter: DownloadsAdapter

    @Inject
    lateinit var downloadsFileActions: DownloadsFileActions

    private val toolbar
        get() = binding.includeToolbar.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar(toolbar)
        setupRecyclerView()

        lifecycleScope.launch {
            viewModel.downloads()
                .flowWithLifecycle(lifecycle, STARTED)
                .collectLatest { render(it) }
        }

        lifecycleScope.launch {
            viewModel.commands()
                .flowWithLifecycle(lifecycle, STARTED)
                .collectLatest { processCommands(it) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.downloads_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.downloads_delete_all -> viewModel.deleteAllDownloadedItems()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun processCommands(command: DownloadsViewModel.Command) {
        when (command) {
            is OpenFile -> showOpen(command)
            is ShareFile -> showShare(command)
            is DisplayMessage -> showSnackbar(command.messageId, command.arg)
            is DisplayUndoMessage -> showUndo(command)
        }
    }

    private fun showOpen(command: OpenFile) {
        val file = File(command.item.filePath)
        if (file.exists()) {
            val result = downloadsFileActions.openFile(this@DownloadsActivity, file)
            if (!result) {
                showSnackbar(R.string.downloadsCannotOpenFileErrorMessage)
            }
        } else {
            viewModel.delete(command.item)
        }
    }

    private fun showShare(command: ShareFile) {
        downloadsFileActions.shareFile(this@DownloadsActivity, File(command.item.filePath))
    }

    private fun showUndo(command: DisplayUndoMessage) {
        Snackbar.make(
            binding.root,
            getString(command.messageId, command.arg),
            Snackbar.LENGTH_LONG
        ).setAction(R.string.downloadsUndoActionName) {
            // noop, handled in onDismissed callback
        }.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(
                transientBottomBar: Snackbar?,
                event: Int
            ) {
                when (event) {
                    // handle the UNDO action here as we only have one
                    BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION -> viewModel.insert(command.items)
                    BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_SWIPE,
                    BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_TIMEOUT -> viewModel.deleteFilesFromDisk(command.items)
                    BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_CONSECUTIVE,
                    BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_MANUAL -> { /* noop */ }
                }
            }
        }).show()
    }

    private fun showSnackbar(@StringRes messageId: Int, arg: String = "") {
        Snackbar.make(binding.root, getString(messageId, arg), Snackbar.LENGTH_LONG).show()
    }

    private fun render(viewState: DownloadsViewModel.ViewState) {
        if (viewState.downloadItems.isEmpty()) {
            downloadsAdapter.updateData(listOf(DownloadViewItem.Empty))
        } else {
            downloadsAdapter.updateData(viewState.downloadItems)
        }
    }

    private fun setupRecyclerView() {
        downloadsAdapter = DownloadsAdapter(viewModel)
        binding.downloadsContentView.layoutManager = LinearLayoutManager(this)
        binding.downloadsContentView.adapter = downloadsAdapter
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, DownloadsActivity::class.java)
    }
}
