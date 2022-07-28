package com.vcdaniel.photoprep.ui.editPrepItem

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnLayout
import androidx.fragment.app.DialogFragment
import androidx.navigation.navGraphViewModels
import com.google.android.material.chip.Chip
import com.vcdaniel.photoprep.ConditionType
import com.vcdaniel.photoprep.R
import com.vcdaniel.photoprep.databinding.DialogEditPrepItemBinding

private const val ORIGINATING_VIEW_ID: String = "originatingViewId"

//https://developer.android.com/codelabs/basic-android-kotlin-training-shared-viewmodel#3
/** Allows the user to edit or create a prep item. This includes specifying the name and conditions
 * on the prep. This dialog also allows the user to delete prep */
class EditPrepItemDialog(var originatingViewId: Int = 0) : DialogFragment() {

    // The view model for the dialog which allows the data to be maintained if the user rotates the
    // screen as well as pass data to the CommonPrepLibrary and photoShootLocation fragments.
    lateinit var viewModel: EditPrepItemViewModel

    /** Indicates if the user has started editing the name of the prep */
    private var editingStarted = false

    /** Watch for the name of the prep to be edited and display a warning to the user if the name
     * doesn't meet the requirements to be saved. This is based on the tutorial at
     * https://stackoverflow.com/questions/40569436/kotlin-addtextchangelistener-lambda*/
    private lateinit var requiredFieldTextWatcher: TextWatcher
    private lateinit var binding: DialogEditPrepItemBinding

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save which type of fragment is using this view to edit a prep item
        outState.putInt(ORIGINATING_VIEW_ID, originatingViewId)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Retrieve which type of fragment is using this view to edit a prep item
        savedInstanceState?.let {
            if (savedInstanceState.containsKey(ORIGINATING_VIEW_ID)) {
                originatingViewId = savedInstanceState.getInt(ORIGINATING_VIEW_ID)
            }
        }

        // Get the appropriate view model depending on which type of fragment is using this view to edit a prep item
        val myViewModel: EditPrepItemViewModel by navGraphViewModels(originatingViewId) { defaultViewModelProviderFactory }
        viewModel = myViewModel

        binding = DialogEditPrepItemBinding.inflate(layoutInflater)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // Set up appropriate buttons to allow the user to either save the edits or cancel editing
        // the prep
        val alertDialog = AlertDialog.Builder(requireActivity())
            .setView(binding.root)
            .setPositiveButton(getString(R.string.dialog_save_button_text)) { dialog, id ->
                viewModel.savePrepData()
            }
            .setNegativeButton(getString(R.string.dialog_cancel_button_text)) { dialog, id ->
                viewModel.cancelEditing()
            }.create()

        // Watch for the name of the prep to be edited and display a warning to the user if the name
        // doesn't meet the requirements to be saved. This is in part based on the tutorial at
        // https://www.tutorialspoint.com/how-to-use-the-textwatcher-class-in-kotlin
        requiredFieldTextWatcher = object : TextWatcher {
            override fun afterTextChanged(text: Editable?) {
                editingStarted = true
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !text.isNullOrBlank()

                if (editingStarted && text.isNullOrBlank()) {
                    binding.prepNameEditText.error = getString(R.string.required_error_text)
                    editingStarted
                } else {
                    // Clear the error once the name meets the requirements to be saved
                    binding.prepNameEditText.error = null
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        }

        setConditionalButtonsOnClick()

        viewModel.prep.observe(this) {
            if (it == null) {
                alertDialog.dismiss()
            }
        }

        // Set the focus of to the edit text that represents the name of the prep and move the
        // cursor to the end of the prep name. This is based on the answer at
        // https://stackoverflow.com/a/16038451
        binding.prepNameEditText.doOnLayout { prepNameEditText ->
            prepNameEditText as EditText
            prepNameEditText.requestFocus()
            prepNameEditText.setSelection(prepNameEditText.text.length)
        }

        // Return the custom dialog
        return alertDialog
    }

    /** Set up on click listeners for each of the prep conditions to save the user's
     * desired condition(s) for this prep */
    private fun setConditionalButtonsOnClick() {
        binding.groupChip.setOnClickListener {
            toggleOnConditionalChipsState(
                it as Chip,
                ConditionType.GROUP
            )
        }
        binding.childChip.setOnClickListener {
            toggleOnConditionalChipsState(
                it as Chip,
                ConditionType.CHILD
            )
        }
        binding.petChip.setOnClickListener {
            toggleOnConditionalChipsState(
                it as Chip,
                ConditionType.PET
            )
        }
        binding.rainyChip.setOnClickListener {
            toggleOnConditionalChipsState(
                it as Chip,
                ConditionType.RAINY
            )
        }
        binding.cloudyChip.setOnClickListener {
            toggleOnConditionalChipsState(
                it as Chip,
                ConditionType.CLOUDY
            )
        }
        binding.windyChip.setOnClickListener {
            toggleOnConditionalChipsState(
                it as Chip,
                ConditionType.WINDY
            )
        }
        binding.hotChip.setOnClickListener {
            toggleGroupOnConditionalChipsState(
                it as Chip,
                ConditionType.HOT, ConditionType.COLD
            )
        }
        binding.coldChip.setOnClickListener {
            toggleGroupOnConditionalChipsState(
                it as Chip,
                ConditionType.COLD, ConditionType.HOT
            )
        }
    }

    /** Either add or remove the selected condition depending on the chip's state and display to
     * the user that their change has been taken into account. */
    private fun toggleOnConditionalChipsState(chip: Chip, conditionType: ConditionType) {
        if (viewModel.prep.value?.conditions!!.contains(conditionType)) {
            chip.isChecked = false
            viewModel.prep.value?.conditions?.remove(conditionType)
        } else {
            chip.isChecked = true
            viewModel.prep.value?.conditions?.add(conditionType)
        }
    }

    /** In the case of related chips such as hot and cold make sure only one of the conditions can
     * be selected at a time */
    private fun toggleGroupOnConditionalChipsState(
        chip: Chip,
        selectedConditionType: ConditionType,
        groupedConditionType: ConditionType
    ) {
        viewModel.prep.value?.conditions!!.remove(groupedConditionType)

        if (viewModel.prep.value?.conditions!!.contains(selectedConditionType)) {
            chip.isChecked = false
            viewModel.prep.value?.conditions?.remove(selectedConditionType)
        } else {
            chip.isChecked = true
            viewModel.prep.value?.conditions?.add(selectedConditionType)
        }
    }


    override fun onPause() {
        super.onPause()

        // When the app is not in the foreground stop listening for changes to the prep name
        binding.prepNameEditText.removeTextChangedListener(requiredFieldTextWatcher)
    }

    override fun onResume() {
        super.onResume()

        val dialog = dialog as AlertDialog?

        // Validate if the user should be able able to save the prep and if not disable the
        // positive button this is based on the answer at https://stackoverflow.com/a/40669929
        if (viewModel.prep.value?.prepName == "") {
            dialog!!.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        }

        binding.prepNameEditText.addTextChangedListener(requiredFieldTextWatcher)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        viewModel.prep.value = null
        viewModel.cancelEditing()
    }
}