package com.vcdaniel.photoprep

import android.content.Context
import android.content.SharedPreferences

// These enum classes define the options of what should trigger prep with conditions to be shown.
// For example if the the CAUTIOUS option is chosen by the user for the ImperialHotThresholdValues
// then the hot condition is considered to be met if the temperature is above
// 70 degrees Fahrenheit. The values are equivalent imperial and metric units which are dynamically
// used depending on the user's prefered units

enum class ImperialHotThresholdValues(val temperature: Double) {
    CAUTIOUS(70.0), STANDARD(80.0), RISKY(95.0)
}

private enum class ImperialColdThresholdValues(val temperature: Double) {
    RISKY(40.0), STANDARD(60.0), CAUTIOUS(70.0)
}

private enum class ImperialWindSpeedThresholdValues(val speed: Double) {
    CAUTIOUS(10.0), STANDARD(15.0), RISKY(25.0)
}

private enum class MetricHotThresholdValues(val temperature: Double) {
    CAUTIOUS(21.0), STANDARD(27.0), RISKY(35.0)
}

private enum class MetricColdThresholdValues(val temperature: Double) {
    RISKY(5.0), STANDARD(15.0), CAUTIOUS(21.0)
}

private enum class MetricWindSpeedThresholdValues(val speed: Double) {
    CAUTIOUS(4.0), STANDARD(7.0), RISKY(11.0)
}

private enum class PrecipitationPercentageThresholdValues(val percentage: Double) {
    CAUTIOUS(.10), STANDARD(.30), RISKY(.60)
}

private enum class CloudinessPercentageThresholdValues(val percentage: Double) {
    CAUTIOUS(10.0), STANDARD(30.0), RISKY(60.0)
}

/** Provide the values that should trigger weather related conditions and the user's preferred units.
 * This abstracts dealing with the specifics of retrieving the users preferences. */
class WeatherPreferences(
    sharedPreferences: SharedPreferences,
    val context: Context
) {
    /** Returns if the user has set Imperial units as their preferred units */
    fun prefersImperial(): Boolean {
        return preferredUnits == context.getString(R.string.weather_unit_imperial)
    }

    // The values that should trigger the weather related conditions
    var hotThresholdTemperature: Double = 0.0
    var coldThresholdTemperature: Double = 0.0
    var precipitationPercentageThreshold: Double = 0.0
    var windSpeedThreshold: Double = 0.0
    var cloudinessThreshold: Double = 0.0
    var preferredUnits = String()

    init {

        // Retrieve the index of the chosen item for each of the weather preferences
        val hotPrefIndex =
            Integer.parseInt(
                sharedPreferences.getString(
                    context.getString(R.string.hot_preference_key),
                    context.getString(R.string.standard_weather_option)
                )!!
            )
        val coldPrefIndex = Integer.parseInt(
            sharedPreferences.getString(
                context.getString(R.string.cold_preference_key),
                context.getString(R.string.standard_weather_option)
            )!!
        )
        val precipitationPrefIndex = Integer.parseInt(
            sharedPreferences.getString(
                context.getString(R.string.rain_preference_key),
                context.getString(R.string.standard_weather_option)
            )!!
        )
        val windPrefIndex = Integer.parseInt(
            sharedPreferences.getString(
                context.getString(R.string.wind_preference_key),
                context.getString(R.string.standard_weather_option)
            )!!
        )
        val cloudPrefIndex = Integer.parseInt(
            sharedPreferences.getString(
                context.getString(R.string.cloud_preference_key),
                context.getString(R.string.standard_weather_option)
            )!!
        )

        // Store the user's preferred precipitation percentage and and cloudiness threshold values
        precipitationPercentageThreshold =
            PrecipitationPercentageThresholdValues.values()[precipitationPrefIndex].percentage
        cloudinessThreshold =
            CloudinessPercentageThresholdValues.values()[cloudPrefIndex].percentage

        // Retrieve the user's preferred units
        preferredUnits =
            sharedPreferences.getString(
                context.getString(R.string.weather_units_preference_key),
                context.getString(R.string.weather_unit_imperial)
            ).toString()

        // Store the appropriate threshold values depending on the user's selection and
        // their preferred units
        if (prefersImperial()) {
            hotThresholdTemperature =
                ImperialHotThresholdValues.values()[hotPrefIndex].temperature
            coldThresholdTemperature =
                ImperialColdThresholdValues.values()[coldPrefIndex].temperature
            windSpeedThreshold = ImperialWindSpeedThresholdValues.values()[windPrefIndex].speed
        } else {
            hotThresholdTemperature =
                MetricHotThresholdValues.values()[hotPrefIndex].temperature
            coldThresholdTemperature =
                MetricColdThresholdValues.values()[coldPrefIndex].temperature
            windSpeedThreshold = MetricWindSpeedThresholdValues.values()[windPrefIndex].speed
        }
    }
}