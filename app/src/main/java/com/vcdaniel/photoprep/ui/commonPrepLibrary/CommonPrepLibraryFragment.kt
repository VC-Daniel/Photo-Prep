package com.vcdaniel.photoprep.ui.commonPrepLibrary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.RecyclerView
import com.vcdaniel.photoprep.CommonPrep
import com.vcdaniel.photoprep.ConditionType
import com.vcdaniel.photoprep.R
import com.vcdaniel.photoprep.databinding.FragmentCommonPrepLibraryBinding
import com.vcdaniel.photoprep.ui.CommonPrepListAdapter
import com.vcdaniel.photoprep.ui.editPrepItem.EditPrepItemDialog
import com.vcdaniel.photoprep.ui.editPrepItem.EditPrepItemViewModel

private const val COMMON_PREP_LIBRARY_TAG = "CommonPrepLibraryFragment"

/** Allows the user to create prep that represents common tasks or items that will be useful to add
 * to multiple photo shoot locations. */
class CommonPrepLibraryFragment : Fragment() {

    private var _binding: FragmentCommonPrepLibraryBinding? = null

    // Use a shared view model to edit a common prep item with a dialog. This is based on the answer
    // at https://stackoverflow.com/questions/62560019/hilt-creating-different-instances-of-view-model-inside-same-activity
    private val sharedViewModel: EditPrepItemViewModel by navGraphViewModels(R.id.nav_common_prep_library) { defaultViewModelProviderFactory } // by activityViewModels()

    // The view model for this fragment
    private lateinit var commonPrepLibraryViewModel: CommonPrepViewModel

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        commonPrepLibraryViewModel = ViewModelProvider(this)[CommonPrepViewModel::class.java]
        _binding = FragmentCommonPrepLibraryBinding.inflate(inflater, container, false)
        val root: View = binding.root
        binding.lifecycleOwner = this
        binding.viewModel = commonPrepLibraryViewModel

        // Show a dialog to create new common prep when the user clicks the add button
        binding.addCommonPrepFAB.setOnClickListener { showCommonPrepEditorDialog(null) }

        // When the shared view model sets savePrep to true then the user wants to save common prep
        // that was created or edited with the editor dialog
        sharedViewModel.savePrep.observe(viewLifecycleOwner) {
            if (it) {
                sharedViewModel.prep.value?.let { prepData ->
                    commonPrepLibraryViewModel.savePrepItem(
                        CommonPrep(prepData, false, sharedViewModel.prepId)
                    )
                }
                sharedViewModel.saveComplete()
            }
        }

        sharedViewModel.cancelEditingPrep.observe(viewLifecycleOwner)
        {
            if (it) {
                sharedViewModel.cancelEditingComplete()
            }
        }

        // When the shared view model sets deletePrep to true then the user wants to delete the
        // prep item that was being modified in the dialog
        sharedViewModel.deletePrep.observe(viewLifecycleOwner) {
            if (it) {
                if (sharedViewModel.prepId != 0L) {
                    commonPrepLibraryViewModel.deletePrepItem(sharedViewModel.prepId)
                }
                sharedViewModel.deleteComplete()
            }
        }

        // When the user clicks on a common prep item display the editor so they can modify it
        binding.prepRecycler.adapter = CommonPrepListAdapter(CommonPrepListAdapter.OnClickListener {
            showCommonPrepEditorDialog(it)
        })

        // Show the fab to create new common prep as either expanded, shrunk or hidden depending on
        // the users scrolling and the position in the recyclerView especially at the very top or
        // bottom. This is based on the tutorial at
        // https://www.geeksforgeeks.org/auto-hide-or-auto-extend-floating-action-button-in-recyclerview-in-android/
        binding.prepRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // if the recycler view is scrolled down shrink the FAB
                if (dy > 10 && binding.addCommonPrepFAB.isExtended) {
                    binding.addCommonPrepFAB.show()
                    binding.addCommonPrepFAB.shrink()
                }

                // if the recycler view is scrolled up extend the FAB
                if (dy < -10 && !binding.addCommonPrepFAB.isExtended) {
                    binding.addCommonPrepFAB.show()
                    binding.addCommonPrepFAB.extend()
                }

                // When the recycler view is at the first item always extend the FAB
                if (!recyclerView.canScrollVertically(-1)) {
                    binding.addCommonPrepFAB.show()
                    binding.addCommonPrepFAB.extend()
                }

                // If no scrolling is necessary then don't hide the fab
                if (recyclerView.canScrollVertically(-1) && !recyclerView.canScrollVertically(1)) {
                    // If the recycler view is at the last item always hide the FAB. This is in part
                    // based on the answer at https://stackoverflow.com/questions/36127734/detect-when-recyclerview-reaches-the-bottom-most-position-while-scrolling
                    if (!recyclerView.canScrollVertically(1)) {
                        binding.addCommonPrepFAB.hide()
                    }
                }
            }

        })

        return root
    }

    /** Show a dialog that allows the user to edit the supplied [commonPrep] */
    private fun showCommonPrepEditorDialog(commonPrep: CommonPrep?) {

        // If no common prep is passed in then the user is creating new common prep
        if (commonPrep == null) {
            sharedViewModel.setPrepData(
                CommonPrep(
                    prepName = "",
                    conditions = HashSet<ConditionType>(),
                    id = 0
                )
            )
        } else {
            // If the user is editing common prep then pass this data to the shared view model
            sharedViewModel.setPrepData(commonPrep)
        }

        val dialog = EditPrepItemDialog(R.id.nav_common_prep_library)
        dialog.show(childFragmentManager, COMMON_PREP_LIBRARY_TAG)
    }
}