package com.vcdaniel.photoprep.ui.preferences

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.vcdaniel.photoprep.R

// https://developer.android.com/guide/topics/ui/settings
/** Display the preferences to the user and allow them to change them. The preferences include
 * weather related preferences such as what the minimum temperature to consider as hot is.
 * Additionally there are user interface preferences such as if the user prefers image or
 * map thumbnails. */
class PreferencesFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}