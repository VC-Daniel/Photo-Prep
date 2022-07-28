package com.vcdaniel.photoprep.ui.editPhotoShootLocation

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * The view model for the AddPhotoShootModal. Facilitates saving a new photo or choosing an existing
 * photo from storage.
 */
class AddPhotoModalViewModel : ViewModel() {

    /** The absolute path to the photo the user has either taken or copied from an existing photo */
    var currentAbsolutePhotoPath: String = ""

    /** The path to the new photo the user has confirmed they would like to use */
    val photoPath = MutableLiveData<String>()

    /** The uri to the location to store the photo at */
    lateinit var photoURI: Uri

    /** The internal indicator of if the user wants to change the photo to a newly selected or taken photo */
    private val _changePhoto: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    /** Denotes if the user wants to change the photo to a newly selected or taken photo */
    val changePhoto: LiveData<Boolean>
        get() = _changePhoto

    /** Indicate that the user wants to change the photo that is currently being used to a newly
     * selected photo */
    fun changePhotoRequested() {
        _changePhoto.value = true
    }

    /** Once the photo has been changed to a new photo clear the viewModel data */
    fun changePhotoComplete() {
        photoPath.value = ""
        currentAbsolutePhotoPath = ""
        _changePhoto.value = false
    }

    /** Indicate the path to a new photo that has been taken or selected */
    fun savePath() {
        photoPath.value = currentAbsolutePhotoPath
    }
}