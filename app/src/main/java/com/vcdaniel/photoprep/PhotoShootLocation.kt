package com.vcdaniel.photoprep

import android.os.Parcelable
import com.vcdaniel.photoprep.database.DatabasePhotoShootLocation
import kotlinx.android.parcel.Parcelize

/** A place that the user takes pictures at. This includes information about the next time they
 * are planning to take pictures there and when available the weather on that day as well as a list
 * of items they should bring or tasks they need to complete before taking pictures at this location. */
@Parcelize
data class PhotoShootLocation(
    var photoShootOverview: PhotoShootOverview,
    var weatherForecast: WeatherForecast? = null,
    var prep: ArrayList<PhotoShootPrep>
) : Parcelable

/** Convert from a collection of app related objects to objects for use with a database*/
fun List<PhotoShootLocation>.asDatabaseModel(): Array<DatabasePhotoShootLocation> {
    return map {
        // If there is a weather forecast available store it in the database
        if (it.weatherForecast != null) {
            DatabasePhotoShootLocation(
                photoShootOverview = it.photoShootOverview.asDatabaseModel(),
                weatherForecast = it.weatherForecast!!.asDatabaseModel(),
                prep = it.prep.asDatabaseModel().toList()
            )
        } else {
            DatabasePhotoShootLocation(
                photoShootOverview = it.photoShootOverview.asDatabaseModel(),
                prep = it.prep.asDatabaseModel().toList()
            )
        }

    }.toTypedArray()
}
