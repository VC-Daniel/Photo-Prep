package com.vcdaniel.photoprep

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/** A task to complete before, or item to bring. The prep can have [conditions] to denote in what
 * scenario the prep is needed. The prep should be considered relevant if any of the [conditions]
 * are met. */
@Parcelize
data class Prep(
    var prepName: String,
    var conditions: HashSet<ConditionType>
) : Parcelable