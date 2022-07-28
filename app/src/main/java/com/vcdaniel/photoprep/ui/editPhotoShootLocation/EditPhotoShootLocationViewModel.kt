package com.vcdaniel.photoprep.ui.editPhotoShootLocation

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.vcdaniel.photoprep.PhotoShootOverview
import com.vcdaniel.photoprep.database.getDatabase
import com.vcdaniel.photoprep.repository.PhotoPrepRepository
import kotlinx.coroutines.launch
import java.io.File

const val EDIT_PHOTO_SHOOT_LOCATION_VIEW_MODEL_TAG = "EditPhotosViewModel"

/** The view model for the EditPhotoShootLocationFragment */
class EditPhotoShootLocationViewModel(
    photoShootOverviewData: PhotoShootOverview,
    app: Application
) : AndroidViewModel(app) {

    /** Denotes if the changes should be saved or discarded */
    private var saveChanges: Boolean = false

    /** The path to an image the user has selected to replace the previous photo shoot image **/
    var temporaryImagePath: String = ""

    /** Denotes if saving is enabled or not */
    var enableSaving: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    /** The photo shoot location overview data */
    var photoShootOverview = MutableLiveData<PhotoShootOverview?>()

    var savePhotoShootComplete: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    var deletePhotoShootComplete: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    // Get the database and repository to interact with the asteroid data
    private val database = getDatabase(app)
    private val photoPrepRepository = PhotoPrepRepository(database)


    val deleteSuccessfulLogMessage = "File deletion successful"
    val deleteFailedLogMessage = "File deletion failed"
    val deleteFailedNoFileMessage =
        "File deletion failed Image file to be deleted was not found."

    override fun onCleared() {
        super.onCleared()

        // If the changes are not being saved such as if the user hit the back button, closed the app,
        // or hit the cancel button delete any temporarily created image. This is in part based
        // on the answer at https://stackoverflow.com/a/50987193
        if (!saveChanges) {
            if (temporaryImagePath.isNotBlank()) {
                deleteImage(temporaryImagePath)
            }
        }
    }

    /** Delete the photo at the supplied [imagePath] */
    fun deleteImage(imagePath: String) {
        val imageFile = File(imagePath)

        // This logic is in part based on the tutorial at
        // https://www.techiedelight.com/delete-a-file-in-kotlin/
        if (imageFile.exists()) {
            val result = imageFile.delete()
            if (result) {
                Log.i(EDIT_PHOTO_SHOOT_LOCATION_VIEW_MODEL_TAG, deleteSuccessfulLogMessage)
            } else {
                Log.i(EDIT_PHOTO_SHOOT_LOCATION_VIEW_MODEL_TAG, deleteFailedLogMessage)
            }
        } else {
            Log.i(
                EDIT_PHOTO_SHOOT_LOCATION_VIEW_MODEL_TAG,
                deleteFailedNoFileMessage
            )

        }
    }

    /** Create a view model and pass in the [photoShootOverviewData]. This is in part based on the
     * answer at  https://stackoverflow.com/a/46704702 */
    class EditPhotoShootLocationViewModelFactory(
        private val photoShootOverviewData: PhotoShootOverview,
        private val app: Application
    ) :
        ViewModelProvider.Factory {
        override fun <EditPhotoShootLocationViewModel : ViewModel> create(modelClass: Class<EditPhotoShootLocationViewModel>): EditPhotoShootLocationViewModel {
            return EditPhotoShootLocationViewModel(
                photoShootOverviewData,
                app
            ) as EditPhotoShootLocationViewModel
        }
    }

    init {
        // Create a copy of the photo shoot overview data
        setPhotoShootOverviewData(photoShootOverviewData)
    }

    /** The photo shoot's co-ordinates as a LatLng object */
    fun locationLatLng(): LatLng {
        return LatLng(
            photoShootOverview.value!!.locationLat,
            photoShootOverview.value!!.locationLng
        )
    }

    /** Save the changes made to the photo shoot location or save a new photo shoot location if the
     * user is creating a new photo shoot location*/
    fun savePhotoShootOverviewData() {
        viewModelScope.launch {
            if (photoShootOverview.value!!.photoShootLocationId != 0L) {
                photoPrepRepository.updatePhotoShootOverview(listOf(photoShootOverview.value!!))
            } else {
                photoPrepRepository.insertPhotoShootOverview(listOf(photoShootOverview.value!!))
            }
            savePhotoShootComplete.value = true
            saveChanges = true
        }
    }

    /** Delete the photo shoot location and any of it's associated data such as weather forecasts
     * or prep items */
    fun deletePhotoShoot() {
        viewModelScope.launch {
            photoShootOverview.value!!.let {
                photoPrepRepository.deletePhotoShootOverview(it.photoShootLocationId)
                deletePhotoShootComplete.value = true
            }
        }
    }

    /** Determine if the photo shoot location overview fulfills the requirements in order to be
     * saved. This validates if the user has specified a name and selected a location on the map. */
    fun isReadyToSave(): Boolean {
        return !photoShootOverview.value?.photoShootLocationName.isNullOrEmpty() && photoShootOverview.value?.locationLat != 0.0 && photoShootOverview.value?.locationLng != 0.0
    }

    /** Copy the existing photo shoot location overview data into a local object so the changes will
     * only be saved if the user chooses to save the data */
    private fun setPhotoShootOverviewData(overviewData: PhotoShootOverview) {
        photoShootOverview.value = PhotoShootOverview(
            photoShootLocationId = overviewData.photoShootLocationId,
            photoShootLocationName = overviewData.photoShootLocationName,
            mainImagePath = overviewData.mainImagePath,
            locationLng = overviewData.locationLng,
            locationLat = overviewData.locationLat,
            locationTitle = overviewData.locationTitle,
            nextPhotoShootHasGroup = overviewData.nextPhotoShootHasGroup,
            nextPhotoShootHasChild = overviewData.nextPhotoShootHasChild,
            nextPhotoShootHasPet = overviewData.nextPhotoShootHasPet,
            nextPhotoShootDate = overviewData.nextPhotoShootDate
        )
    }

    /** Set the photo shoot overview location information based off of a point of interest. */
    fun setLocation(poi: PointOfInterest) {
        photoShootOverview.value?.locationLng = poi.latLng.longitude
        photoShootOverview.value?.locationLat = poi.latLng.latitude
        photoShootOverview.value?.locationTitle = poi.name
    }
}