package com.vcdaniel.photoprep.ui.photoShootLocations

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vcdaniel.photoprep.database.getDatabase
import com.vcdaniel.photoprep.repository.PhotoPrepRepository
import kotlinx.coroutines.launch

/** The view model for the PhotoShootLocationsFragment */
class PhotoShootLocationsViewModel(app: Application) : AndroidViewModel(app) {

    // Get the database and repository to interact with the photo shoot location data
    private val database = getDatabase(app)
    private val photoPrepRepository = PhotoPrepRepository(database)

    // The LiveData for the photo shoot location to display
    val photoShootLocations = photoPrepRepository.photoShootLocations

    init {
        // Retrieve the latest photo shoot locations
        refreshPhotoShootLocations()
    }

    /** Retrieve the latest photo shoot locations */
    fun refreshPhotoShootLocations() {
        viewModelScope.launch {
            photoPrepRepository.getAllPhotoShootLocations()
        }
    }
}