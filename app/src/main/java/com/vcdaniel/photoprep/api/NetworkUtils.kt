package com.vcdaniel.photoprep.api

import org.json.JSONObject
import java.util.*

/** Parse weather forecast data from the [jsonResult] of an api call. This is based on the logic
 * found at https://github.com/VC-Daniel/Asteroid-Tracker-App*/
fun parseWeatherForecastJsonResult(
    jsonResult: JSONObject,
    photoShootId: Long,
    desiredDay: Int,
    desiredDayMillis: Long,
    locationLat: Double,
    locationLong: Double,
    preferredUnits: String
): NetworkWeatherForecast? {
    val dailyForecastKey = "daily"
    val temperatureKey = "temp"
    val minTemperatureKey = "min"
    val maxTemperatureKey = "max"
    val windSpeedKey = "wind_speed"
    val precipitationPercentageKey = "pop"
    val cloudinessKey = "clouds"
    val overviewKey = "weather"
    val overviewIDKey = "id"
    val overviewIcon = "icon"
    val overViewDescriptionKey = "description"

    // Store the current time to denote when the weather forecast was retrieved
    val calendar = Calendar.getInstance()
    val timeDataRetrieved = calendar.timeInMillis

    var networkWeatherForecast: NetworkWeatherForecast? = null

    if (jsonResult.has(dailyForecastKey)) {
        val weatherForecastsJsonArray = jsonResult.getJSONArray(dailyForecastKey)

        if (weatherForecastsJsonArray.length() >= desiredDay && desiredDay >= 0) {
            // default to the id and icon for a "few clouds: 11-25%" if no weather was provided
            var weatherId = 801
            var weatherIcon = "02d.png"
            var weatherDescription = ""

            val forecastJson = weatherForecastsJsonArray.getJSONObject(desiredDay)
            val temperatureData = forecastJson.getJSONObject(temperatureKey)
            val minTemp = temperatureData.getDouble(minTemperatureKey)
            val maxTemp = temperatureData.getDouble(maxTemperatureKey)
            val windSpeed = forecastJson.getDouble(windSpeedKey)
            val precipitationPercentage = forecastJson.getDouble(precipitationPercentageKey)
            val cloudiness = forecastJson.getInt(cloudinessKey)
            val weatherOverviewData = forecastJson.getJSONArray(overviewKey)
            if (weatherOverviewData.length() > 0) {
                weatherId = weatherOverviewData.getJSONObject(0).getInt(overviewIDKey)
                weatherIcon = weatherOverviewData.getJSONObject(0).getString(overviewIcon)
                weatherDescription =
                    weatherOverviewData.getJSONObject(0).getString(overViewDescriptionKey)
            }

            // Create a NetworkWeatherForecast object with the data retrieved from the api as well
            // as the provided information about the units and date of the forecast which are used
            // to determine if a new forecast should be retrieved
            networkWeatherForecast = NetworkWeatherForecast(
                photoShootId = photoShootId,
                timeDataRetrieved = timeDataRetrieved,
                locationLat = locationLat,
                locationLong = locationLong,
                units = preferredUnits,
                dateForForecast = desiredDayMillis,
                minTemp = minTemp,
                maxTemp = maxTemp,
                windSpeed = windSpeed,
                precipitationPercentage = precipitationPercentage,
                cloudiness = cloudiness,
                weatherId = weatherId,
                weatherIcon = weatherIcon,
                description = weatherDescription
            )
        }
    }

    return networkWeatherForecast
}