package com.vcdaniel.photoprep

import android.os.Parcelable
import com.vcdaniel.photoprep.database.DatabaseCommonPrep
import kotlinx.android.parcel.Parcelize

/** A common A task to complete before, or item to bring on, a photo shoot. The common prep can be
 * added to a photo shoot location. */
@Parcelize
data class CommonPrep(
    var prepName: String,
    var conditions: HashSet<ConditionType>,
    var completed: Boolean = false,
    var id: Long
) : Parcelable {

    /** Create a CommonPrep by individually specifying the desired state of each condition. */
    constructor(
        _prepName: String,
        _completed: Boolean = false,
        _id: Long,
        onlyWithGroup: Boolean,
        onlyWithChild: Boolean,
        onlyWithPet: Boolean,
        onlyWhenRainy: Boolean,
        onlyWhenCloudy: Boolean,
        onlyWhenWindy: Boolean,
        onlyWhenHot: Boolean,
        onlyWhenCold: Boolean,
    ) : this(_prepName, HashSet<ConditionType>(), _completed, _id) {

        // Store the conditions that should trigger the prep
        if (onlyWithGroup)
            conditions.add(ConditionType.GROUP)
        if (onlyWithChild)
            conditions.add(ConditionType.CHILD)
        if (onlyWithPet)
            conditions.add(ConditionType.PET)
        if (onlyWhenRainy)
            conditions.add(ConditionType.RAINY)
        if (onlyWhenCloudy)
            conditions.add(ConditionType.CLOUDY)
        if (onlyWhenWindy)
            conditions.add(ConditionType.WINDY)
        if (onlyWhenHot)
            conditions.add(ConditionType.HOT)
        if (onlyWhenCold)
            conditions.add(ConditionType.COLD)
    }

    constructor(
        prepData: Prep,
        _completed: Boolean = false,
        _id: Long
    ) : this(prepData.prepName, prepData.conditions, _completed, _id)
}

/** Convert from a collection of app related objects to objects for use with a database. */
fun List<CommonPrep>.asDatabaseModel(): Array<DatabaseCommonPrep> {
    return map {
        DatabaseCommonPrep(
            prepName = it.prepName,
            onlyWithGroup = it.conditions.containsCondition(ConditionType.GROUP),
            onlyWithChild = it.conditions.containsCondition(ConditionType.CHILD),
            onlyWithPet = it.conditions.containsCondition(ConditionType.PET),
            onlyWhenRainy = it.conditions.containsCondition(ConditionType.RAINY),
            onlyWhenCloudy = it.conditions.containsCondition(ConditionType.CLOUDY),
            onlyWhenWindy = it.conditions.containsCondition(ConditionType.WINDY),
            onlyWhenHot = it.conditions.containsCondition(ConditionType.HOT),
            onlyWhenCold = it.conditions.containsCondition(ConditionType.COLD),
            completed = it.completed,
            id = it.id
        )
    }.toTypedArray()
}

fun List<CommonPrep>.asListOfNames(): Array<String> {
    return map {
        it.prepName
    }.toTypedArray()
}

/** Convert a commonprep object to a PhotoShootPrep object such as in order to be used in a
 * photo shoot location */
fun CommonPrep.asPhotoShootPrep(photoShootId: Long): PhotoShootPrep {
    return PhotoShootPrep(
        prepName = this.prepName,
        conditions = this.conditions,
        photoShootId = photoShootId
    )
}