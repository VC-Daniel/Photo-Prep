package com.vcdaniel.photoprep.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.vcdaniel.photoprep.*
import com.vcdaniel.photoprep.api.WeatherDataApi
import com.vcdaniel.photoprep.api.asDatabaseModel
import com.vcdaniel.photoprep.api.parseWeatherForecastJsonResult
import com.vcdaniel.photoprep.database.PhotoPrepDatabase
import com.vcdaniel.photoprep.database.asDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Facilitates interaction with the photo shoot and common prep data in the repository. The
 * repository uses a database to persist data about places the user commonly takes photos at
 * as well as common prep. */
class PhotoPrepRepository(private val database: PhotoPrepDatabase) {

    /** Internal version of all the common prep */
    private var _commonPrep = MutableLiveData<List<CommonPrep>>()

    /** The common prep to be presented to the user */
    val commonPrep: LiveData<List<CommonPrep>>
        get() = _commonPrep

    /** Internal version of all of the photo shoot locations to be presented to the user */
    private var _photoShootLocations = MutableLiveData<List<PhotoShootLocation>>()

    /** The photo shoot location data to be presented to the user */
    val photoShootLocations: LiveData<List<PhotoShootLocation>>
        get() = _photoShootLocations

    /** Internal version of the photo shoot location data to be presented to the user */
    private var _selectedPhotoShootLocation = MutableLiveData<PhotoShootLocation>()

    /** The photo shoot location data to be presented to the user*/
    val selectedPhotoShootLocation: LiveData<PhotoShootLocation>
        get() = _selectedPhotoShootLocation

    // Used for logging any error messages
    private val className = "PhotoPrepRepository"
    private val databaseExceptionFormat = "Error when saving new data to the database: %s"
    private val databaseExceptionRemovingDataFormat =
        "Error when deleting data from the database: %s"
    private val weatherAPIError = "Unable to parse JSON response Object from Weather API"

    /** Removes the common prep item with the matching [id] */
    suspend fun removeCommonPrepData(id: Long) {
        withContext(Dispatchers.IO) {
            try {
                database.photoPrepDao.removeCommonPrepItem(id)
                getAllCommonPrep()
            } catch (e: Exception) {
                Log.e(
                    className,
                    String.format(
                        databaseExceptionRemovingDataFormat,
                        e.message.toString()
                    )
                )
            }
        }
    }

    /** Remove photo shoot prep that is associated with the specified photo shoot
     * location ([photoShootId]) and has the specified id([prepId]) */
    suspend fun removePhotoShootPrepData(prepId: Long, photoShootId: Long) {
        withContext(Dispatchers.IO) {
            try {
                database.photoPrepDao.removePhotoShootPrepItem(prepId, photoShootId)
                getAllCommonPrep()
            } catch (e: Exception) {
                Log.e(
                    className,
                    String.format(
                        databaseExceptionRemovingDataFormat,
                        e.message.toString()
                    )
                )
            }
        }
    }

    /** Retrieve all common prep data from the repository */
    suspend fun getAllCommonPrep() {
        withContext(Dispatchers.IO) {
            // Using postValue to update the live data on a background thread instead of setting
            // the live data value as described in https://stackoverflow.com/a/60126585
            _commonPrep.postValue(database.photoPrepDao.getAllCommonPrep().asDomainModel())
        }
    }

    /** Store new common prep items */
    suspend fun insertCommonPrep(commonPrep: List<CommonPrep>) {
        withContext(Dispatchers.IO) {
            database.photoPrepDao.insertCommonPrep(*commonPrep.asDatabaseModel())
            getAllCommonPrep()
        }
    }

    /** Store new photo shoot prep items */
    suspend fun insertPhotoShootPrep(photoShootPrep: List<PhotoShootPrep>) {
        withContext(Dispatchers.IO) {
            database.photoPrepDao.insertPhotoShootPrep(*photoShootPrep.asDatabaseModel())
            getAllPhotoShootLocations()
        }
    }

