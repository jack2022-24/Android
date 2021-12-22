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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.duckduckgo.app.browser.databinding.ViewItemDownloadsEmptyBinding
import com.duckduckgo.app.browser.databinding.ViewItemDownloadsHeaderBinding
import com.duckduckgo.app.browser.databinding.ViewItemDownloadsItemBinding
import com.duckduckgo.app.downloads.DownloadViewItem.Empty
import com.duckduckgo.app.downloads.DownloadViewItem.Header
import com.duckduckgo.app.downloads.DownloadViewItem.Item

class DownloadsAdapter(private val downloadsItemListener: DownloadsItemListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<DownloadViewItem> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_EMPTY -> EmptyViewHolder(binding = ViewItemDownloadsEmptyBinding.inflate(inflater, parent, false))
            VIEW_TYPE_HEADER -> HeaderViewHolder(binding = ViewItemDownloadsHeaderBinding.inflate(inflater, parent, false))
            VIEW_TYPE_ITEM -> ItemViewHolder(
                binding = ViewItemDownloadsItemBinding.inflate(inflater, parent, false),
                listener = downloadsItemListener
            )
            else -> throw IllegalArgumentException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            VIEW_TYPE_EMPTY -> (holder as EmptyViewHolder)
            VIEW_TYPE_HEADER -> (holder as HeaderViewHolder).bind(items[position] as Header)
            VIEW_TYPE_ITEM -> (holder as ItemViewHolder).bind(items[position] as Item)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is Empty -> VIEW_TYPE_EMPTY
            is Header -> VIEW_TYPE_HEADER
            is Item -> VIEW_TYPE_ITEM
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(list: List<DownloadViewItem>) {
        val diffCallback = DiffCallback(old = this.items, new = list)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.items = list
        diffResult.dispatchUpdatesTo(this)
    }

    class EmptyViewHolder(val binding: ViewItemDownloadsEmptyBinding) :
        RecyclerView.ViewHolder(binding.root)

    class HeaderViewHolder(val binding: ViewItemDownloadsHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Header) {
            binding.downloadsHeaderTextView.text = item.text
        }
    }

    class ItemViewHolder(val binding: ViewItemDownloadsItemBinding, val listener: DownloadsItemListener) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item) {
            binding.downloadsFileName.text = item.downloadItem.fileName
            binding.downloadsFileSize.text = item.downloadItem.contentLength.toString()
            binding.downloadsItem.setOnClickListener {
                listener.onItemClicked(item.downloadItem)
            }
        }
    }

    class DiffCallback(
        private val old: List<DownloadViewItem>,
        private val new: List<DownloadViewItem>
    ) : DiffUtil.Callback() {

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return old[oldItemPosition] == new[newItemPosition]
        }

        override fun getOldListSize(): Int {
            return old.size
        }

        override fun getNewListSize(): Int {
            return new.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return old[oldItemPosition] == new[newItemPosition]
        }
    }

    companion object {
        const val VIEW_TYPE_EMPTY = 0
        const val VIEW_TYPE_HEADER = 1
        const val VIEW_TYPE_ITEM = 2
    }
}
