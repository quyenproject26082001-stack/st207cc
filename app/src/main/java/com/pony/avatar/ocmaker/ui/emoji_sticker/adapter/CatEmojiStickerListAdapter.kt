package com.pony.avatar.ocmaker.ui.emoji_sticker.adapter

import com.pony.avatar.ocmaker.R
import com.pony.avatar.ocmaker.core.base.BaseAdapter
import com.pony.avatar.ocmaker.core.extensions.loadImage
import com.pony.avatar.ocmaker.core.extensions.tap
import com.pony.avatar.ocmaker.databinding.ItemCatEmojiStickerListBinding

class CatEmojiStickerListAdapter : BaseAdapter<String, ItemCatEmojiStickerListBinding>(ItemCatEmojiStickerListBinding::inflate) {
    var onItemClick: ((String) -> Unit) = {}
    var onItemLongClick: ((String) -> Unit) = {}
    var onSelectionChanged: ((Int) -> Unit) = {} // Callback khi số lượng selected thay đổi
    var onDownloadClick: ((String) -> Unit) = {} // Callback khi click download từng item

    var isSelectMode = false
        private set
    private val selectedItems = mutableSetOf<String>()

    fun enterSelectMode() {
        isSelectMode = true
        notifyDataSetChanged()
    }

    fun exitSelectMode() {
        isSelectMode = false
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<String> = selectedItems.toList()

    fun getSelectedCount(): Int = selectedItems.size

    fun isAllSelected(): Boolean = selectedItems.size == itemCount && itemCount > 0

    fun selectAll() {
        items.forEach { selectedItems.add(it) }
        onSelectionChanged.invoke(selectedItems.size)
        notifyDataSetChanged()
    }

    fun deselectAll() {
        selectedItems.clear()
        onSelectionChanged.invoke(0)
        notifyDataSetChanged()
    }

    private fun toggleSelection(item: String) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
        } else {
            selectedItems.add(item)
        }
        onSelectionChanged.invoke(selectedItems.size)
        notifyDataSetChanged()
    }

    override fun onBind(binding: ItemCatEmojiStickerListBinding, item: String, position: Int) {
        binding.apply {
            loadImage(item, imvImage, onShowLoading = {
                sflShimmer.visibility = android.view.View.VISIBLE
                sflShimmer.showShimmer(true)
            }, onDismissLoading = {
                sflShimmer.hideShimmer()
                sflShimmer.visibility = android.view.View.GONE
            })

            // Hiển thị/ẩn icon select và download
            if (isSelectMode) {
                btnSelect.visibility = android.view.View.VISIBLE
                btnDownload.visibility = android.view.View.GONE
                val isSelected = selectedItems.contains(item)
                btnSelect.setImageResource(
                    if (isSelected) R.drawable.ic_selected else R.drawable.ic_not_select
                )
            } else {
                btnSelect.visibility = android.view.View.GONE
                btnDownload.visibility = android.view.View.VISIBLE
            }

            // Download button click
            btnDownload.tap {
                onDownloadClick.invoke(item)
            }

            root.tap {
                if (isSelectMode) {
                    toggleSelection(item)
                } else {
                    onItemClick.invoke(item)
                }
            }

            root.setOnLongClickListener {
                if (!isSelectMode) {
                    onItemLongClick.invoke(item)
                    toggleSelection(item) // Chọn luôn item đầu tiên
                }
                true
            }
        }
    }
}
