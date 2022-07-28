package com.vcdaniel.photoprep.ui.photoShootLocation

import android.app.Application
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.google.android.gms.maps.model.LatLng
import com.vcdaniel.photoprep.*
import com.vcdaniel.photoprep.database.getDatabase
import com.vcdaniel.photoprep.repository.PhotoPrepRepository
import kotlinx.coroutines.launch
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

/** The options for the status of the prep filter */
enum class PrepFilter { ALL, FILTERED, NOT_SET }

/** The options for if the forecast is retrievable */
enum class ForecastRetrievableStatus {
    RETRIEVABLE, NOT_SET, TO_FAR_AHEAD, IN_PAST
}

/** The view model for the PhotoShootLocationFragment. Facilitates viewing the details of a photo
 * shoot location as well as editing the status of the prep for a photo shoot location */
class PhotoShootLocationViewModel(
    selectedPhotoShootLocationID: Long,
    var weatherPreferences: WeatherPreferences,
    app: Application
) :
    AndroidViewModel(app) {

    /** The id of the selected photo shoot location */
    private var photoShootLocationId: Long = 0

    /** Prep items that the user wants to add from the common prep library */
    var newPrepItems = ArrayList<PhotoShootPrep>()

    /** Indicates that a change to the next photo shoot needs to be saved */
    val savePhotoShootNextPhotoShoot = MutableLiveData<Boolean>(false)

    /** Indicates that a change to the photo shoot prep needs to be saved */
    val savePhotoShootPrep = MutableLiveData<Boolean>(false)

    /** The currently selected prep filter */
    private var _prepFilter = MutableLiveData<PrepFilter>(PrepFilter.NOT_SET)
    val prepFilter: LiveData<PrepFilter>
        get() = _prepFilter

    // Get the database and repository to interact with the photo prep data
    private val database = getDatabase(app)
    private val photoPrepRepository = PhotoPrepRepository(database)

    /** Indicates that something about the photo shoot location has changed */
    val photoShootDataUpdated = MutableLiveData<Boolean>(false)

    /** Indicates that the weather forecast data has changed */
    val weatherDataUpdated = MutableLiveData<Boolean>(false)

    /** All the prep that should be shown. This takes into account the current filter status */
    val visiblePrepItems = MutableLiveData<ArrayList<PhotoShootPrep>>()

    /** All the common prep */
    val commonPrep = photoPrepRepository.commonPrep

    /** The photo shoot location that the user has selected to view the details of */
    val photoShootLocation = photoPrepRepository.selectedPhotoShootLocation

    /** The conditions that are currently applicable to the photo shoot location prep. This
     * includes both weather related prep as well as prep related to the next photo shoot */
    var activeConditions = HashSet<ConditionType>()

    /** The api key for interacting with the OpenWeather api */
    private val weatherAPIKey = app.getString(R.string.WEATHER_API_KEY)

    /** Update the active conditions and which prep items should be displayed */
    private val photoShootLocationObserver = Observer<PhotoShootLocation> {
        if (it != null) {
            updateConditions()
            updateVisiblePrepItems()
        }
    }

    /** Re-evaluate which conditions are relevant */
    private fun updateConditions() {
        // Remove any previously added conditions
        activeConditions.clear()

        // Add conditions related to the subject(s) of the next photo shoot
        if (photoShootLocation.value?.photoShootOverview?.nextPhotoShootHasGroup == true) {
            activeConditions.add(ConditionType.GROUP)
        }

        if (photoShootLocation.value?.photoShootOverview?.nextPhotoShootHasChild == true) {
            activeConditions.add(ConditionType.CHILD)
        }

        if (photoShootLocation.value?.photoShootOverview?.nextPhotoShootHasPet == true) {
            activeConditions.add(ConditionType.PET)
        }

        // Add weather related conditions by comparing the current value in the forecast to the
        // corresponding threshold value the user has set
        photoShootLocation.value?.weatherForecast?.let { weatherForecast ->
            if (weatherForecast.precipitationPercentage > weatherPreferences.precipitationPercentageThreshold) {
                activeConditions.add(ConditionType.RAINY)
            }

            if (weatherForecast.cloudiness > weatherPreferences.cloudinessThreshold) {
                activeConditions.add(ConditionType.CLOUDY)
            }

            if (weatherForecast.windSpeed > weatherPreferences.windSpeedThreshold) {
                activeConditions.add(ConditionType.WINDY)
            }

            if (weatherForecast.maxTemp > weatherPreferences.hotThresholdTemperature) {
                activeConditions.add(ConditionType.HOT)
            }

            if (weatherForecast.minTemp < weatherPreferences.coldThresholdTemperature) {
                activeConditions.add(ConditionType.COLD)
            }
        }
    }

    /** Display the photo shoot location's prep. This takes into account the status of the current
     * prep filter */
    private fun updateVisiblePrepItems() {
        if (_prepFilter.value == PrepFilter.ALL) {
            // If the filter is set to all display all the prep
            visiblePrepItems.postValue(photoShootLocation.value?.prep)
        } else {
            // If the prep should be filtered then only display prep that either has no conditions
            // or has at least on condition that is currently relevant
            val prepToShow = ArrayList<PhotoShootPrep>()
            photoShootLocation.value?.prep?.forEach {
                if (it.conditions.isEmpty()) {
                    prepToShow.add(it)
                } else {
                    if (it.conditions.containsAnyCondition(activeConditions)) {
                        prepToShow.add(it)
                    }
                }
            }

            visiblePrepItems.postValue(prepToShow)
        }
    }

    /** Set the prep filter to the supplied [filter] value and update the prep if [updatePrep]
     * is set to true */
    fun setFilter(filter: PrepFilter, updatePrep: Boolean = true) {
        _prepFilter.value = filter

        if (updatePrep) {
            updateVisiblePrepItems()
        }
    }

    /** Provide the photo shoot locations coordinates as a latLng object */
    fun locationLatLng(): LatLng {
        return LatLng(
            photoShootLocation.value!!.photoShootOverview.locationLat,
            photoShootLocation.value!!.photoShootOverview.locationLng
        )
    }

    init {

        // Get the latest photo shoot location data for the selected photo shoot location
        photoShootLocationId = selectedPhotoShootLocationID
        retrieveLatestPhotoShootLocationData(true)

        photoShootLocation.observeForever(photoShootLocationObserver)

        // Get the latest common prep
        viewModelScope.launch {
            photoPrepRepository.getAllCommonPrep()
        }
    }

    /** Create a view model and pass in the [selectedPhotoShootLocationID] and [weatherPreferences].
     * This is in part based on the logic in the answer at https://stackoverflow.com/a/46704702 */
    class PhotoShootLocationViewModelFactory(
        private val selectedPhotoShootLocationID: Long,
        private val weatherPreferences: WeatherPreferences,
        private val app: Application
    ) :
        ViewModelProvider.Factory {
        override fun <PhotoShootLocationViewModel : ViewModel> create(modelClass: Class<PhotoShootLocationViewModel>): PhotoShootLocationViewModel {
            return PhotoShootLocationViewModel(
                selectedPhotoShootLocationID,
                weatherPreferences,
                app
            ) as PhotoShootLocationViewModel
        }
    }

    override fun onCleared() {
        super.onCleared()
        photoShootLocation.removeObserver(photoShootLocationObserver)
    }

    /** Save any changes made to the photo shoot location overview information */
    fun savePhotoShootOverviewData() {
        viewModelScope.launch {
            photoShootLocation.value?.photoShootOverview?.let {
                photoPrepRepository.updatePhotoShootOverview(listOf(it))
            }
            savePhotoShootNextPhotoShoot.value = false
            updatePhotoShootPrepItem()
        }
    }

    /** Save any changes made to the photo shoot location's prep */
    fun savePhotoShootPrepData() {
        viewModelScope.launch {
            photoShootLocation.value?.prep?.let {
                photoPrepRepository.insertPhotoShootPrep(newPrepItems)
                newPrepItems.clear()
                retrieveLatestPhotoShootLocationData()
            }
            savePhotoShootPrep.value = false
        }
    }

    /** Get the latest photo shoot location data and if specified by [notifyWhenComplete] notify
     * that the data has been updated */
    fun retrieveLatestPhotoShootLocationData(notifyWhenComplete: Boolean = false) {
        viewModelScope.launch {
            photoPrepRepository.updateSelectedPhotoShootLocation(
                photoShootLocationId
            )

            if (notifyWhenComplete) {
                photoShootDataUpdated.value = true
            }
        }
    }

    /** Update the photo shoot location's prep items */
    fun updatePhotoShootPrepItem() {
        viewModelScope.launch {
            photoShootLocation.value?.prep?.let {
                photoPrepRepository.updatePhotoShootPrep(newPrepItems)
                newPrepItems.clear()
                retrieveLatestPhotoShootLocationData()
            }
            savePhotoShootPrep.value = false
        }
    }

    /** Delete the prep with the corresponding [prepId] in the photo shoot location with the
     * matching [photoShootId] */
    fun deletePrepItem(prepId: Long, photoShootId: Long) {
        viewModelScope.launch {
            photoPrepRepository.removePhotoShootPrepData(prepId, photoShootId)
            retrieveLatestPhotoShootLocationData()
        }
    }

    /** Change the status of all prep items to not completed. This includes prep that is not
     * currently displayed */
    fun unCheckAllPrepItems() {
        viewModelScope.launch {
            photoShootLocation.value?.prep?.forEach {
                val editedPrep = PhotoShootPrep(
                    prepName = it.prepName,
                    conditions = it.conditions,
                    completed = false,
                    id = it.id,
                    photoShootId = it.photoShootId
                )
                newPrepItems.add(editedPrep)
            }

            updatePhotoShootPrepItem()
        }
    }

    /** Determine if the weather forecast for the next photo shoot can be retrieved. For example if
     * the next photo shoot date is in the past */
    fun isForecastRetrievable(): ForecastRetrievableStatus {
        var forecastRetrievableStatus: ForecastRetrievableStatus = ForecastRetrievableStatus.NOT_SET
        // check if the next photo shoot date is not set or beyond the 7 days of available forecast
        photoShootLocation.value?.photoShootOverview?.nextPhotoShootDate?.let { nextPhotoShootDate ->
            forecastRetrievableStatus = when {
                nextPhotoShootDate == 0L -> {
                    // date not set
                    ForecastRetrievableStatus.NOT_SET
                }
                dayUntilNextPhotoShoot(nextPhotoShootDate) < 0 -> {
                    // date in the past
                    ForecastRetrievableStatus.IN_PAST
                }
                dayUntilNextPhotoShoot(nextPhotoShootDate) > 7 -> {
                    // date to far in the future
                    ForecastRetrievableStatus.TO_FAR_AHEAD
                }
                else -> {
                    ForecastRetrievableStatus.RETRIEVABLE
                }
            }
        }

        return forecastRetrievableStatus
    }

    /** Retrieve the latest forecast for the next photo shoot. If previously retrieved forecast data
     *  is still valid then no new data is retrieved unless [forceUpdate] is set to true. */
    fun retrieveForecastData(forceUpdate: Boolean = false) {
        viewModelScope.launch {
            if (forceUpdate || !isWeatherDataCurrent()) {
                photoShootLocation.value?.photoShootOverview?.let { photoShootOverview ->
                    photoPrepRepository.refreshWeatherForecastData(
                        apiToken = weatherAPIKey,
                        photoShootId = photoShootOverview.photoShootLocationId,
                        latLng = locationLatLng(),
                        desiredDay = dayUntilNextPhotoShoot(photoShootOverview.nextPhotoShootDate).toInt(),
                        weatherPreferences.preferredUnits
                    )
                }
                retrieveLatestPhotoShootLocationData(true)
                weatherDataUpdated.value = true
            }
        }
    }

    /** Determine if the weather forecast data is current. This checks if the data was retrieved
     * recently enough and for the expected date, location, units, etc.  */
    fun isWeatherDataCurrent(): Boolean {
        var isForecastValid = false
        var isForecastOutdated = false

        // Only get a new weather forecast after 6 hours have passed since last retrieving it if
        // the next photo shoot date is later then tomorrow
        val minutesBetweenDistantDateUpdates = 60 * 6

        // Only get a new weather forecast after 2 hours have passed since last retrieving it if
        // the next photo shoot date is today or tomorrow
        val minutesBetweenNearDateUpdates = 60 * 2

        photoShootLocation.value?.photoShootOverview?.nextPhotoShootDate.let { nextPhotoShootDate ->
            photoShootLocation.value?.weatherForecast?.let { weatherForecast ->
                // Check if the forecast data was retrieved for the expected location and in the
                // preferred units
                isForecastValid =
                    nextPhotoShootDate != null
                            && isOnSameDay(nextPhotoShootDate, weatherForecast.dateForForecast)
                            && weatherForecast.locationLat == photoShootLocation.value?.photoShootOverview?.locationLat
                            && weatherForecast.locationLong == photoShootLocation.value?.photoShootOverview?.locationLng
                            && weatherForecast.units == weatherPreferences.preferredUnits

                // If the next photo shoot is not today or tomorrow check if the weather data has
                // been updated in 6 hours retrieve new data
                nextPhotoShootDate?.let { nextPhotoShootDate ->
                    if (dayUntilNextPhotoShoot(nextPhotoShootDate) > 1L) {
                        if (minutesPassed(weatherForecast.timeDataRetrieved) > minutesBetweenDistantDateUpdates) {
                            isForecastOutdated = true
                        }
                    } else {
                        // if the next photo shoot is today check if the weather data has been updated in 2 hours
                        // if not update the data then display it
                        if (minutesPassed(weatherForecast.timeDataRetrieved) > minutesBetweenNearDateUpdates) {
                            isForecastOutdated = true
                        }
                    }
                }

                // If the weather forecast needs to be refreshed clear the previous data and
                // retrieve the latest forecast
                if (!isForecastValid || isForecastOutdated) {
                    viewModelScope.launch {
                        photoPrepRepository.clearWeatherForecastData(photoShootLocationId)
                        retrieveLatestPhotoShootLocationData(true)
                    }
                }
            }
        }

        return isForecastValid && !isForecastOutdated
    }

    /** Calculate the minutes that have passed since [timeDataRetrieved] This is in part based on
     * the following answers https://stackoverflow.com/a/6850919
     * https://stackoverflow.com/questions/7829571/milliseconds-to-days
     * https://stackoverflow.com/a/61730808 */
    private fun minutesPassed(timeDataRetrieved: Long): Long {
        val nowCalendar: Calendar = GregorianCalendar()

        val forecastRetrievedCalendar: Calendar = GregorianCalendar()
        forecastRetrievedCalendar.timeInMillis = timeDataRetrieved

        val timePassed = nowCalendar.timeInMillis - forecastRetrievedCalendar.timeInMillis

        return timePassed.milliseconds.inWholeMinutes
    }

    /** Calculate the number of dats until the [nextPhotoShootDate] This is in part based on the
     * following answers https://stackoverflow.com/a/6850919
     * https://stackoverflow.com/questions/7829571/milliseconds-to-days
     * https://stackoverflow.com/a/61730808*/
    private fun dayUntilNextPhotoShoot(nextPhotoShootDate: Long): Long {
        // Compare today's date to the the next photo shoot date

        // Get both of the days at an equivalent times of the day
        val todayCalendar: Calendar = GregorianCalendar()
        // reset hour, minutes, seconds and millis
        todayCalendar[Calendar.HOUR_OF_DAY] = 0
        todayCalendar[Calendar.MINUTE] = 0
        todayCalendar[Calendar.SECOND] = 0
        todayCalendar[Calendar.MILLISECOND] = 0

        val photoShootCalendar: Calendar = GregorianCalendar()
        photoShootCalendar.timeInMillis = nextPhotoShootDate
        // reset hour, minutes, seconds and millis
        photoShootCalendar[Calendar.HOUR_OF_DAY] = 0
        photoShootCalendar[Calendar.MINUTE] = 0
        photoShootCalendar[Calendar.SECOND] = 0
        photoShootCalendar[Calendar.MILLISECOND] = 0

        // Now that the two dates are at equivalent times calculate the number of days between them
        val timeBetweenDates = photoShootCalendar.timeInMillis - todayCalendar.timeInMillis
        return if (timeBetweenDates < 0) {
            -1
        } else {
            return timeBetweenDates.milliseconds.inWholeDays
        }
    }

    /** Determine if the [firstDate] and [secondDate] are on the same day regardless of the time
     * of day. This is based on the tutorial at https://www.baeldung.com/java-check-two-dates-on-same-day */
    private fun isOnSameDay(firstDate: Long, secondDate: Long): Boolean {
        val firstCalendar = Calendar.getInstance()
        firstCalendar.timeInMillis = firstDate

        val secondCalendar = Calendar.getInstance()
        secondCalendar.timeInMillis = secondDate

        return (firstCalendar.get(Calendar.YEAR) == secondCalendar.get(Calendar.YEAR)
                && firstCalendar.get(Calendar.MONTH) == secondCalendar.get(Calendar.MONTH)
                && firstCalendar.get(Calendar.DAY_OF_MONTH) == secondCalendar.get(Calendar.DAY_OF_MONTH))
    }

}

/** Returns if their is any overlap between what conditions are in the two HashSets  */
private fun <ConditionType> HashSet<ConditionType>.containsAnyCondition(activeConditions: HashSet<ConditionType>): Boolean {
    var containsAnyCondition = false
    when {
        activeConditions.isEmpty() -> {
            // If there aren't any active conditions then no prep with conditions should be added
            containsAnyCondition = false
        }
        activeConditions.size <= this.size -> {
            // Iterate through whichever collection is smaller
            activeConditions.forEach { conditionToCheck ->
                // If any condition is found there's no need to continue checking
                // for overlapping conditions
                if (!containsAnyCondition) {
                    containsAnyCondition = this.contains(conditionToCheck)
                }
            }
        }
        else -> {
            this.forEach { conditionToCheck ->
                if (!containsAnyCondition) {
                    containsAnyCondition = activeConditions.contains(conditionToCheck)
                }
            }
        }
    }

    return containsAnyCondition
}