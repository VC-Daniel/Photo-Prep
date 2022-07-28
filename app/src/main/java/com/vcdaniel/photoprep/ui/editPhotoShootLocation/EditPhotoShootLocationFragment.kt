package com.vcdaniel.photoprep.ui.editPhotoShootLocation

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.RelativeLayout
import android.widget.SearchView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.util.Strings
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.CancelableCallback
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import com.vcdaniel.photoprep.BuildConfig
import com.vcdaniel.photoprep.R
import com.vcdaniel.photoprep.databinding.FragmentEditPhotoShootLocationBinding
import java.io.File
import java.io.IOException
import java.util.*


private const val TAG = "EDITPHOTOSHOOTFRAGMENT"

/** Denotes a request to turn on the location service */
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29

/** Denotes a request to access the users location */
private const val REQUEST_LOCATION_PERMISSION = 1

/** Used when taking the user to the app's settings screen */
private const val LOCATION_SETTINGS_REQUEST_CODE = 102

/** Allows the user to create or edit a photo shoot location. This page optionally uses the user's
 * location to display their current location to improve the experience of selecting the photo
 * shoot location by giving them a point of reference. The permissions logic is based on the code at
 * https://github.com/VC-Daniel/nd940-android-kotlin-c4-starter and Wander example app from the
 * Udacity Android Nano Developer Degree program */
class EditPhotoShootLocationFragment : Fragment(), OnMapReadyCallback {

    private lateinit var binding: FragmentEditPhotoShootLocationBinding

    /** A view model that is shared with a AddPhotoModalSheet which allows the user to specify a
     * photo that represents this photo shoot location */
    private val addPhotoSharedViewModel: AddPhotoModalViewModel by navGraphViewModels(R.id.nav_edit_photo_shoot_location) { defaultViewModelProviderFactory }

    // Watch for the user to enter a valid name for the photo shoot location before enabling saving
    private lateinit var requiredFieldTextWatcher: TextWatcher

    // Used to determine if the user has started editing the photo shoot location name. This is used
    // to only alert the user that they must supply a name after they have started typing and then
    // if they clear the required photo shoot location name
    private var editingStarted = false

    private lateinit var saveMenuItem: MenuItem

    private lateinit var viewModel: EditPhotoShootLocationViewModel

    /** Denotes if we are currently waiting for the users location to be provided */
    var requestingLocationUpdates = false

    /** A map for the user to select where the photo shoot location is */
    private lateinit var map: GoogleMap

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    /** Display why location permissions are requested and provides quick access to the
     * app's settings page */
    private lateinit var settingsSnackbar: Snackbar

    /** When the user's location has been determined display it on the map */
    private val locationCallback = object : LocationCallback() {

        override fun onLocationResult(locationResult: LocationResult) {
            updateMapUserLocation(locationResult.locations.lastOrNull())
        }
    }
    private val normalZoom = 16f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Retrieve the photo shoot location that is being edited. This is in part based on the
        // tutorial at https://www.section.io/engineering-education/safe-args-in-android/
        val args: EditPhotoShootLocationFragmentArgs by navArgs()

        viewModel = ViewModelProvider(
            this,
            EditPhotoShootLocationViewModel.EditPhotoShootLocationViewModelFactory(
                args.photoShootOverview,
                requireActivity().application
            )
        )[EditPhotoShootLocationViewModel::class.java]

        binding = FragmentEditPhotoShootLocationBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // Watch for the required photo shoot location name to be specified and only allow the user
        // to attempt to save this location once a name has been supplied
        // https://www.tutorialspoint.com/how-to-use-the-textwatcher-class-in-kotlin
        requiredFieldTextWatcher = object : TextWatcher {
            override fun afterTextChanged(text: Editable?) {
                editingStarted = true
                saveMenuItem.isEnabled = !text.isNullOrBlank()

                if (editingStarted && text.isNullOrBlank()) {
                    // Change the button color to show that it saving is disabled
                    saveMenuItem.icon.setTint(
                        getColor(
                            requireContext(),
                            androidx.appcompat.R.color.bright_foreground_disabled_material_dark
                        )
                    )
                    binding.locationNameText.error = getString(R.string.required_error_text)
                } else {
                    // Change the button color to show that it saving is enabled
                    saveMenuItem.icon.setTint(getColor(requireContext(), R.color.white))
                    binding.locationNameText.error = null
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        }

        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val settingsScheme = "package"

        /** If the user changes the main photo for this photo shoot location show the new image */
        addPhotoSharedViewModel.changePhoto.observe(viewLifecycleOwner) {
            if (it) {
                setNewImage(addPhotoSharedViewModel.photoPath.value!!)
                addPhotoSharedViewModel.changePhotoComplete()
            }
        }

        /** Display a modal sheet to allow the user to choose a new photo */
        binding.imageButton.setOnClickListener {
            val addPhotoModalSheet = AddPhotoModalSheet()
            addPhotoModalSheet.show(parentFragmentManager, AddPhotoModalSheet.TAG)
        }

        /** If the user denied location permissions give them the opportunity to go to the settings
         * page to enable location permissions. This is in part based on the logic at
         * https://github.com/VC-Daniel/nd940-android-kotlin-c4-starter */
        settingsSnackbar = Snackbar.make(
            binding.editPhotoShootLocationConstraint, R.string.permission_denied_explanation,
            Snackbar.LENGTH_LONG
        ).setAction(R.string.settings) {
            // This is in part based on the answer at
            // https://stackoverflow.com/questions/7910840/android-startactivityforresult-immediately-triggering-onactivityresult
            startActivityForResult(Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts(settingsScheme, BuildConfig.APPLICATION_ID, null)
            }, LOCATION_SETTINGS_REQUEST_CODE)
        }

        //
        // Obtain the SupportMapFragment and get notified when the map is ready to be used. This is
        // in part based off the logic found at:
        // https://stackoverflow.com/questions/63931552/how-to-use-google-maps-android-sdk-in-a-fragment
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Get a location provider client to obtain the users current location. This is used to
        // move the map's position to the users current location. This is based on the logic found
        // in the tutorial at:
        // https://developers.google.com/maps/documentation/android-sdk/current-place-tutorial
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        // adding on query listener for our search view. This is in part based on the tutorial at
        // https://www.geeksforgeeks.org/how-to-add-searchview-in-google-maps-in-android/
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Retrieve the entered value to search
                val location: String = binding.searchView.query.toString()

                // Used to store the results of the search
                var addressList: List<Address>? = null

                // Check if the user has actually entered a value to search
                if (location.isNotBlank()) {
                    val geocoder = Geocoder(context)
                    try {
                        // get the top result from the search
                        addressList = geocoder.getFromLocationName(location, 1)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    // Get the gps coordinates for the search result
                    if (!addressList.isNullOrEmpty()) {
                        val address: Address = addressList[0]
                        val latLng = LatLng(address.latitude, address.longitude)

                        // Animate the camera to the search results location
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, normalZoom))
                    } else {
                        // If no results were returned notify the user
                        Toast.makeText(
                            context,
                            getString(R.string.search_no_location_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        // Once the user has saved their changes return the the previous fragment
        viewModel.savePhotoShootComplete.observe(viewLifecycleOwner) { saveComplete ->
            if (saveComplete) {
                findNavController().popBackStack()
            }
        }

        // If the user is deleting this photo shoot location always exit to the list of photo shoot
        // locations so it doesn't attempt to return to the photoShootLocationFragment
        viewModel.deletePhotoShootComplete.observe(viewLifecycleOwner) { deletePhotoShootComplete ->
            if (deletePhotoShootComplete) {
                val action =
                    EditPhotoShootLocationFragmentDirections.actionNavEditPhotoShootLocationToNavPhotoShootLocations()
                findNavController().navigate(
                    action
                )
            }
        }

        // If available set the image to either the new image that the user has selected while editing the photo
        // shoot location or set it to the previously saved photo.
        if (viewModel.temporaryImagePath.isNotBlank()) {
            setNewImage(viewModel.temporaryImagePath, false)
        } else if (!viewModel.photoShootOverview.value?.mainImagePath.isNullOrBlank()) {
            setNewImage(viewModel.photoShootOverview.value?.mainImagePath!!, false)
        }
    }

    /** Set the main photo view to display the photo at the [newPath] amd delete any previous
     *  temporary images */
    private fun setNewImage(newPath: String, newTempImage: Boolean = true) {
        if (newPath.isNotBlank()) {
            if (newTempImage) {
                // Delete any previous temporary images
                if (viewModel.temporaryImagePath.isNotBlank()) {
                    viewModel.deleteImage(viewModel.temporaryImagePath)
                }
                viewModel.temporaryImagePath = newPath
            }
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

    /** Add the appropriate menu and initialize the save and delete menu items appropriately */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.editor, menu)

        saveMenuItem = menu.findItem(R.id.action_save_changes)
        initializeSaveEnabled()

        // If the user is editing an existing photo shoot then display the option to delete it
        if (viewModel.photoShootOverview.value?.photoShootLocationId != 0L) {
            menu.findItem(R.id.action_delete_photo_shoot_location).isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_save_changes) {
            if (viewModel.isReadyToSave()) {

                // If the user is replacing the main image delete the previous image
                if (viewModel.temporaryImagePath.isNotBlank()) {
                    viewModel.photoShootOverview.value?.let { photoShootOverview ->
                        if (photoShootOverview.mainImagePath.isNotBlank()) {
                            viewModel.deleteImage(photoShootOverview.mainImagePath)
                        }
                    }
                    viewModel.photoShootOverview.value!!.mainImagePath =
                        viewModel.temporaryImagePath
                }
                viewModel.savePhotoShootOverviewData()
            } else {
                // If no name or location has been specified then don't proceed with saving the
                // photo shoot location and instead inform the user on why they weren't able to save
                val builder: AlertDialog.Builder? = activity?.let {
                    AlertDialog.Builder(it)
                }
                builder?.setMessage(R.string.dialog_cannot_save_photoshoot_message)
                    ?.setTitle(R.string.dialog_cannot_save_photoshoot_title)
                    ?.setPositiveButton(getString(R.string.ok)) { _, _ -> }
                builder?.create()?.show()
            }
            return true
        } else if (item.itemId == R.id.action_delete_photo_shoot_location) {
            val builder: AlertDialog.Builder? = activity?.let {
                AlertDialog.Builder(it)
            }

            // Verify that the user wants to delete the photo shoot location and everything
            // associated with it
            builder?.setMessage(R.string.dialog_delete_photoshoot_message)
                ?.setTitle(R.string.dialog_delete_photoshoot_title)
                ?.setPositiveButton(R.string.dialog_delete_photoshoot_continue) { _, _ ->
                    viewModel.photoShootOverview.value?.let { photoShootOverview ->

                        // Delete the main photo if there is one
                        if (photoShootOverview.mainImagePath.isNotBlank()) {
                            viewModel.deleteImage(photoShootOverview.mainImagePath)
                        }

                        // Delete the temporary photo if there is one
                        if (viewModel.temporaryImagePath.isNotBlank()) {
                            if (photoShootOverview.mainImagePath.isNotBlank()) {
                                viewModel.deleteImage(viewModel.temporaryImagePath)
                            }
                        }

                        // Cascade delete all the data associated with this photo shoot
                        viewModel.deletePhotoShoot()
                    }
                }
                ?.setNeutralButton(R.string.dialog_delete_photoshoot_cancel, null)
            builder?.create()?.show()
            return true
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()

        // Dismiss the snackbar that displays errors related to permissions and stop requesting
        // location updates
        settingsSnackbar.dismiss()
        stopLocationUpdates()
    }

    override fun onPause() {
        super.onPause()

        // Don't listen for changes to the name or location when the app is not in the foreground
        binding.locationNameText.removeTextChangedListener(requiredFieldTextWatcher)
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()

        // Set if the save button should be enabled and resume listening for editing of the
        // location's name as well as the changes to the user's location
        if (::saveMenuItem.isInitialized) {
            initializeSaveEnabled()
        }

        binding.locationNameText.addTextChangedListener(requiredFieldTextWatcher)
        if (requestingLocationUpdates) enableMyLocation()
    }

    /** Determine whether or not the save button should be enabled and set the menu item's visual
     * properties appropriately */
    private fun initializeSaveEnabled() {
        if (Strings.isEmptyOrWhitespace(viewModel.photoShootOverview.value?.photoShootLocationName)) {
            saveMenuItem.isEnabled = false
            saveMenuItem.icon.setTint(
                getColor(
                    requireContext(),
                    androidx.appcompat.R.color.bright_foreground_disabled_material_dark
                )
            )
        } else {
            saveMenuItem.isEnabled = true
            saveMenuItem.icon.setTint(getColor(requireContext(), R.color.white))
        }
    }

    /** Check if location permissions have been granted */
    private fun isPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Get the current position of the user and focus the map on it once it has been determined */
    fun updateMapUserLocation(lastKnownLocation: Location?) {
        val zoomLevel = 10f
        // Display the user's location once it is available
        if (lastKnownLocation != null) {
            requestingLocationUpdates = false
            stopLocationUpdates()
            if (viewModel.locationLatLng().latitude == 0.0) {
                val lastKnownLongLat =
                    lastKnownLocation.let {
                        LatLng(
                            lastKnownLocation.latitude, lastKnownLocation.longitude
                        )
                    }
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        lastKnownLongLat,
                        zoomLevel
                    )
                )
            }
        } else {
            // If we aren't already watching for updates request location updates
            if (!requestingLocationUpdates) {
                startLocationUpdates()
            }
        }
    }

    /**
     * Request the user's location so we can focus on it on the map. This is based on the code from
     * the documentation at https://developer.android.com/training/location/change-location-settings
     * Call this once permission has been granted otherwise it will through a MissingPermission error
     */
    @Throws(SecurityException::class)
    private fun startLocationUpdates() {
        val locationRequest =
            LocationRequest().setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        requestingLocationUpdates = true
    }

    /**
     * Stop requesting updates on the user's location. This is based off the documentation at
     * https://developer.android.com/training/location/request-updates#updates
     */
    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val watermarkTag = "GoogleWatermark"

        // Initialize the map with the supplied map now that it is ready
        map = googleMap

        // Move the Google logo to the top of the view so it isn't covered by the search edit text
        // This is based off of the answer at https://stackoverflow.com/a/47506659
        val googleLogo: View = binding.map.findViewWithTag(watermarkTag)
        val glLayoutParams = googleLogo.layoutParams as RelativeLayout.LayoutParams
        glLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
        glLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0)
        glLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_START, 0)
        glLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
        glLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE)
        googleLogo.layoutParams = glLayoutParams

        // Disable the map toolbar that displays various controls such as a button to get directions
        // to the location. This is based on the answer at https://stackoverflow.com/a/30354428
        map.uiSettings.isMapToolbarEnabled = false

        // Allow the user to select a POI
        setPoiClick(map)

        // Allow the user to select a custom location at any point
        setCustomLocationMapClick(map)

        if (viewModel.locationLatLng().latitude != 0.0) {
            // Once the animation has finished or been canceled place the marker at the
            // selected co-ordinates. This is based on the documentation at
            // https://developers.google.com/maps/documentation/android-sdk/views#maps_android_camera_and_view_common_map_movements-kotlin
            // as well as the answer at https://stackoverflow.com/questions/33751753/how-to-implement-googlemap-cancelablecallback
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                viewModel.locationLatLng(),
                normalZoom
            ),
                object : CancelableCallback {
                    override fun onFinish() {
                        addPhotoShootSavedLocationMarker(false)
                    }

                    override fun onCancel() {
                        addPhotoShootSavedLocationMarker(false)
                    }
                })
        }

        // Request location permissions to focus the map on the user's current location
        enableMyLocation()
    }

    /** Add a marker to the selected photo shoot location and animate to the location if
     * [centerOnPin] is true */
    fun addPhotoShootSavedLocationMarker(centerOnPin: Boolean = true) {
        map.clear()
        viewModel.enableSaving.value = true

        if (centerOnPin) {
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    viewModel.locationLatLng(),
                    map.cameraPosition.zoom
                )
            )
        }

        map.addMarker(
            MarkerOptions().position(
                viewModel.locationLatLng()
            )
                .snippet(viewModel.photoShootOverview.value?.locationTitle)
                .title(getString(R.string.photo_shoot_marker_title))
        )?.showInfoWindow()
    }

    /**
     * After a POI is selected add a marker and enable saving the photo shoot location.
     */
    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            viewModel.setLocation(poi)
            addPhotoShootSavedLocationMarker()
        }
    }

    /**
     * When the user selects any point on the map display a marker at the selected location.
     * Now that a location has been selected add a marker and enable saving the photo shoot location.
     * This logic is based off of the Wander example app from the Udacity Android Nano Developer
     * Degree program
     */
    private fun setCustomLocationMapClick(map: GoogleMap) {
        map.setOnMapClickListener { latLng ->
            val snippet = String.format(
                Locale.getDefault(),
                getString(R.string.gps_location_formatting),
                latLng.latitude,
                latLng.longitude
            )

            viewModel.setLocation(
                PointOfInterest(
                    latLng,
                    getString(R.string.photo_shoot_marker_title),
                    snippet
                )
            )

            addPhotoShootSavedLocationMarker()

        }
    }

    /** Request location permissions and if provided focus the map on the users current location
     * Only do this after verifying permissions have been granted otherwise it will throw a
     * MissingPermission error. This is in part based off the logic at
     * https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-throws/ */
    @Throws(SecurityException::class)
    private fun enableMyLocation() {
        if (!isPermissionGranted()) {
            requestPermissions(
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            // Verify that the location service is turned on
            checkDeviceLocationServiceStatus()
            // Provide the user with the option to focus the map on their location
            map.isMyLocationEnabled = true

            startLocationUpdates()
        }
    }

    /*
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationServiceStatus(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        // If the snackbar was displaying the option to go to the settings hide it as we verify if
        // permissions have been granted
        if (this::settingsSnackbar.isInitialized) {
            settingsSnackbar.dismiss()
        }

        if (!map.isMyLocationEnabled) {
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            val settingsClient = LocationServices.getSettingsClient(requireActivity())
            val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

            // If permissions have been denied then notify the user that this permission is required
            // for the app to function properly
            locationSettingsResponseTask.addOnFailureListener { exception ->
                if (exception is ResolvableApiException && resolve) {
                    try {
                        // If an activity has been supplied to resolve this then start it

                        startIntentSenderForResult(
                            exception.resolution.intentSender,
                            REQUEST_TURN_DEVICE_LOCATION_ON,
                            null,
                            0,
                            0,
                            0,
                            null
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        Log.d(TAG, getString(R.string.location_Settings_error) + sendEx.message)
                    }
                } else {
                    if (resolve) {
                        settingsSnackbar.show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.location_enable_explanation),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            locationSettingsResponseTask.addOnCanceledListener {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.location_enable_explanation),
                    Toast.LENGTH_SHORT
                ).show()
            }

            // If the required permissions have not been added then display the snackbar
            locationSettingsResponseTask.addOnCompleteListener {
                if (!it.isSuccessful) {
                    settingsSnackbar.show()
                }
            }
        }
    }
}