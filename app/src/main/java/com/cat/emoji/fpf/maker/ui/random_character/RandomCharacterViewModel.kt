package com.cat.emoji.fpf.maker.ui.random_character

import androidx.lifecycle.ViewModel
import com.cat.emoji.fpf.maker.R
import com.cat.emoji.fpf.maker.core.helper.InternetHelper
import com.cat.emoji.fpf.maker.core.utils.state.HandleState
import com.cat.emoji.fpf.maker.data.model.custom.SuggestionModel
import com.cat.emoji.fpf.maker.ui.customize.CustomizeCharacterActivity
import kotlinx.coroutines.flow.MutableStateFlow

class RandomCharacterViewModel : ViewModel() {

    val randomList = ArrayList<SuggestionModel>()
    // Data từ API hay không
    private val _isDataAPI = MutableStateFlow(false)
    //-----------------------------------------------------------------------------------------------------------------

    suspend fun updateRandomList(suggestionModel: SuggestionModel){
        randomList.add(suggestionModel)
    }
    fun upsideDownList() = randomList.shuffle()

    fun setIsDataAPI(isAPI: Boolean) {
        _isDataAPI.value = isAPI
    }

    fun checkDataInternet(context: RandomCharacterActivity, action: (() -> Unit)) {
        if (!_isDataAPI.value) {
            action.invoke()
            return
        }
        InternetHelper.checkInternet(context) { result ->
            if (result == HandleState.SUCCESS) {
                action.invoke()
            } else {
                // Show No Internet dialog
                val dialog = com.cat.emoji.fpf.maker.dialog.YesNoDialog(
                    context,
                    R.string.no_internet,
                    R.string.please_check_your_internet,
                    isError = true,
                    dialogType = com.cat.emoji.fpf.maker.dialog.DialogType.INTERNET
                )
                dialog.show()
                dialog.onYesClick = {
                    dialog.dismiss()
                }
            }
        }
    }


}