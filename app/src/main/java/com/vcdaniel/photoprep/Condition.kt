package com.vcdaniel.photoprep

import android.content.Context

/** The name and icon that represent the supplied [conditionType]. */
class Condition(var conditionType: ConditionType, context: Context) {
    var iconInt: Int = 0
    var conditionName: String

    init {
        // Set the icon and name that corresponds with the provided conditionType
        when (conditionType) {
            ConditionType.GROUP -> {
                iconInt = R.drawable.ic_baseline_groups_24
                conditionName = context.getString(R.string.group_condition)
            }
            ConditionType.CHILD -> {
                iconInt = R.drawable.ic_baseline_child_care_24
                conditionName = context.getString(R.string.child_condition)
            }
            ConditionType.PET -> {
                iconInt = R.drawable.ic_baseline_pets_24
                conditionName = context.getString(R.string.pet_condition)
            }
            ConditionType.RAINY -> {
                iconInt = R.drawable.ic_rain_cloud
                conditionName = context.getString(R.string.rainy_condition)
            }
            ConditionType.CLOUDY -> {
                iconInt = R.drawable.ic_clouds
                conditionName = context.getString(R.string.cloudy_condition)
            }
            ConditionType.WINDY -> {
                iconInt = R.drawable.ic_baseline_wind_power_24
                conditionName = context.getString(R.string.windy_condition)
            }
            ConditionType.HOT -> {
                iconInt = R.drawable.ic_hot_24
                conditionName = context.getString(R.string.hot_condition)
            }
            ConditionType.COLD -> {
                iconInt = R.drawable.ic_cold_24
                conditionName = context.getString(R.string.cold_condition)
            }
        }
    }
}

/** The different conditions that can trigger prep to be considered relevant. */
enum class ConditionType {
    GROUP, CHILD, PET, RAINY, CLOUDY, WINDY, HOT, COLD
}

/** Determine if a condition is contained with the supplied ConditionTypes. */
fun HashSet<ConditionType>.containsCondition(conditionType: ConditionType): Boolean {
    return this.contains(conditionType)
}