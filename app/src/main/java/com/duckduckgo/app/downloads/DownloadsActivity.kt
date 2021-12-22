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
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.duckduckgo.app.browser.databinding.ActivityDownloadsBinding
import com.duckduckgo.app.downloads.DownloadsViewModel.Command.DisplayMessage
import com.duckduckgo.app.downloads.DownloadsViewModel.Command.OpenFile
import com.duckduckgo.app.global.DuckDuckGoActivity
import com.duckduckgo.mobile.android.ui.viewbinding.viewBinding
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
    lateinit var  downloadsFileActions: DownloadsFileActions

    private val toolbar
        get() = binding.includeToolbar.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar(toolbar)
        setupRecyclerView()

        lifecycleScope.launch {
            viewModel.downloads().flowWithLifecycle(lifecycle, STARTED).collect {
                render(it)
            }
        }

        lifecycleScope.launch {
            viewModel.commands().flowWithLifecycle(lifecycle, STARTED).collectLatest {
                when(it) {
                    is OpenFile -> {
                        downloadsFileActions.openFile(this@DownloadsActivity, File(it.item.filePath))
//                         downloadsFileActions.shareFile(this@DownloadsActivity, File(it.item.filePath))
                    }
                    else -> Snackbar.make(
                        binding.root,
                        (it as DisplayMessage).message,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
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
