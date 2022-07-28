package com.vcdaniel.photoprep.api

import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import com.vcdaniel.photoprep.database.DatabaseWeatherForecast

/** This file contains objects to facilitate interesting with the OpenWeather api as well as
 * converting them to objects that can be used to store them in the database. */

/** A collection of NetworkForecast objects*/
@JsonClass(generateAdapter = true)
data class NetworkForecastContainer(val weatherForecasts: List<NetworkWeatherForecast>)

/** The information on the weather forecast for the specified location on the date of the next
 * photo shoot is retrieved from the OpenWeather api. This is based on the logic found at
 * https://github.com/VC-Daniel/Asteroid-Tracker-App */
@JsonClass(generateAdapter = true)
data class NetworkWeatherForecast constructor(
    @PrimaryKey
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
)

/** Convert from a collection of network related weather forecast objects to objects for use with
 * a database*/
fun NetworkForecastContainer.asDatabaseModel(): Array<DatabaseWeatherForecast> {
    return weatherForecasts.map {
        DatabaseWeatherForecast(
            photoShootId = it.photoShootId,
            timeDataRetrieved = it.timeDataRetrieved,
            locationLat = it.locationLat,
            locationLong = it.locationLong,
            units = it.units,
            dateForForecast = it.dateForForecast,
            minTemp = it.minTemp,
            maxTemp = it.maxTemp,
            windSpeed = it.windSpeed,
            precipitationPercentage = it.precipitationPercentage,
            cloudiness = it.cloudiness,
            weatherId = it.weatherId,
            weatherIcon = it.weatherIcon,
            description = it.description
        )
    }.toTypedArray()
}

/** Convert from a single network related weather forecast objects to objects for use
 * with a database*/
fun NetworkWeatherForecast.asDatabaseModel(): DatabaseWeatherForecast {
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