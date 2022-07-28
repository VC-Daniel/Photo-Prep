package com.vcdaniel.photoprep.ui.editPrepItem

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vcdaniel.photoprep.CommonPrep
import com.vcdaniel.photoprep.ConditionType
import com.vcdaniel.photoprep.PhotoShootPrep
import com.vcdaniel.photoprep.Prep

/** The view model for the EditPrepItemDialog. This facilitates editing, creating and saving
 * common prep and photo shoot location prep. */
class EditPrepItemViewModel : ViewModel() {

    /** The prep that is being edited */
    var prep = MutableLiveData<Prep?>()

    /** Denotes if the user wants to save the prep that is being edited or created */
    var savePrep: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    /** Denotes that the user wants to cancel editing or creating prep */
    var cancelEditingPrep: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    /** Denotes that the user wants to delete the prep item that is being edited */
    var deletePrep: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    /** The id of the prep that is being edited or 0 if the user is creating new prep */
    var prepId: Long = 0
    var photoShootId: Long = 0
    var completed: Boolean = false

    fun savePrepData() {
        savePrep.value = true
    }

    // Using live data to denote when the user wants to save or delete prep is in part based on
    // the answer at https://stackoverflow.com/questions/55926038/how-to-handle-onclick-or-ontouch-like-events-in-viewmodel-with-data-binding-in-m
    fun saveComplete() {
        savePrep.value = false
        clearData()
    }

    fun deletePrep() {
        deletePrep.value = true
    }

    fun deleteComplete() {
        deletePrep.value = false
        clearData()
    }

    fun cancelEditing() {
        cancelEditingPrep.value = true
    }

    fun cancelEditingComplete() {
        cancelEditingPrep.value = false
    }

    private fun clearData() {
        prep.value = null
        prepId = 0
        photoShootId = 0
    }

    /** If the user is editing common prep then copy the data into a temporary object so the changes
     * will only be saved if the user chooses to save the data */
    fun setPrepData(prepData: CommonPrep) {
        val previousConditions = HashSet<ConditionType>()
        prepData.conditions.forEach {
            previousConditions.add(it)
        }

        prep.value = Prep(
            prepName = prepData.prepName,
            conditions = previousConditions
        )

        this.prepId = prepData.id
    }

    /** If the user is editing photo shoot location prep then copy the data into a temporary object so the changes
     * will only be saved if the user chooses to save the data */
    fun setPrepData(prepData: PhotoShootPrep) {
        val previousConditions = HashSet<ConditionType>()
        prepData.conditions.forEach {
            previousConditions.add(it)
        }

        prep.value = Prep(
            prepName = prepData.prepName,
            conditions = previousConditions
        )

        this.prepId = prepData.id
        this.photoShootId = prepData.photoShootId
        this.completed = prepData.completed
    }
}