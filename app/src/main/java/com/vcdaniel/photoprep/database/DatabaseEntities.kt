package com.vcdaniel.photoprep.database

import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.vcdaniel.photoprep.*

/** This file contains classes that represent equivalent domain objects that are use to persist data
 *  via a Room database.  */

private const val photoShootLocationIdColumn = "photoShootLocationId"
private const val photoShootIdColumn = "photoShootId"

// Ensure that the common prep name is not repeated by setting it as unique. This is based on the
// tutorial at https://www.sqlitetutorial.net/sqlite-unique-constraint/
@Entity(
    indices = [Index(
        value = ["prepName"],
        unique = true
    )]
)
data class DatabaseCommonPrep(
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    var prepName: String,
    var onlyWithGroup: Boolean = false,
    var onlyWithChild: Boolean = false,
    var onlyWithPet: Boolean = false,
    var onlyWhenRainy: Boolean = false,
    var onlyWhenCloudy: Boolean = false,
    var onlyWhenWindy: Boolean = false,
    var onlyWhenHot: Boolean = false,
    var onlyWhenCold: Boolean = false,
    var completed: Boolean = false
)

/** Convert from a collection of database common prep objects to objects for use within
 * the context of the app*/
@JvmName("asDomainModelDatabasePrep")
fun List<DatabaseCommonPrep>.asDomainModel(): List<CommonPrep> {
    return map {
        CommonPrep(
            _prepName = it.prepName,
            _completed = it.completed,
            _id = it.id,
            onlyWithGroup = it.onlyWithGroup,
            onlyWithChild = it.onlyWithChild,
            onlyWithPet = it.onlyWithPet,
            onlyWhenRainy = it.onlyWhenRainy,
            onlyWhenCloudy = it.onlyWhenCloudy,
            onlyWhenWindy = it.onlyWhenWindy,
            onlyWhenHot = it.onlyWhenHot,
            onlyWhenCold = it.onlyWhenCold
        )
    }
}

@Entity
data class DatabasePhotoShootOverview(
    @PrimaryKey(autoGenerate = true)
    var photoShootLocationId: Long,
    var photoShootLocationName: String,
    var mainImagePath: String,
    var locationLng: Double,
    var locationLat: Double,
    var locationTitle: String,
    var nextPhotoShootHasGroup: Boolean,
    var nextPhotoShootHasChild: Boolean,
    var nextPhotoShootHasPet: Boolean,
    var nextPhotoShootDate: Long
)

/** Convert from a collection of app related objects to objects for use with a database */
@JvmName("asSingleDomainModelDatabasePhotoShootOverview")
fun DatabasePhotoShootOverview.asDomainModel(): PhotoShootOverview {
    return PhotoShootOverview(
        photoShootLocationId = this.photoShootLocationId,
        photoShootLocationName = this.photoShootLocationName,
        mainImagePath = this.mainImagePath,
        locationLng = this.locationLng,
        locationLat = this.locationLat,
        locationTitle = this.locationTitle,
        nextPhotoShootHasGroup = this.nextPhotoShootHasGroup,
        nextPhotoShootHasChild = this.nextPhotoShootHasChild,
        nextPhotoShootHasPet = this.nextPhotoShootHasPet,
        nextPhotoShootDate = this.nextPhotoShootDate
    )
}

/** The weather forecast for a particular photo shoot location. This will be automatically deleted
 * if the photo shoot location is deleted because onDelete is specified as cascade */
