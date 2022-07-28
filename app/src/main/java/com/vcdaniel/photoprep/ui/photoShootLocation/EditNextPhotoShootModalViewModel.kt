package com.vcdaniel.photoprep.ui.photoShootLocation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** The view model for the EditNextPhotoShootModalSheet. Facilitates editing the next photo shoot for a
 * specific photo shoot location */
class EditNextPhotoShootModalViewModel(
    isGroupChecked: Boolean,
    isChildChecked: Boolean,
    isPetChecked: Boolean,
    nextPhotoShootDate: Long
) : ViewModel() {

    /** Denotes if the next photo shoot subject(s) will include a group */
    val isGroupChecked: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    /** Denotes if the next photo shoot subject(s) will include a child */
    val isChildChecked: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    /** Denotes if the next photo shoot subject(s) will include a pet */
    val isPetChecked: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    /** The internal next photo shoot date */
    private val _nextPhotoShootDate: MutableLiveData<Long> = MutableLiveData<Long>(0L)

    /** The date of the next photo shoot at this photo shoot location */
    val nextPhotoShootDate: LiveData<Long>
        get() = _nextPhotoShootDate

    /** Create a view model and pass in the provided data about the next photo shoot. This is in
     * part based on the answer at  https://stackoverflow.com/a/46704702 */
    class EditPhotoShootLocationViewModelFactory(
        private val isGroupChecked: Boolean,
        private val isChildChecked: Boolean,
        private val isPetChecked: Boolean,
        private val nextPhotoShootDate: Long
    ) :
        ViewModelProvider.Factory {
        override fun <EditNextPhotoShootModalViewModel : ViewModel> create(modelClass: Class<EditNextPhotoShootModalViewModel>): EditNextPhotoShootModalViewModel {
            return EditNextPhotoShootModalViewModel(
                isGroupChecked = isGroupChecked,
                isChildChecked = isChildChecked,
                isPetChecked = isPetChecked,
                nextPhotoShootDate = nextPhotoShootDate
            ) as EditNextPhotoShootModalViewModel
        }
    }

    /** Copy the existing next photo shoot data into local objects so the changes will
     * only be saved if the user chooses to save them. */
    init {
        this.isGroupChecked.value = isGroupChecked
        this.isChildChecked.value = isChildChecked
        this.isPetChecked.value = isPetChecked
        _nextPhotoShootDate.value = nextPhotoShootDate
    }

    /** Replace the date for the next photo shoot with the provided [timeInMillis] */
    fun setNextPhotoShootDate(timeInMillis: Long) {
        _nextPhotoShootDate.value = timeInMillis
    }
}