    /** Retrieve all photo shoot location data from the repository */
    suspend fun getAllPhotoShootLocations() {
        withContext(Dispatchers.IO) {
            _photoShootLocations.postValue(
                database.photoPrepDao.getPhotoShootLocations().asDomainModel()
            )
        }
    }

    /** Store overview information for a photo shoot location */
    suspend fun insertPhotoShootOverview(photoShootOverview: List<PhotoShootOverview>) {
        withContext(Dispatchers.IO) {
            database.photoPrepDao.insertPhotoShootOverview(*photoShootOverview.asDatabaseModel())
            getAllPhotoShootLocations()
        }
    }

    /** Update a photo shoot overview */
    suspend fun updatePhotoShootOverview(photoShootOverview: List<PhotoShootOverview>) {
        withContext(Dispatchers.IO) {
            database.photoPrepDao.updatePhotoShootOverview(*photoShootOverview.asDatabaseModel())
            getAllPhotoShootLocations()
        }
    }

    /** Delete a photo shoot overview */
    suspend fun deletePhotoShootOverview(photoShootLocationId: Long) {
        withContext(Dispatchers.IO) {
            database.photoPrepDao.deletePhotoShootOverview(photoShootLocationId)
            getAllPhotoShootLocations()
        }
    }

    /** Update photo shoot prep items */
    suspend fun updatePhotoShootPrep(photoShootPrep: List<PhotoShootPrep>) {
        withContext(Dispatchers.IO) {
            database.photoPrepDao.updatePhotoShootPrep(*photoShootPrep.asDatabaseModel())
            getAllPhotoShootLocations()
        }
    }

    /** Update the selected photo shoot location to the location with the matching
     * [photoShootLocationId] */
    suspend fun updateSelectedPhotoShootLocation(photoShootLocationId: Long) {
        withContext(Dispatchers.IO) {
            _selectedPhotoShootLocation.postValue(
                database.photoPrepDao.getPhotoShootLocation(photoShootLocationId).asDomainModel()
            )
        }
    }

    /** Retrieve the latest weather forecast data from the OpenWeather api and store it in
     *  the database */
    suspend fun refreshWeatherForecastData(
        apiToken: String,
        photoShootId: Long,
        latLng: LatLng,
        desiredDay: Int,
        preferredUnits: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Call the api to get the weather forecast data
                val response = WeatherDataApi.retrofitService.getWeatherForecast(
                    lat = latLng.latitude,
                    lon = latLng.longitude,
                    apiKey = apiToken,
                    unit = preferredUnits
                )

                // Parse the response to generate a weather forecast object that
                // represents the desired day
                val jsonObject = JSONObject(response)
                val forecast = parseWeatherForecastJsonResult(
                    jsonResult = jsonObject,
                    photoShootId = photoShootId,
                    desiredDay = desiredDay,
                    desiredDayMillis = _selectedPhotoShootLocation.value!!.photoShootOverview.nextPhotoShootDate,
                    locationLat = _selectedPhotoShootLocation.value!!.photoShootOverview.locationLat,
                    locationLong = _selectedPhotoShootLocation.value!!.photoShootOverview.locationLng,
                    preferredUnits = preferredUnits
                )

                // Store the weather forecast
                withContext(Dispatchers.IO) {
                    if (forecast != null) {
                        database.photoPrepDao.insertWeatherForecast(forecast.asDatabaseModel())
                        getAllPhotoShootLocations()
                    } else {
                        throw java.lang.Exception(weatherAPIError)
                    }
                }

            } catch (e: Exception) {
                Log.e(
                    className,
                    String.format(
                        databaseExceptionFormat,
                        e.message.toString()
                    )
                )
            }
        }
    }

    /** Remove the weather forecast data for the photo shot location with the id that
     * matches the provided [photoShootId] */
    suspend fun clearWeatherForecastData(photoShootId: Long) {
        withContext(Dispatchers.IO) {
            database.photoPrepDao.deleteWeatherForecast(photoShootId)
            getAllPhotoShootLocations()
        }
    }
}