@Entity(
    foreignKeys = [ForeignKey(
        entity = DatabasePhotoShootOverview::class,
        parentColumns = [photoShootLocationIdColumn],
        childColumns = [photoShootIdColumn], onDelete = CASCADE
    )]
)
data class DatabaseWeatherForecast(
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

@Entity(
    indices = [Index(
        value = [photoShootIdColumn, "prepName"],
        unique = true
    )], foreignKeys = [ForeignKey(
        entity = DatabasePhotoShootOverview::class,
        parentColumns = [photoShootLocationIdColumn],
        childColumns = [photoShootIdColumn], onDelete = CASCADE
    )]
)
data class DatabasePhotoShootPrep(
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    val photoShootId: Long,
    var prepName: String,
    var onlyWithGroup: Boolean = false,
    var onlyWithChild: Boolean = false,
    var onlyWithPet: Boolean = false,
    var onlyWhenRainy: Boolean = false,
    var onlyWhenCloudy: Boolean = false,
    var onlyWhenWindy: Boolean = false,
    var onlyWhenHot: Boolean = false,
    var onlyWhenCold: Boolean = false,
    var completed: Boolean = false
)

/** Ties together all the information about a photo shoot location including overview information,
 * the weather forecast, and the prep. */
data class DatabasePhotoShootLocation(
    // This is in part based on the tutorials that use this app to demonstrate using relationships
    // between tables in Room https://github.com/philipplackner/MultipleRoomTables
    @Embedded val photoShootOverview: DatabasePhotoShootOverview,
    @Relation(
        parentColumn = photoShootLocationIdColumn,
        entityColumn = photoShootIdColumn
    )
    val weatherForecast: DatabaseWeatherForecast? = null,
    @Relation(
        parentColumn = photoShootLocationIdColumn,
        entityColumn = photoShootIdColumn, entity = DatabasePhotoShootPrep::class
    )
    val prep: List<DatabasePhotoShootPrep>
)

/** Convert from a collection of database photoShootLocation objects to objects for use within
 * the context of the app */
@JvmName("asDomainModelDatabasePhotoShootLocation")
fun List<DatabasePhotoShootLocation>.asDomainModel(): List<PhotoShootLocation> {
    return map {
        // If there is a weather forecast available store it in the database
        if (it.weatherForecast != null) {
            PhotoShootLocation(
                photoShootOverview = it.photoShootOverview.asDomainModel(),
                weatherForecast = it.weatherForecast!!.asDomainModel(),
                prep = it.prep.asDomainModel().toCollection(ArrayList())
            )
        } else {
            PhotoShootLocation(
                photoShootOverview = it.photoShootOverview.asDomainModel(),
                prep = it.prep.asDomainModel().toCollection(ArrayList())
            )
        }

    }
}

@JvmName("asSingleDomainModelDatabasePhotoShootLocation")
fun DatabasePhotoShootLocation.asDomainModel(): PhotoShootLocation {
    if (this.weatherForecast != null) {
        // If there is a weather forecast available store it in the database
        return PhotoShootLocation(
            photoShootOverview = photoShootOverview.asDomainModel(),
            weatherForecast = this.weatherForecast!!.asDomainModel(),
            // Convert the prep to an ArrayList using the logic found in the answer at
            // https://stackoverflow.com/a/40045644
            prep = prep.asDomainModel().toCollection(ArrayList())
        )
    } else {
        return PhotoShootLocation(
            photoShootOverview = photoShootOverview.asDomainModel(),
            prep = prep.asDomainModel().toCollection(ArrayList())
        )
    }
}

/** Convert from a collection of database photoShootOverview objects to objects for use within
 * the context of the app*/
@JvmName("asDomainModelDatabasePhotoShootOverview")
fun List<DatabasePhotoShootOverview>.asDomainModel(): List<PhotoShootOverview> {
    return map {
        PhotoShootOverview(
            photoShootLocationId = it.photoShootLocationId,
            photoShootLocationName = it.photoShootLocationName,
            mainImagePath = it.mainImagePath,
            locationLng = it.locationLng,
            locationLat = it.locationLat,
            locationTitle = it.locationTitle,
            nextPhotoShootHasGroup = it.nextPhotoShootHasGroup,
            nextPhotoShootHasChild = it.nextPhotoShootHasChild,
            nextPhotoShootHasPet = it.nextPhotoShootHasPet,
            nextPhotoShootDate = it.nextPhotoShootDate
        )
    }
}

/** Convert from a collection of database photoShootPrep objects to objects for use within
 * the context of the app*/
@JvmName("asDomainModelDatabasePhotoShootPrep")
fun List<DatabasePhotoShootPrep>.asDomainModel(): List<PhotoShootPrep> {
    return map {
        PhotoShootPrep(
            _prepName = it.prepName,
            _completed = it.completed,
            _id = it.id,
            _photoShootId = it.photoShootId,
            onlyWithGroup = it.onlyWithGroup,
            onlyWithChild = it.onlyWithChild,
            onlyWithPet = it.onlyWithPet,
            onlyWhenRainy = it.onlyWhenRainy,
            onlyWhenCloudy = it.onlyWhenCloudy,
            onlyWhenWindy = it.onlyWhenWindy,
            onlyWhenHot = it.onlyWhenHot,
            onlyWhenCold = it.onlyWhenCold,
        )
    }
}

@JvmName("asSingleDomainModelDatabaseWeatherForecast")
fun DatabaseWeatherForecast.asDomainModel(): WeatherForecast {
    return WeatherForecast(
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