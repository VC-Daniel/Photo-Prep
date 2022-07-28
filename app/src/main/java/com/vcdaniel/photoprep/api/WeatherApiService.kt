package com.vcdaniel.photoprep.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Retrieve weather forecasts using the One Call api from OpenWeather which can be found at
 * https://openweathermap.org/api/one-call-3. This is based on the logic found at
 * https://github.com/VC-Daniel/Asteroid-Tracker-App
 */

const val BASE_URL = "https://api.openweathermap.org/"
const val CALL_TIMEOUT: Long = 2
const val CONNECTION_TIMEOUT: Long = 20
const val INTERACTION_TIMEOUT: Long = 30

const val API_ENDPOINT = "data/3.0/onecall"
const val LATITUDE_KEY = "lat"
const val LONGITUDE_KEY = "lon"
const val EXCLUDE_KEY = "exclude"
const val EXCLUDE_VALUE = "current,minutely,hourly,alert"
const val API_ID_KEY = "appid"
const val UNITS_KEY = "units"

/** Used to parse api data */
private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

/** Set the timeouts to support slower internet connections
This is based off of this tutorial https://howtodoinjava.com/retrofit2/connection-timeout-exception/ */
var httpClient: OkHttpClient = OkHttpClient.Builder()
    .callTimeout(CALL_TIMEOUT, TimeUnit.MINUTES)
    .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
    .readTimeout(INTERACTION_TIMEOUT, TimeUnit.SECONDS)
    .writeTimeout(INTERACTION_TIMEOUT, TimeUnit.SECONDS)
    .build()

/** Configured retrofit object to work with the One Call 3.0 OpenWeather api */
private val weatherDataRetrofit = Retrofit.Builder()
    .addConverterFactory(ScalarsConverterFactory.create())
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .client(httpClient)
    .build()

/** Provides calls to get weather forecast data */
interface WeatherDataApiService {
    /** Returns the weather forecasts for the today and the next 7 days for the specified location
     * using the OpenWeather api using the provided [apiKey] */
    @GET(API_ENDPOINT)
    suspend fun getWeatherForecast(
        @Query(LATITUDE_KEY) lat: Double,
        @Query(LONGITUDE_KEY) lon: Double,
        @Query(EXCLUDE_KEY) exclude: String = EXCLUDE_VALUE,
        @Query(API_ID_KEY) apiKey: String,
        @Query(UNITS_KEY) unit: String
    ): String
}


/** An object configured to interact with the One Call 3.0 OpenWeather api */
object WeatherDataApi {
    val retrofitService: WeatherDataApiService by lazy {
        weatherDataRetrofit.create(WeatherDataApiService::class.java)
    }
}