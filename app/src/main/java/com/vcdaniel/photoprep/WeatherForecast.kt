package com.vcdaniel.photoprep

import android.net.Uri
import android.os.Parcelable
import com.vcdaniel.photoprep.database.DatabaseWeatherForecast
import kotlinx.android.parcel.Parcelize

/** A summary of the weather at the specified location ([locationLat],[locationLong]) on the
 * specified [dateForForecast]. */
@Parcelize
class WeatherForecast(
    val photoShootId: Long,
    val timeDataRetrieved: Long,
    val locationLat: Double,
    val locationLong: Double,
    val units: String,
    val dateForForecast: Long,
    val minTemp: Double,
    val maxTemp: Double,
    val windSpeed: Double,
    val precipitationPercentage: Double,
    val cloudiness: Int,
    val weatherId: Int,
    val weatherIcon: String,
    val description: String
) : Parcelable {
    private val scheme = "https"
    private val authority = "openweathermap.org"
    private val iconType = "img"
    private val pathInfo = "wn"
    private val iconSizeInfo = "@4x.png"

    fun getWeatherIconPath(): String {
        // Retrieve the icon that represents the overall forecast
        // For example this url returns the icon for a rainy day
        // http://openweathermap.org/img/wn/10d@4x.png
        // The icon code and equivalent images are list at
        // https://openweathermap.org/weather-conditions#Weather-Condition-Codes-2

        val iconUrlName = weatherIcon + iconSizeInfo

        val builder = Uri.Builder()
        builder.scheme(scheme)
            .authority(authority)
            .appendPath(iconType)
            .appendPath(pathInfo)
            .appendPath(iconUrlName)
        return builder.build().toString()
    }
}

fun WeatherForecast.asDatabaseModel(): DatabaseWeatherForecast {
    return DatabaseWeatherForecast(
        photoShootId = this.photoShootId,
        timeDataRetrieved = this.timeDataRetrieved,
        locationLat = this.locationLat,
        locationLong = this.locationLong,
        units = this.units,
        dateForForecast = this.dateForForecast,
        minTemp = this.minTemp,
        maxTemp = this.maxTemp,
        windSpeed = this.windSpeed,
        precipitationPercentage = this.precipitationPercentage,
        cloudiness = this.cloudiness,
        weatherId = this.weatherId,
        weatherIcon = this.weatherIcon,
        description = this.description
    )
}