package com.cat.emoji.fpf.maker.ui.my_creation.fragment

import android.app.ActivityOptions
import android.content.Intent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.cat.emoji.fpf.maker.R
import com.cat.emoji.fpf.maker.core.base.BaseFragment
import com.cat.emoji.fpf.maker.core.extensions.gone
import com.cat.emoji.fpf.maker.core.extensions.hideNavigation
import com.cat.emoji.fpf.maker.core.extensions.showInterAll
import com.cat.emoji.fpf.maker.core.extensions.visible
import com.cat.emoji.fpf.maker.core.helper.InternetHelper
import com.cat.emoji.fpf.maker.core.helper.LanguageHelper
import com.cat.emoji.fpf.maker.core.utils.key.IntentKey
import com.cat.emoji.fpf.maker.core.utils.key.ValueKey
import com.cat.emoji.fpf.maker.databinding.FragmentMyEmojiBinding
import com.cat.emoji.fpf.maker.dialog.DialogType
import com.cat.emoji.fpf.maker.dialog.YesNoDialog
import com.cat.emoji.fpf.maker.ui.emoji_custom.EmojiCustomActivity
import com.cat.emoji.fpf.maker.ui.my_creation.MyCreationActivity
import com.cat.emoji.fpf.maker.ui.my_creation.adapter.MyAvatarAdapter
import com.cat.emoji.fpf.maker.ui.my_creation.view_model.MyCreationViewModel
import com.cat.emoji.fpf.maker.ui.my_creation.view_model.MyEmojiViewModel
import com.cat.emoji.fpf.maker.ui.view.ViewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyEmojiFragment : BaseFragment<FragmentMyEmojiBinding>() {
    private val viewModel: MyEmojiViewModel by viewModels()
    private val myCreationViewModel: MyCreationViewModel by activityViewModels()
    private val myEmojiAdapter by lazy { MyAvatarAdapter(requireActivity()) }

    private val myAlbumActivity: MyCreationActivity
        get() = requireActivity() as MyCreationActivity

    override fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentMyEmojiBinding {
        return FragmentMyEmojiBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        initRcv()
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.myEmojiList.collect { list ->
                        myEmojiAdapter.submitList(list)
                        binding.layoutNoItem.isVisible = list.isEmpty()
                    }
                }
                launch {
                    myCreationViewModel.typeStatus
                        .drop(1)
                        .collect {
                            resetData()
                        }
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            rcvMyEmoji.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(
                    recyclerView: RecyclerView, motionEvent: MotionEvent
                ): Boolean {
                    return when {
                        motionEvent.action != MotionEvent.ACTION_UP || recyclerView.findChildViewUnder(
                            motionEvent.x, motionEvent.y
                        ) != null -> false

                        else -> {
                            resetData()
                            true
                        }
                    }
                }

                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
                override fun onTouchEvent(recyclerView: RecyclerView, motionEvent: MotionEvent) {}
            })

            myEmojiAdapter.onItemClick = { pathInternal -> handleItemClick(pathInternal) }
            myEmojiAdapter.onItemTick = { position ->
                viewModel.toggleSelect(position)
                val allSelected = viewModel.myEmojiList.value.all { it.isSelected }
                myAlbumActivity.updateSelectAllIcon(allSelected)
            }
            myEmojiAdapter.onEditClick = { pathInternal -> handleEditClick(pathInternal) }
            myEmojiAdapter.onDeleteClick = { pathInternal -> handleDelete(arrayListOf(pathInternal)) }
            myEmojiAdapter.onLongClick = { position -> handleLongClick(position) }
        }
    }

    private fun initRcv() {
        binding.apply {
            rcvMyEmoji.apply {
                adapter = myEmojiAdapter
                itemAnimator = null
                setHasFixedSize(true)
                setItemViewCacheSize(20)
                isNestedScrollingEnabled = true
            }
        }
    }

    private fun handleDelete(pathInternalList: ArrayList<String>) {
        if (pathInternalList.isEmpty()) {
            myAlbumActivity.showToast(R.string.please_select_an_image)
            return
        }
        val dialog = YesNoDialog(myAlbumActivity, R.string.delete, R.string.are_you_sure_want_to_delete_this_item)
        LanguageHelper.setLocale(myAlbumActivity)
        dialog.show()
        dialog.onDismissClick = {
            dialog.dismiss()
            myAlbumActivity.hideNavigation()
            resetData()
        }
        dialog.onNoClick = {
            dialog.dismiss()
            myAlbumActivity.hideNavigation()
        }
        dialog.onYesClick = {
            lifecycleScope.launch(Dispatchers.IO) {
                viewModel.deleteItem(myAlbumActivity, pathInternalList)
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    myAlbumActivity.hideNavigation()
                    resetData()
                }
            }
        }
    }

    private fun handleEditClick(pathInternal: String) {
        // Check internet before entering edit mode
        if (!InternetHelper.isInternetAvailable(myAlbumActivity)) {
            showNoInternetDialog()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            myAlbumActivity.showLoading()
            val success = viewModel.prepareEmojiEdit(myAlbumActivity, pathInternal)
            withContext(Dispatchers.Main) {
                myAlbumActivity.dismissLoading()
                if (success) {
                    val intent = Intent(myAlbumActivity, EmojiCustomActivity::class.java).apply {
                        putExtra(IntentKey.STATUS_FROM_KEY, ValueKey.EDIT)
                    }
                    val options = android.app.ActivityOptions.makeCustomAnimation(
                        myAlbumActivity, R.anim.slide_out_left, R.anim.slide_in_right
                    )
                    myAlbumActivity.showInterAll { startActivity(intent) }
                } else {
                    myAlbumActivity.showToast(R.string.error)
                }
            }
        }
    }

    private fun showNoInternetDialog() {
        val dialog = YesNoDialog(
            myAlbumActivity,
            R.string.no_internet,
            R.string.please_check_your_internet,
            isError = true,
            dialogType = DialogType.INTERNET
        )
        dialog.show()
        dialog.onYesClick = {
            dialog.dismiss()
            myAlbumActivity.hideNavigation()
        }
    }

    private fun handleItemClick(pathInternal: String) {
        val intent = Intent(myAlbumActivity, ViewActivity::class.java)
        intent.putExtra(IntentKey.INTENT_KEY, pathInternal)
        intent.putExtra(IntentKey.TYPE_KEY, ValueKey.TYPE_VIEW)
        intent.putExtra(IntentKey.STATUS_KEY, ValueKey.AVATAR_TYPE)
        val options = ActivityOptions.makeCustomAnimation(myAlbumActivity, R.anim.slide_in_right, R.anim.slide_out_left)
        myAlbumActivity.showInterAll { startActivity(intent, options.toBundle()) }
    }

    private fun handleLongClick(position: Int) {
        viewModel.showLongClick(position)
        myAlbumActivity.binding.lnlBottom.visible()
        myAlbumActivity.enterSelectionMode()
        myEmojiAdapter.isSelectMode = true

        val allSelected = viewModel.myEmojiList.value.all { it.isSelected }
        myAlbumActivity.updateSelectAllIcon(allSelected)
    }

    private fun resetData() {
        viewModel.loadMyEmoji(myAlbumActivity)
        myAlbumActivity.binding.lnlBottom.gone()
        myAlbumActivity.exitSelectionMode()
        myEmojiAdapter.isSelectMode = false
    }

    fun deleteSelectedItems() {
        handleDelete(viewModel.getPathSelected())
    }

    fun getSelectedPaths(): ArrayList<String> {
        return viewModel.getPathSelected()
    }

    fun selectAllItems() {
        viewModel.selectAll(true)
        myEmojiAdapter.notifyItemRangeChanged(0, myEmojiAdapter.itemCount)
    }

    fun deselectAllItems() {
        viewModel.selectAll(false)
        myEmojiAdapter.notifyItemRangeChanged(0, myEmojiAdapter.itemCount)
    }

    fun resetSelectionMode() {
        viewModel.clearSelection()
        myAlbumActivity.binding.lnlBottom.gone()
        myAlbumActivity.exitSelectionMode()
        myEmojiAdapter.isSelectMode = false
    }

    override fun onStart() {
        super.onStart()
        resetData()
    }
}
