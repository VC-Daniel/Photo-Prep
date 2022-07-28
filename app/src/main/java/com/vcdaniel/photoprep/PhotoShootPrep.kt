package com.vcdaniel.photoprep

import android.os.Parcelable
import com.vcdaniel.photoprep.database.DatabasePhotoShootPrep
import kotlinx.android.parcel.Parcelize

/** A task to complete before, or item to bring on, a photo shoot. The prep can have [conditions]
 * to denote in what scenario the prep is needed. The prep should be considered relevant if any
 * of the [conditions] are met. */
@Parcelize
data class PhotoShootPrep(
    var prepName: String,
    var conditions: HashSet<ConditionType>,
    var completed: Boolean = false,
    var id: Long = 0L,
    var photoShootId: Long
) : Parcelable {

    /** Create a PhotoShootPrep by individually specifying the desired state of each condition. */
    constructor(
        _prepName: String,
        _completed: Boolean = false,
        _id: Long,
        _photoShootId: Long,
        onlyWithGroup: Boolean,
        onlyWithChild: Boolean,
        onlyWithPet: Boolean,
        onlyWhenRainy: Boolean,
        onlyWhenCloudy: Boolean,
        onlyWhenWindy: Boolean,
        onlyWhenHot: Boolean,
        onlyWhenCold: Boolean,
    ) : this(_prepName, HashSet<ConditionType>(), _completed, _id, _photoShootId) {

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
        _id: Long,
        _photoShootId: Long
    ) : this(
        prepName = prepData.prepName,
        conditions = prepData.conditions,
        completed = _completed,
        id = _id,
        photoShootId = _photoShootId
    )

    /** Determine if two PhotoShootPreps are equal based on the name, id, completion status and
     * it's conditions*/
    override fun equals(other: Any?): Boolean {
        return if (other?.javaClass == PhotoShootPrep::class.java) {
            other as PhotoShootPrep
            (this.prepName == other.prepName
                    && this.conditions == other.conditions
                    && this.completed == other.completed
                    && this.id == other.id
                    && this.photoShootId == other.photoShootId)
        } else {
            super.equals(other)
        }
    }
}

/** Convert from a collection of app related objects to objects for use with a database*/
fun List<PhotoShootPrep>.asDatabaseModel(): Array<DatabasePhotoShootPrep> {
    return map {
        DatabasePhotoShootPrep(
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
            id = it.id,
            photoShootId = it.photoShootId
        )
    }.toTypedArray()
}