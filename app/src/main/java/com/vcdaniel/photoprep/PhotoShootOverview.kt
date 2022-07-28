package com.vcdaniel.photoprep

import android.net.Uri
import android.os.Parcelable
import com.vcdaniel.photoprep.database.DatabasePhotoShootOverview
import kotlinx.android.parcel.Parcelize

/** Summary information of a place that the user takes pictures at. This includes information such
 * as the location, name and information about the next time they are planning to take pictures at
 * the location such as the date and the subjects they will be photographing. */
@Parcelize
class PhotoShootOverview(
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
) : Parcelable {

    /** Determine the address to a static Google Maps image. */
    fun getStaticMapUrl(apiKey:String): String {
        var staticMapUrl: String

        // This is based on the tutorial at https://developers.google.com/maps/documentation/maps-static/start
        val scheme = "https"
        val authority = "maps.googleapis.com"
        val service = "maps"
        val serviceType = "api"
        val serviceName = "staticmap"
        val zoomKey = "zoom"
        val zoomValue = "16"
        val sizeKey = "size"
        val sizeValue = "400x350"
        val scaleKey = "scale"
        val scaleValue = "2"
        val locationKey = "markers"
        val locationMarker = String.format("|%f,%f", locationLat, locationLng)
        val apiParameterKey = "key"

        val builder = Uri.Builder()
        builder.scheme(scheme)
            .authority(authority)
            .appendPath(service)
            .appendPath(serviceType)
            .appendPath(serviceName)
            .appendQueryParameter(zoomKey, zoomValue)
            .appendQueryParameter(sizeKey, sizeValue)
            .appendQueryParameter(scaleKey, scaleValue)
            .appendQueryParameter(locationKey, locationMarker)
            .appendQueryParameter(apiParameterKey, apiKey)
        staticMapUrl = builder.build().toString()

        // Format the url using the a comma instead of the converted value for comma. This is based
        // on the answer at https://stackoverflow.com/q/19167954
        staticMapUrl = staticMapUrl.replace("%2C", ",")
        return staticMapUrl
    }
}

/** Convert from a collection of app related objects to objects for use with a database*/
fun List<PhotoShootOverview>.asDatabaseModel(): Array<DatabasePhotoShootOverview> {
    return map {
        DatabasePhotoShootOverview(
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
    }.toTypedArray()
}

/** Convert from a collection of app related objects to objects for use with a database*/
fun PhotoShootOverview.asDatabaseModel(): DatabasePhotoShootOverview {
    return DatabasePhotoShootOverview(
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