package com.vcdaniel.photoprep.ui.commonPrepLibrary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vcdaniel.photoprep.CommonPrep
import com.vcdaniel.photoprep.database.getDatabase
import com.vcdaniel.photoprep.repository.PhotoPrepRepository
import kotlinx.coroutines.launch

/** The view model for the common prep library. */
class CommonPrepViewModel(app: Application) : AndroidViewModel(app) {

    // Get the database and repository to interact with the common prep data
    private val database = getDatabase(app)
    private val photoPrepRepository = PhotoPrepRepository(database)

    // The LiveData for all the common prep to display to the user
    val commonPrep = photoPrepRepository.commonPrep

    init {
        // Get common prep from the database
        viewModelScope.launch {
            photoPrepRepository.getAllCommonPrep()
        }
    }

    /** Save the common prep */
    fun savePrepItem(commonPrep: CommonPrep) {
        viewModelScope.launch {
            photoPrepRepository.insertCommonPrep(listOf(commonPrep))
        }
    }

    /** Delete the prep item with the corresponding [id] */
    fun deletePrepItem(id: Long) {
        viewModelScope.launch {
            photoPrepRepository.removeCommonPrepData(id)
        }
    }
}