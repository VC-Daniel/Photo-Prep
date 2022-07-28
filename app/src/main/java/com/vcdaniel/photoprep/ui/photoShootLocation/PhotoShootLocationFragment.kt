package com.vcdaniel.photoprep.ui.photoShootLocation

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.view.*
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.preference.PreferenceManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.chip.Chip
import com.squareup.picasso.Picasso
import com.vcdaniel.photoprep.*
import com.vcdaniel.photoprep.databinding.FragmentPhotoShootLocationBinding
import com.vcdaniel.photoprep.ui.PhotoShootPrepListAdapter
import com.vcdaniel.photoprep.ui.editPrepItem.EditPrepItemDialog
import com.vcdaniel.photoprep.ui.editPrepItem.EditPrepItemViewModel
import kotlinx.android.synthetic.main.fragment_photo_shoot_location.*
import java.io.File
import java.util.*
import kotlin.math.abs

const val GROUP_CONDITION_CHECKED = "GROUP_CONDITION_CHECKED"
const val CHILD_CONDITION_CHECKED = "CHILD_CONDITION_CHECKED"
const val PET_CONDITION_CHECKED = "PET_CONDITION_CHECKED"
const val NEXT_PHOTO_SHOOT_DATE = "NEXT_PHOTO_SHOOT_CHECKED"
const val SELECTED_PREP_KEY = "SELECTED_PREP"

/** Shows the user the details of a photo shoot location and allows them to add prep and mark prep
 * as complete. Information about the next time the user has a photo shoot at this location can also
 * be viewed and edited. For the user's convenience give them information about the weather on the
 * day of the photo shoot as well as allow them to easily launch another app to get directions to
 * the location */
class PhotoShootLocationFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentPhotoShootLocationBinding? = null
    private val binding get() = _binding!!

    /** The view model for this fragment */
    private lateinit var photoShootLocationViewModel: PhotoShootLocationViewModel

    /** The shared view model that facilitates editing the details of a prep item */
    private val sharedViewModel: EditPrepItemViewModel by navGraphViewModels(R.id.nav_photo_shoot_location) { defaultViewModelProviderFactory }

    /** Displays a map of the are around the photo shoot location using lite mode. This is based off
     * the documentation at https://developers.google.com/maps/documentation/android-sdk/lite#overview_of_lite_mode */
    lateinit var map: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private var wasEditingPhotoShoot = false

    /** The details of the issue with retrieving the weather for the next photo shoot */
    private var weatherIssueDetails = String()
    private var selectedCommonPrep = ArrayList<Int>()
    private var selectingCommonPrep = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        // Retrieve which photo shoot location the user wants to view the details of. The id is
        // passed into this fragment via the navigation library. This is in part based on the
        // tutorial at https://www.section.io/engineering-education/safe-args-in-android/
        val args: PhotoShootLocationFragmentArgs by navArgs<PhotoShootLocationFragmentArgs>()

        // Instantiate the view model and pass in which photo shoot was selected
        photoShootLocationViewModel = ViewModelProvider(
            this,
            PhotoShootLocationViewModel.PhotoShootLocationViewModelFactory(
                args.selectedPhotoShootLocationID,
                WeatherPreferences(sharedPreferences, requireContext()),
                requireActivity().application
            )
        )[PhotoShootLocationViewModel::class.java]

        // If the prep filter is changed then show the correct filter icon to the user
        photoShootLocationViewModel.prepFilter.observe(viewLifecycleOwner) { currentFilter ->
            setFilterUIState(currentFilter)
        }

        // Set the prep filter to either the existing value or retrieve the value such as if the
        // fragment is being recreated due to an orientation change or retrieve the value from the
        // preferences
        if (photoShootLocationViewModel.prepFilter.value != PrepFilter.NOT_SET) {
            photoShootLocationViewModel.prepFilter.value?.let {
                photoShootLocationViewModel.setFilter(
                    it, false
                )
            }
        } else {
            val defaultToFilteringPrep =
                sharedPreferences.getBoolean(getString(R.string.default_filter_state_key), true)
            if (defaultToFilteringPrep) {
                photoShootLocationViewModel.setFilter(PrepFilter.FILTERED, false)
            } else {
                photoShootLocationViewModel.setFilter(PrepFilter.ALL, false)
            }
        }

        _binding = FragmentPhotoShootLocationBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.lifecycleOwner = this
        binding.viewModel = photoShootLocationViewModel

        // Co-ordinate the users scrolling with minimizing all the views that are not related to the
        // prep for this photo shoot location
        coordinateMotion()

        // Setup all observers related to adding, deleting and modifying prep
        setupPrepObservers()

        // Setup the adapter to display all the prep for the selected photo shoot location and
        // if the user clicks on the prep then toggle the completed status and if the user long
        // clicks on prep then open the editor so they can edit or delete the prep
        binding.prepRecycler.adapter =
            PhotoShootPrepListAdapter(
                PhotoShootPrepListAdapter.OnClickListener
                {
                    val editedPrep = PhotoShootPrep(
                        it.prepName,
                        it.conditions,
                        !it.completed,
                        id = it.id,
                        photoShootId = it.photoShootId
                    )
                    photoShootLocationViewModel.newPrepItems.add(editedPrep)
                    photoShootLocationViewModel.updatePhotoShootPrepItem()
                },
                PhotoShootPrepListAdapter.OnLongClickListener
                {
                    showEditPrepDialog(it)
                }
            )

        // When the fab button to add prep is clicked the user has the option to either add prep
        // from the common library or create new prep. When the additional fab buttons are displayed
        // the addPrepItemFab icon should change to a close icon to indicate that this will minimize
        // the adding prep buttons
        binding.extendedFabMotionLayout.setTransitionListener(object :
            MotionLayout.TransitionListener {
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {

            }

            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float
            ) {

            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                if (motionLayout?.progress == 1.0f) {
                    binding.addPrepItemFab.setImageResource(R.drawable.ic_baseline_close_24)
                } else {
                    binding.addPrepItemFab.setImageResource(R.drawable.ic_baseline_add_task_24)
                }
            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {
            }

        })

        // Setup all the onClick listeners for this fragment
        setupOnClickListeners()

        // When the photo shoot location changes such as if the main photo is changed update the ui,
        // including the weather related views, to show the latest values. Also update the weather
        // related ui items.
        photoShootLocationViewModel.photoShootDataUpdated.observe(viewLifecycleOwner) {
            if (it) {
                initializePhotoShootLocationViews()
                initiateWeatherUIRefresh()
            }
        }

        // If the user was selecting common prep from the dialog recreate the dialog and populate it
        // with the items they were selecting
        savedInstanceState?.let {
            if (savedInstanceState.containsKey(SELECTED_PREP_KEY)) {
                savedInstanceState.getIntegerArrayList(SELECTED_PREP_KEY)?.let { selectedItems ->
                    selectedCommonPrep = selectedItems

                    val alertDialog = getCommonPrepSelectorAlertDialog()
                    alertDialog?.show()

                    // Only enable the add button if the user has selected at least one common prep
                    alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled =
                        (selectedItems.size > 0)
                    selectingCommonPrep = true
                }
            }
        }

        setHasOptionsMenu(true)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // If the user was editing the photo shoot location information then retrieve the latest
        // version of the photo shoot location
        if (wasEditingPhotoShoot) {
            photoShootLocationViewModel.retrieveLatestPhotoShootLocationData(true)
            wasEditingPhotoShoot = false
        }

        // When the user edits the next photo shoot retrieve any changes and save them in the
        // repository. This is based on the tutorial at https://developer.android.com/guide/fragments/communicate
        childFragmentManager.setFragmentResultListener(
            NEXT_PHOTO_SHOOT_MODAL_TAG,
            this
        ) { _, bundle ->
            if (bundle.containsKey(NEXT_PHOTO_SHOOT_DATE)
                && bundle.containsKey(GROUP_CONDITION_CHECKED)
                && bundle.containsKey(CHILD_CONDITION_CHECKED)
                && bundle.containsKey(PET_CONDITION_CHECKED)
            ) {
                // Get the data passed in about the next photo shoot
                val newGroupCheckedStatus = bundle.getBoolean(GROUP_CONDITION_CHECKED, false)
                val newChildCheckedStatus = bundle.getBoolean(CHILD_CONDITION_CHECKED, false)
                val newPetCheckedStatus = bundle.getBoolean(PET_CONDITION_CHECKED, false)
                val newNextPhotoShootDate = bundle.getLong(NEXT_PHOTO_SHOOT_DATE, 0)

                // Update the view model data to reflect these changes
                photoShootLocationViewModel.photoShootLocation.value?.photoShootOverview?.let { photoShootOverview ->
                    photoShootOverview.nextPhotoShootHasGroup = newGroupCheckedStatus
                    photoShootOverview.nextPhotoShootHasChild = newChildCheckedStatus
                    photoShootOverview.nextPhotoShootHasPet = newPetCheckedStatus
                    photoShootOverview.nextPhotoShootDate = newNextPhotoShootDate
                }

                // Indicate that the changes to the photo shoot location's next photo shoot
                // needs to be saved.
                photoShootLocationViewModel.savePhotoShootNextPhotoShoot.value = true
            }
        }

        // When the next photo shoot should be saved update the repository to reflect the changes
        // the user made and refresh the ui, including the weather related views.
        photoShootLocationViewModel.savePhotoShootNextPhotoShoot.observe(viewLifecycleOwner) {
            if (it) {
                photoShootLocationViewModel.savePhotoShootOverviewData()
                setNextPhotoShootData()
                initiateWeatherUIRefresh()
            }
        }

        // When a change to the photo shoot prep should be saved update the repository to reflect
        // the changes the user made and minimize the fabs for adding prep.
        photoShootLocationViewModel.savePhotoShootPrep.observe(viewLifecycleOwner) {
            if (it) {
                photoShootLocationViewModel.savePhotoShootPrepData()
                binding.extendedFabMotionLayout.transitionToStart()
            }
        }

        // By default show the weather note view and icon
        binding.weatherNoteDetail.alpha = 1.0F
        binding.weatherNoteInfoImage.alpha = 1.0F

        // Obtain the SupportMapFragment and get notified when the map is ready to be used. This is
        // in part based off the logic found at:
        // https://stackoverflow.com/questions/63931552/how-to-use-google-maps-android-sdk-in-a-fragment
        mapFragment =
            childFragmentManager.findFragmentById(R.id.liteMap) as SupportMapFragment

        // Don't allow the user to directly modify the map. This is based on
        // https://developers.google.com/maps/documentation/android-sdk/events
        mapFragment.view?.isClickable = false
        mapFragment.getMapAsync(this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Add the menu which allows the user to edit the photo shoot location
        inflater.inflate(R.menu.edit_photo_shoot, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit_photo_shoot -> {
                // Open the fragment that allows the user to edit the photo shoot location
                // overview details
                showEditFragment(photoShootLocationViewModel.photoShootLocation.value!!.photoShootOverview)
                true
            }
            else -> false
        }
    }

    /** Setup all the onClick listeners for this fragment */
    private fun setupOnClickListeners() {
        // As a convenience to the user allow them to uncheck all the prep items for this photo shoot
        // location. Confirm with the user that they want to do this before proceeding because they
        // will lose all completion information about items including those that are not displayed
        // as a result of filtering
        binding.uncheckAllButton.setOnClickListener {
            val alertDialog: AlertDialog? = activity?.let {
                val builder = AlertDialog.Builder(it)
                builder.apply {
                    setPositiveButton(R.string.continue_uncheck,
                        DialogInterface.OnClickListener { _, _ ->
                            photoShootLocationViewModel.unCheckAllPrepItems()
                        })
                    setNegativeButton(R.string.cancel_uncheck,
                        DialogInterface.OnClickListener { _, _ ->

                        })
                }
                    // Set other dialog properties
                    .setTitle(R.string.dialog_uncheck_all_prep_title)
                    .setMessage(R.string.dialog_uncheck_all_prep_message)
                    .setIcon(R.drawable.ic_baseline_remove_done_24)

                // Create the AlertDialog
                builder.create()
            }
            // show the dialog to confirm if the user wants to uncheck all prep
            alertDialog?.show()
        }

        // When the user selects the filter button toggle the icon to indicate what will happen if
        // they click the button again and update which prep is shown
        binding.filterButton.setOnClickListener {
            if (photoShootLocationViewModel.prepFilter.value == PrepFilter.ALL) {
                photoShootLocationViewModel.setFilter(PrepFilter.FILTERED)
            } else {
                photoShootLocationViewModel.setFilter(PrepFilter.ALL)

            }
        }

        // Give the user additional information about why the weather cannot be retrieved for the
        // photo shoot location. For example if the date is beyond the available forecast.
        binding.weatherNoteDetail.setOnClickListener {
            val alertDialog: AlertDialog? = activity?.let {
                val builder = AlertDialog.Builder(it)
                builder.apply {
                    setPositiveButton(R.string.ok,
                        DialogInterface.OnClickListener { _, _ ->
                        })
                }
                    .setTitle(R.string.dialog_weather_not_retrieved_title)
                    .setMessage(weatherIssueDetails)
                    .setIcon(R.drawable.ic_no_weather_data_alt)
                builder.create()
            }
            alertDialog?.show()
        }

        binding.addNewPrepItemFab.setOnClickListener {
            // Show the dialog to create new prep
            showEditPrepDialog(null)
        }

        // When the user clicks on the directions button open a separate app and supply it with the
        // photo shoot location co-ordinates to navigate to. This is based on the tutorial at
        // https://developers.google.com/maps/documentation/urls/get-started#constructing-valid-urls
        binding.directionsButton.setOnClickListener {
            val locationMarker = String.format(
                "%f,%f",
                photoShootLocationViewModel.photoShootLocation.value?.photoShootOverview?.locationLat,
                photoShootLocationViewModel.photoShootLocation.value?.photoShootOverview?.locationLng
            )

            val scheme = "https"
            val authority = "www.google.com"
            val mapsApiPath = "maps"
            val apiRequest = "dir/"
            val apiVersionKey = "api"
            val apiVersionValue = "1"
            val apiActionKey = "dir_action"
            val apiActionValue = "navigate"
            val apiDestinationParameterKey = "destination"

            val builder = Uri.Builder()
            builder.scheme(scheme)
                .authority(authority)
                .appendPath(mapsApiPath)
                .appendEncodedPath(apiRequest)
                .appendQueryParameter(apiVersionKey, apiVersionValue)
                .appendQueryParameter(apiActionKey, apiActionValue)
                .appendQueryParameter(apiDestinationParameterKey, locationMarker)
            val mapDirectionsUrl = builder.build().toString()

            // Launch an intent to open the approriate app to show navigation information to the
            // user. This is based on the answer at https://stackoverflow.com/a/2663565
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(mapDirectionsUrl)
            )
            startActivity(intent)
        }

        // Open a dialog that allows the user to choose which prep to copy from the common
        // prep library to this photo shoot location. This is in part based on the tutorial at
        // https://developer.android.com/guide/topics/ui/dialogs#AddingAList
        binding.addCommonPrepItemFab.setOnClickListener {
            // If there isn't any common prep defined explain that the user must first add prep
            // to the common prep library before they can copy it to a photo shoot location
            if (photoShootLocationViewModel.commonPrep.value.isNullOrEmpty()) {
                val alertDialog: AlertDialog? = activity?.let {
                    val builder = AlertDialog.Builder(it)
                    builder.apply {
                        setPositiveButton(R.string.ok,
                            DialogInterface.OnClickListener { _, _ ->
                            })
                    }
                        // Set other dialog properties
                        .setTitle(R.string.dialog_no_common_prep_title)
                        .setMessage(R.string.dialog_no_common_prep_message)
                        .setIcon(R.drawable.ic_baseline_checklist_rtl_24)
                    // Create the AlertDialog
                    builder.create()
                }

                alertDialog?.show()
            } else {
                val alertDialog: AlertDialog? = getCommonPrepSelectorAlertDialog()
                alertDialog?.show()
                selectingCommonPrep = true

                // disable the add button until the user has selected at least 1 common prep
                alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
            }

        }

        // Open the editor to modify the next photo shoot if the user clicks on the calender icon
        binding.editNextPhotoShootButton.setOnClickListener {
            openNextPhotoShootEditor()
        }

        // Open the editor to modify the next photo shoot and toggle the selected option
        binding.groupChip.setOnClickListener {
            openNextPhotoShootEditor(groupChecked = true)
        }

        binding.childChip.setOnClickListener {
            openNextPhotoShootEditor(childChecked = true)
        }

        binding.petChip.setOnClickListener {
            openNextPhotoShootEditor(petChecked = true)
        }

        // If the user clicks on the weather description label trigger marquee scrolling. If the
        // weather description is long the text will automatically scroll once but the user can
        // manually trigger this to scroll again by click on it.
        binding.weatherDescriptionLabel.setOnClickListener {
            binding.weatherDescriptionLabel.isSelected = false
            binding.weatherDescriptionLabel.isSelected = true
        }
    }

    /** Setup observers related to adding, deleting and modifying prep */
    private fun setupPrepObservers() {
        // When the user wants to save changes to a prep item or create a new prep item
        // store the changes in the repository
        sharedViewModel.savePrep.observe(viewLifecycleOwner) {
            if (it) {
                sharedViewModel.prep.value?.let { prepData ->
                    val newPrep = PhotoShootPrep(
                        prepData,
                        sharedViewModel.completed,
                        sharedViewModel.prepId,
                        photoShootLocationViewModel.photoShootLocation.value!!.photoShootOverview.photoShootLocationId
                    )
                    photoShootLocationViewModel.newPrepItems.add(newPrep)
                    photoShootLocationViewModel.savePhotoShootPrep.value = true
                }
                sharedViewModel.saveComplete()
            }
        }

        /** When the user wants to cancel editing prep minimize the add prep button */
        sharedViewModel.cancelEditingPrep.observe(viewLifecycleOwner)
        {
            if (it) {
                binding.extendedFabMotionLayout.transitionToStart()
                sharedViewModel.cancelEditingComplete()
            }
        }

        /** When the user wants to delete a prep item delete the item via the view model */
        sharedViewModel.deletePrep.observe(viewLifecycleOwner) {
            if (it) {
                if (sharedViewModel.prepId != 0L) {
                    photoShootLocationViewModel.deletePrepItem(
                        sharedViewModel.prepId,
                        sharedViewModel.photoShootId
                    )
                }

                sharedViewModel.deleteComplete()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // If the user was in the process of adding common prep save their current selection so it
        // can be displayed in the dialog again so they can continue choosing which common
        // prep to add to the photo shoot location.
        if (selectingCommonPrep) {
            outState.putIntegerArrayList(SELECTED_PREP_KEY, selectedCommonPrep)
        }
    }

    /** Set the filter icon that represents what will happen if the user clicks on it  */
    private fun setFilterUIState(currentFilter: PrepFilter?) {
        if (currentFilter == PrepFilter.ALL) {
            binding.filterButton.text = getString(R.string.filter_apply_filter)
            // This is based on the tutorial at
            // https://www.tutorialspoint.com/how-to-programmatically-set-drawableleft-on-android-button
            val filterImage =
                AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.ic_baseline_filter_alt_off_24
                )
            binding.filterButton.setCompoundDrawablesWithIntrinsicBounds(
                filterImage,
                null,
                null,
                null
            )
        } else {
            binding.filterButton.text = getString(R.string.filter_show_all)
            val filterImage =
                AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.ic_baseline_filter_alt_24
                )
            binding.filterButton.setCompoundDrawablesWithIntrinsicBounds(
                filterImage,
                null,
                null,
                null
            )
        }
    }

    /** Create an alert dialog that allows the user to select common prep to copy to the photo
     * shoot location */
    private fun getCommonPrepSelectorAlertDialog(): AlertDialog? {
        activity?.let { it ->
            val builder = AlertDialog.Builder(it)

            val commonPrepNames = photoShootLocationViewModel.commonPrep.value!!.asListOfNames()

            // Create an array to store which items are selected. This is based on the answer at
            // https://stackoverflow.com/a/18011806
            val initialCheckStates = BooleanArray(commonPrepNames.size)

            // set up the initial checked state of each items. This is manually useful for if the
            // user was using this dialog then the screen orientation was changed so the dialog
            // has to be recreated
            selectedCommonPrep.forEach { position ->
                if (initialCheckStates.size > position) {
                    initialCheckStates[position] = true
                }
            }

            // Create a dialog that allows the user to choose one or more common prep item to copy
            // to the photo shoot location
            builder.apply {
                setMultiChoiceItems(commonPrepNames,
                    initialCheckStates,
                    DialogInterface.OnMultiChoiceClickListener { dialog, selectedPrepPosition, isChecked ->
                        if (isChecked) {
                            // If the user checked the item, add it to the selected items
                            selectedCommonPrep.add(selectedPrepPosition)
                        } else if (selectedCommonPrep.contains(selectedPrepPosition)) {
                            // Else, if the item is already in the array, remove it
                            selectedCommonPrep.remove(selectedPrepPosition)
                        }

                        // By default disable the add button until the user has selected at least
                        // one common prep. This is based on the answer at
                        // https://stackoverflow.com/a/8240314
                        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                            selectedCommonPrep.isNotEmpty()
                    })
                setPositiveButton(R.string.confirm_add,
                    DialogInterface.OnClickListener { _, _ ->

                        // Display an information message to the user to tell them how many items
                        // were added. This is mainly useful when the new prep item is not shown
                        // because it is filtered out so the user knows that it was added even if
                        // it isn't displayed
                        val itemsAddedMessage =
                            when {
                                selectedCommonPrep.isNullOrEmpty() -> {
                                    getString(R.string.no_prep_added_message)
                                }
                                selectedCommonPrep.size == 1 -> {
                                    String.format(
                                        getString(R.string.one_prep_added_message),
                                        selectedCommonPrep.size
                                    )
                                }
                                else -> {
                                    String.format(
                                        getString(R.string.multiple_prep_added_message),
                                        selectedCommonPrep.size
                                    )
                                }
                            }

                        Toast.makeText(
                            requireContext(),
                            itemsAddedMessage,
                            Toast.LENGTH_LONG
                        ).show()

                        // Copy the selected common prep items to the photo shoot location's prep
                        val prepItemsToAdd = ArrayList<PhotoShootPrep>()
                        selectedCommonPrep.forEach { selectedPrepPosition ->
                            prepItemsToAdd.add(
                                photoShootLocationViewModel.commonPrep.value!![selectedPrepPosition].asPhotoShootPrep(
                                    photoShootLocationViewModel.photoShootLocation.value!!.photoShootOverview.photoShootLocationId
                                )
                            )
                        }
                        selectedCommonPrep.clear()
                        selectingCommonPrep = false

                        photoShootLocationViewModel.newPrepItems = prepItemsToAdd
                        photoShootLocationViewModel.savePhotoShootPrep.value = true
                    })
                setNegativeButton(R.string.cancel_add,
                    DialogInterface.OnClickListener { _, _ ->
                        binding.extendedFabMotionLayout.transitionToStart()
                        selectedCommonPrep.clear()
                        selectingCommonPrep = false
                    })
            }
                // Set other dialog properties
                .setTitle(R.string.add_common_prep_title)
                .setOnCancelListener {
                    binding.extendedFabMotionLayout.transitionToStart()
                    selectedCommonPrep.clear()
                    selectingCommonPrep = false
                }
            // Create the AlertDialog
            return builder.create()
        }
        return null
    }

    /** Setup the views that display the photo shoot overview information such as the photo and the
     * name. This is based in part on the answer at https://stackoverflow.com/a/22205521 */
    private fun initializePhotoShootLocationViews() {
        (activity as MainActivity).supportActionBar?.title =
            photoShootLocationViewModel.photoShootLocation.value?.photoShootOverview?.photoShootLocationName

        setNextPhotoShootData()

        if (photoShootLocationViewModel.photoShootLocation.value?.photoShootOverview!!.mainImagePath.isNotBlank()) {
            setNewImage(photoShootLocationViewModel.photoShootLocation.value?.photoShootOverview!!.mainImagePath)
        }

        // If the map is initialized and the co-ordinates have been set then add a marker to
        // represent the photo shoot location. This is in part based on
        // https://developers.google.com/maps/documentation/android-sdk/views#maps_android_camera_and_view_common_map_movements-kotlin
        // and https://stackoverflow.com/questions/33751753/how-to-implement-googlemap-cancelablecallback
        if (::map.isInitialized) {
            if (photoShootLocationViewModel.locationLatLng().latitude != 0.0) {

                addPhotoShootSavedLocationMarker(
                    photoShootLocationViewModel.locationLatLng()
                )

            }
        }
    }

    /** Refresh the weather related views, when the latest forecast data is available, to show the
     * latest information about the weather on the specified next photo shoot date*/
    private fun initiateWeatherUIRefresh() {
        // Verify if the forecast can be retrieved. For example if the specified date is in the past
        // or beyond what is available in the forecast then it is not retrievable
        val isForecastRetrievable = photoShootLocationViewModel.isForecastRetrievable()
        if (isForecastRetrievable == ForecastRetrievableStatus.RETRIEVABLE) {
            // If the weather data is current and represents the correct date then display the
            // data otherwise get the latest forecast data
            if (photoShootLocationViewModel.isWeatherDataCurrent() || photoShootLocationViewModel.weatherDataUpdated.value == true) {
                updateWeatherUI()
            } else {
                photoShootLocationViewModel.retrieveForecastData()
            }
        } else {
            clearWeatherUI(isForecastRetrievable)
        }
    }

    /** Display the latest weather forecast data */
    private fun updateWeatherUI() {
        photoShootLocationViewModel.weatherDataUpdated.value = false

        // By default hide the controls that indicate an issue with retrieving the forecast
        binding.weatherNoteDetail.alpha = 0F
        binding.weatherNoteInfoImage.alpha = 0F
        binding.weatherNoteDetail.isClickable = false

        // Display the latest weather forecast data in the preferred units
        photoShootLocationViewModel.photoShootLocation.value.let { photoShootLocation ->
            photoShootLocation?.weatherForecast?.let { forecast ->
                if (photoShootLocationViewModel.weatherPreferences.prefersImperial()) {

                    binding.highTempLabel.text = String.format(
                        getString(R.string.high_temp_format_imperial),
                        forecast.maxTemp
                    )
                    binding.lowTempLabel.text = String.format(
                        getString(R.string.low_temp_format_imperial),
                        forecast.minTemp
                    )
                    binding.windLabel.text = String.format(
                        getString(R.string.wind_format_imperial),
                        forecast.windSpeed
                    )
                } else {
                    binding.highTempLabel.text = String.format(
                        getString(R.string.high_temp_format_metric),
                        forecast.maxTemp
                    )
                    binding.lowTempLabel.text = String.format(
                        getString(R.string.low_temp_format_metric),
                        forecast.minTemp
                    )
                    binding.windLabel.text = String.format(
                        getString(R.string.wind_format_metric),
                        forecast.windSpeed
                    )
                }

                binding.precipitationLabel.text = String.format(
                    getString(R.string.precipitation_format),
                    forecast.precipitationPercentage * 100
                )

                binding.weatherDescriptionLabel.text = forecast.description

                // Trigger marquee scrolling if necessary
                binding.weatherDescriptionLabel.isSelected = false
                binding.weatherDescriptionLabel.isSelected = true

                // Load the icon that represents the days overall weather from the api using the
                // Picasso library
                Picasso.get()
                    .load(
                        forecast.getWeatherIconPath()
                    )
                    .placeholder(R.drawable.ic_baseline_cloud_sync_24)
                    .fit()
                    .into(binding.mainWeatherIcon)

                Picasso.get()
                    .load(
                        forecast.getWeatherIconPath()
                    )
                    .placeholder(R.drawable.ic_baseline_cloud_sync_24)
                    .into(binding.smallWeatherIcon)

            }
        }
    }

    /** Remove any previously displayed data from the weather related views */
    private fun clearWeatherUI(forecastRetrievableStatus: ForecastRetrievableStatus) {
        photoShootLocationViewModel.isWeatherDataCurrent()

        // If the forecast can't be retrieved allow the user to see additional
        // details explaining why that is the case.
        when (forecastRetrievableStatus) {
            ForecastRetrievableStatus.TO_FAR_AHEAD -> {
                weatherIssueDetails = getString(R.string.dialog_weather_beyond_forecast_message)
            }
            ForecastRetrievableStatus.NOT_SET -> {
                weatherIssueDetails = getString(R.string.dialog_weather_date_not_set_message)
            }
            ForecastRetrievableStatus.IN_PAST -> {
                weatherIssueDetails = getString(R.string.dialog_weather_past_date_message)
            }
        }

        // Set the weather views to their placeholder values and images
        binding.highTempLabel.text = getString(R.string.high_temp_placeholder)
        binding.lowTempLabel.text = getString(R.string.low_temp_placeholder)
        binding.windLabel.text = getString(R.string.wind_placeholder)

        binding.precipitationLabel.text = getString(R.string.precipitation_placeholder)

        binding.weatherDescriptionLabel.text = String()

        binding.mainWeatherIcon.setImageResource(R.drawable.ic_no_weather_data)
        binding.smallWeatherIcon.setImageResource(R.drawable.ic_no_weather_data)

        binding.weatherNoteDetail.isClickable = true
        binding.locationImage.doOnLayout {
            binding.weatherNoteDetail.alpha = 1.0F
            binding.weatherNoteInfoImage.alpha = 1.0F
        }
    }

    /** Display the next photo shoot data */
    private fun setNextPhotoShootData() {

        photoShootLocationViewModel.photoShootLocation.value?.photoShootOverview?.let { photoShootOverview ->
            var nextPhotoShootDate = ""

            if (photoShootOverview.nextPhotoShootDate != 0L) {
                // Format the date appropriately. This is based on the answer at
                // https://developer.android.com/reference/android/text/format/DateFormat#summary
                nextPhotoShootDate = DateFormat.getDateFormat(context)
                    .format(Date(photoShootOverview.nextPhotoShootDate))
            }

            val nextPhotoShootDateFormated =
                String.format(getString(R.string.next_photo_shoot_format), nextPhotoShootDate)
            binding.nextPhotoShootLabelText.text = nextPhotoShootDateFormated
            setChipState(binding.groupChip, photoShootOverview.nextPhotoShootHasGroup)
            setChipState(binding.childChip, photoShootOverview.nextPhotoShootHasChild)
            setChipState(binding.petChip, photoShootOverview.nextPhotoShootHasPet)
        }
    }

    /** Open the editor that allows the user to edit the next photo shoot details and pass in the
     * supplied values as the initial values in the editor. */
    private fun openNextPhotoShootEditor(
        groupChecked: Boolean = false,
        childChecked: Boolean = false,
        petChecked: Boolean = false
    ) {
        binding.extendedFabMotionLayout.transitionToStart()

        val bundle = Bundle()

        photoShootLocationViewModel.photoShootLocation.value?.photoShootOverview?.let {

            // Pass in the currently set date for the next photo shoot
            bundle.putLong(
                NEXT_PHOTO_SHOOT_DATE,
                it.nextPhotoShootDate
            )

            // If the user is opening the editor by clicking on a chip then toggle the status of
            // that chip
            if (groupChecked) {
                bundle.putBoolean(GROUP_CONDITION_CHECKED, !it.nextPhotoShootHasGroup)
            } else {
                bundle.putBoolean(GROUP_CONDITION_CHECKED, it.nextPhotoShootHasGroup)
            }

            if (childChecked) {
                bundle.putBoolean(CHILD_CONDITION_CHECKED, !it.nextPhotoShootHasChild)
            } else {
                bundle.putBoolean(CHILD_CONDITION_CHECKED, it.nextPhotoShootHasChild)
            }

            if (petChecked) {
                bundle.putBoolean(PET_CONDITION_CHECKED, !it.nextPhotoShootHasPet)
            } else {
                bundle.putBoolean(PET_CONDITION_CHECKED, it.nextPhotoShootHasPet)
            }

        }

        // Open the editor and pass in the current next photo shoot data
        val editNextPhotoShootModalSheet = EditNextPhotoShootModalSheet()
        editNextPhotoShootModalSheet.arguments = bundle
        editNextPhotoShootModalSheet.show(childFragmentManager, NEXT_PHOTO_SHOOT_MODAL_TAG)
    }

    /** Set the state of the [chip] to the selected [checked] state */
    private fun setChipState(chip: Chip, checked: Boolean) {
        chip.isCheckable = true
        chip.isChecked = checked
        chip.isCheckable = false
    }

    /** Update the image that is displayed for the photo shoot location */
    private fun setNewImage(newPath: String) {
        if (newPath.isNotBlank()) {
            binding.locationImage.doOnLayout {
                Picasso.get()
                    .load(
                        File(newPath)
                    )
                    .placeholder(R.drawable.lighthouse_placeholder_low_quality)
                    .resize(
                        binding.locationImage.width,
                        binding.locationImage.height
                    )
                    .centerCrop()
                    .into(binding.locationImage)
            }
        }
    }

    /** Co-ordinate the users scrolling with minimizing all the views that are not related to the
     * prep for this photo shoot location */
    private fun coordinateMotion() {
        val appBarLayout: AppBarLayout = binding.appBar
        val motionLayout: MotionLayout = binding.motionLayout
        val scrollPositionBuffer = 0.86f

        // This logic is based off the logic from class on motion layout at
        // https://github.com/udacity/motionlayout and
        // https://microeducate.tech/problem-with-multiple-transitions-in-android-motionlayout/
        val listener = AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            // Determine the amount of scrolling the user has performed
            var seekPosition =
                (-verticalOffset * scrollPositionBuffer) / appBarLayout.totalScrollRange.toFloat()
            seekPosition = (seekPosition)

            // Only start minimizing the views after the user has scroll halfway
            if (seekPosition < 0.50f) {

                if (motionLayout.progress != 0f) {
                    motionLayout.progress = 0f
                }
            } else {

                // Account for the minimizing only starting after the user has scrolled
                // halfway through
                var positional = abs((0.50f - seekPosition) / 0.50f)

                // If the user is very close to finishing scrolling or just barely has started
                // scrolling set the motion layout scene position to the very end or the very beginning
                if (positional > 0.98) {
                    positional = 1.0f
                }

                if (positional < 0.02) {
                    positional = 0.0f
                }


                motionLayout.progress = positional
            }
        }

        appBarLayout.addOnOffsetChangedListener(listener)
    }

    /** Open the fragment that allows the user to edit the photo shoot location overview for this
     * photo shoot location */
    private fun showEditFragment(photoShootOverview: PhotoShootOverview) {
        val action =
            PhotoShootLocationFragmentDirections.actionPhotoShootLocationFragmentToNavEditPhotoShootLocation(
                photoShootOverview = photoShootOverview
            )
        findNavController().navigate(
            action
        )

        wasEditingPhotoShoot = true
    }

    /** Show the dialog to edit or create prep */
    private fun showEditPrepDialog(photoShootPrep: PhotoShootPrep?) {

        // If the user is creating new prep then create a prep item with no values set on it
        if (photoShootPrep == null) {
            sharedViewModel.setPrepData(
                PhotoShootPrep(
                    prepName = "",
                    conditions = HashSet<ConditionType>(),
                    id = 0,
                    photoShootId = photoShootLocationViewModel.photoShootLocation.value!!.photoShootOverview.photoShootLocationId
                )
            )
        } else {

            // If the user is editing an existing prep item then pass in the prep's details
            sharedViewModel.setPrepData(photoShootPrep)
        }
        val dialog = EditPrepItemDialog(R.id.nav_photo_shoot_location)

        dialog.show(childFragmentManager, PHOTO_SHOOT_LOCATION_TAG)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val watermarkTag = "GoogleWatermark"

        // Initialize the map to show the area around the photo shoot location
        map = googleMap

        // Move the Google logo to the top of the view so it isn't covered by the directions buttons
        // This is based off of the answer at https://stackoverflow.com/a/47506659
        val googleLogo: View =
            mapFragment.liteMap.requireView().findViewWithTag(watermarkTag)
        val glLayoutParams = googleLogo.layoutParams as RelativeLayout.LayoutParams
        glLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
        glLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0)
        glLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_START, 0)
        glLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
        glLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE)
        googleLogo.layoutParams = glLayoutParams

        if (photoShootLocationViewModel.photoShootLocation.value != null) {
            if (photoShootLocationViewModel.locationLatLng().latitude != 0.0) {

                // Add a marker at the photo shoot location. By adding a marker navigation buttons
                // will automatically be displayed
                addPhotoShootSavedLocationMarker(
                    photoShootLocationViewModel.locationLatLng()
                )

            }
        }
    }

    /** Add a marker at the provided [latLng] and center the camera on it */
    private fun addPhotoShootSavedLocationMarker(
        latLng: LatLng
    ) {
        val defaultZoomLevel = 14f

        map.clear()

        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                latLng,
                defaultZoomLevel
            )
        )

        map.addMarker(
            MarkerOptions().position(
                latLng
            )
        )

    }

    companion object {
        const val NEXT_PHOTO_SHOOT_MODAL_TAG = "ModalNextPhotoSheetBottomSheet"
        const val PHOTO_SHOOT_LOCATION_TAG = "PhotoShootLocationFragment"
    }
}