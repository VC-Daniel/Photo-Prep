package com.vcdaniel.photoprep.ui.photoShootLocation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.FrameLayout
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vcdaniel.photoprep.databinding.SheetDialogEditNextPhotoShootBinding
import java.util.*

/** Allows the user to edit the details of the next photo shoot for a specific photo shoot location */
class EditNextPhotoShootModalSheet : BottomSheetDialogFragment() {

    private lateinit var binding: SheetDialogEditNextPhotoShootBinding
    private lateinit var viewModel: EditNextPhotoShootModalViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SheetDialogEditNextPhotoShootBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this

        // Create a bottom sheet to allow the user to edit the details of the next photo shoot.
        // Automatically expand the sheet. This is based on the logic at
        // https://stackoverflow.com/a/35976745
        val bottomSheet: FrameLayout = binding.nextPhotoShootBottomSheet
        val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        // Retrieve the next photo shoot date that was passed in using a bundle. This is based
        // on the tutorial at https://developer.android.com/guide/fragments/communicate
        if (arguments != null) {
            val nextPhotoShootBundle = requireArguments()

            val groupCheckedStatus =
                nextPhotoShootBundle.getBoolean(GROUP_CONDITION_CHECKED, false)
            val childCheckedStatus =
                nextPhotoShootBundle.getBoolean(CHILD_CONDITION_CHECKED, false)
            val petCheckedStatus =
                nextPhotoShootBundle.getBoolean(PET_CONDITION_CHECKED, false)
            val nextPhotoShootDate =
                nextPhotoShootBundle.getLong(NEXT_PHOTO_SHOOT_DATE, 0)

            // Create a view model and pass in the data for the next photo shoot
            viewModel = ViewModelProvider(
                this,
                EditNextPhotoShootModalViewModel.EditPhotoShootLocationViewModelFactory(
                    groupCheckedStatus,
                    childCheckedStatus,
                    petCheckedStatus,
                    nextPhotoShootDate
                )
            )[EditNextPhotoShootModalViewModel::class.java]

            // Bind the viewModel so the data such as the selected date will automatically be
            // updated
            binding.viewModel = viewModel
        }

        // Set the minimum selectable date to today. This is based on the the tutorial at
        // https://abhiandroid.com/ui/calendarview#Attributes_of_CalendarView
        binding.nextPhotoShootCalendar.minDate = Date().time

        // Monitor changes to the selected date and save the selected date as the new date for the
        // photo shoot location. This is partially based on the answer at
        // https://stackoverflow.com/a/68252779
        binding.nextPhotoShootCalendar.setOnDateChangeListener { view: CalendarView, year: Int, month: Int, dayOfMonth: Int ->
            val calender: Calendar = Calendar.getInstance()

            // Set the attributes in calender object to match the selected date. Use the calendar to
            // calculate the date in milliseconds and store the result
            calender.set(year, month, dayOfMonth)
            viewModel.setNextPhotoShootDate(calender.timeInMillis)
        }

        // Clear the selected date when the user clicks the clearSelectionButton
        binding.clearSelectionButton.setOnClickListener {
            viewModel.setNextPhotoShootDate(0)
        }

        // Pass the updated next photo shoot data as a fragment result using a bundle. This is
        // partially based on the logic at https://stackoverflow.com/a/34527080
        binding.saveEditButton.setOnClickListener {

            val bundle = Bundle()
            bundle.putBoolean(GROUP_CONDITION_CHECKED, viewModel.isGroupChecked.value!!)
            bundle.putBoolean(CHILD_CONDITION_CHECKED, viewModel.isChildChecked.value!!)
            bundle.putBoolean(PET_CONDITION_CHECKED, viewModel.isPetChecked.value!!)
            bundle.putLong(NEXT_PHOTO_SHOOT_DATE, viewModel.nextPhotoShootDate.value!!)

            setFragmentResult(PhotoShootLocationFragment.NEXT_PHOTO_SHOOT_MODAL_TAG, bundle)
            dismiss()
        }

        // If the user decides to cancel making changes dismiss the modal sheet
        binding.cancelEditButton.setOnClickListener {
            dismiss()
        }

        return binding.root
    }
}