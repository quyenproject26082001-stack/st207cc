package com.pony.avatar.ocmaker.ui.random_character

import androidx.lifecycle.ViewModel
import com.pony.avatar.ocmaker.R
import com.pony.avatar.ocmaker.core.helper.InternetHelper
import com.pony.avatar.ocmaker.core.utils.state.HandleState
import com.pony.avatar.ocmaker.data.model.custom.SuggestionModel
import com.pony.avatar.ocmaker.ui.customize.CustomizeCharacterActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RandomCharacterViewModel : ViewModel() {

    val randomList = ArrayList<SuggestionModel>()
    // Data từ API hay không
    private val _isDataAPI = MutableStateFlow(false)
    //-----------------------------------------------------------------------------------------------------------------

    private val cache = mutableMapOf<Int, ArrayList<SuggestionModel>>()

    fun getCached(type: Int): ArrayList<SuggestionModel>? = cache[type]

    fun setCached(type: Int,list: ArrayList<SuggestionModel>){
        cache[type] = list
    }

    fun clearCache(type: Int? = null){
        if(type ==null) cache.clear() else cache.remove(type)
    }

    suspend fun updateRandomList(suggestionModel: SuggestionModel){
        randomList.add(suggestionModel)
    }
    fun upsideDownList() = randomList.shuffle()

    fun setIsDataAPI(isAPI: Boolean) {
        _isDataAPI.value = isAPI
    }

    private val _typeStatus = MutableStateFlow<Int>(-1)
    val typeStatus = _typeStatus.asStateFlow()

    fun setTypeStatus(type: Int){
        if (type == _typeStatus.value) return
        _typeStatus.value = type
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
                val dialog = com.pony.avatar.ocmaker.dialog.YesNoDialog(
                    context,
                    R.string.no_internet,
                    R.string.please_check_your_internet,
                    isError = true,
                    dialogType = com.pony.avatar.ocmaker.dialog.DialogType.INTERNET
                )
                dialog.show()
                dialog.onYesClick = {
                    dialog.dismiss()
                }
            }
        }
    }


}