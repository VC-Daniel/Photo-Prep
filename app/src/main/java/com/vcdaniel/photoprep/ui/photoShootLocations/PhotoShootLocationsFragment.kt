package com.vcdaniel.photoprep.ui.photoShootLocations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.vcdaniel.photoprep.PhotoShootLocation
import com.vcdaniel.photoprep.PhotoShootOverview
import com.vcdaniel.photoprep.com.vcdaniel.photoprep.ui.PhotoShootLocationsAdapter
import com.vcdaniel.photoprep.databinding.FragmentPhotoShootLocationsBinding

/** Display an overview of all the photo shoot locations the user has created. Allow the user to
 * select a photo shoot location to view additional details of or edit. Also allow the user to
 * initiate creating a new photo shoot location. */
class PhotoShootLocationsFragment : Fragment() {

    private var _binding: FragmentPhotoShootLocationsBinding? = null

    private val binding get() = _binding!!
    lateinit var photoShootLocationsViewModel: PhotoShootLocationsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Get the view model for this fragment
        photoShootLocationsViewModel =
            ViewModelProvider(this).get(PhotoShootLocationsViewModel::class.java)

        _binding = FragmentPhotoShootLocationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.lifecycleOwner = this
        binding.viewModel = photoShootLocationsViewModel

        // initiate creating a new photo shoot location when the user clicks on the newPhotoShootFAB
        binding.newPhotoShootFAB.setOnClickListener {
            showAddDialog(
                photoShootOverview = PhotoShootOverview(
                    photoShootLocationId = 0,
                    photoShootLocationName = "",
                    mainImagePath = "",
                    locationLng = 0.0,
                    locationLat = 0.0,
                    locationTitle = "",
                    nextPhotoShootHasGroup = false,
                    nextPhotoShootHasChild = false,
                    nextPhotoShootHasPet = false,
                    nextPhotoShootDate = 0
                )
            )
        }

        // Use a recycler view to show an overview of each photo shoot location with a thumbnail
        // and the name of each photo shoot location. If the user clicks on an overview display the
        // details of the photo shoot location or if they long press on an overview initiate editing
        // of the photo shoot location.
        binding.photoShootLocationsRecycler.adapter =
            PhotoShootLocationsAdapter(PhotoShootLocationsAdapter.OnClickListener {
                showPhotoShootDetails(it)
            }, PhotoShootLocationsAdapter.OnLongClickListener {
                showAddDialog(it.photoShootOverview)
            })

        // Show the fab to create a new photo shoot location as either expanded, shrunk or hidden
        // depending on the users scrolling and the position in the recyclerView especially at the
        // very top or bottom. This is based on the tutorial at
        // https://www.geeksforgeeks.org/auto-hide-or-auto-extend-floating-action-button-in-recyclerview-in-android/
        binding.photoShootLocationsRecycler.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // if the recycler view is scrolled down shrink the FAB
                if (dy > 10 && binding.newPhotoShootFAB.isExtended) {
                    binding.newPhotoShootFAB.show()
                    binding.newPhotoShootFAB.shrink()
                }

                // if the recycler view is scrolled up extend the FAB
                if (dy < -10 && !binding.newPhotoShootFAB.isExtended) {
                    binding.newPhotoShootFAB.show()
                    binding.newPhotoShootFAB.extend()
                }

                // When the recycler view is at the first item always extend the FAB
                if (!recyclerView.canScrollVertically(-1)) {
                    binding.newPhotoShootFAB.show()
                    binding.newPhotoShootFAB.extend()
                }

                // If the recycler view is at the last item always hide the FAB. This is in part
                // based on the answer at https://stackoverflow.com/questions/36127734/detect-when-recyclerview-reaches-the-bottom-most-position-while-scrolling
                if (recyclerView.canScrollVertically(-1) && !recyclerView.canScrollVertically(1)) {
                    if (!recyclerView.canScrollVertically(1)) {
                        binding.newPhotoShootFAB.hide()
                    }
                }
            }

        })

        return root
    }

    override fun onResume() {
        super.onResume()
        // Retrieve the latest photo shoot location data
        photoShootLocationsViewModel.refreshPhotoShootLocations()
    }

    /** Show the user the details of the selected photo shoot location in the
     * PhotoShootLocationFragment. */
    private fun showPhotoShootDetails(photoShootLocation: PhotoShootLocation) {
        val action =
            PhotoShootLocationsFragmentDirections.actionNavPhotoShootLocationsToPhotoShootLocationFragment(
                photoShootLocation.photoShootOverview.photoShootLocationId
            )
        findNavController().navigate(
            action
        )
    }

    /** Allow the user to edit the details of the selected photo shoot location in the
     * EditPhotoShootLocationFragment. */
    private fun showAddDialog(photoShootOverview: PhotoShootOverview) {
        val action =
            PhotoShootLocationsFragmentDirections.actionNavPhotoShootLocationsToEditPhotoShootLocation(
                photoShootOverview = photoShootOverview
            )
        findNavController().navigate(
            action
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}