package com.vcdaniel.photoprep.ui.editPhotoShootLocation

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.navigation.navGraphViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vcdaniel.photoprep.BuildConfig
import com.vcdaniel.photoprep.R
import com.vcdaniel.photoprep.databinding.SheetDialogAddPhotoBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/** Facilitates the user adding a picture as the main photo for a photo shoot location by either
 * taking a photo or selecting one of their existing photos. */
class AddPhotoModalSheet : BottomSheetDialogFragment() {

    // Get the view model
    private val viewModel: AddPhotoModalViewModel by navGraphViewModels(R.id.nav_edit_photo_shoot_location) { defaultViewModelProviderFactory }

    private lateinit var binding: SheetDialogAddPhotoBinding

    /** Launches an external app to take capture an image with the devices camera */
    private val takePictureIntent =
        Intent(MediaStore.ACTION_IMAGE_CAPTURE)

    /** Launches an exeternal app to allow the user to select an image from the devices storage */
    private val choosePictureIntent = Intent(Intent.ACTION_GET_CONTENT)

    /** Facilitates working with device permissions, capturing a photo and retrieving files
     * from device storage */
    private lateinit var packageManager: PackageManager

    /** Start an activity that will return a result which includes a status to indicate if the user
     * wishes to save the image. */
    private var startForResult: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {

                // Save the image the user captured if the result is marked as successful
                viewModel.savePath()
            }
        }

    /** Launch an activity to allow the user to choose an image and receive the result which
     * contains the selected image. */
    private var startImagePickerForResult: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data.let { resultIntent ->
                    if (resultIntent != null && resultIntent.data != null) {

                        // Copy the selected image locally
                        createFileFromContentUri(resultIntent.data!!)
                        viewModel.savePath()
                    }
                }
            }
        }

    /** Request permissions to use the device camera and if the user grants camera permission then
     * launch the intent to capture a photo */
    private var changeCameraPermissionsLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                dispatchTakePictureIntent()
            }
        }

    /** Request permissions to select a photo from the devices memory and if the user grants the
     * permission then launch the select a photo */
    private var changeFileReadPermissionsLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                chooseGalleryImage()
            }
        }

    /** Request the permission to use the device camera or if it already granted then launch the
     * intent to take a photo. This is based on the tutorial at
     * https://developer.android.com/training/permissions/requesting#allow-system-manage-request-code */
    private val requestCameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                dispatchTakePictureIntent()
            } else {

                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied.
                val builder: AlertDialog.Builder? = activity?.let {
                    AlertDialog.Builder(it)
                }
                builder?.setMessage(R.string.dialog_camera_permission_needed)
                    ?.setTitle(R.string.dialog_camera_permission_needed_title)
                    ?.setPositiveButton(getString(R.string.settings)) { _, _ ->

                        // Give the use the opportunity to go to the settings to grant the permission
                        val permissionsIntent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts(settingsScheme, BuildConfig.APPLICATION_ID, null)
                        )
                        changeCameraPermissionsLauncher.launch(permissionsIntent)
                    }
                    ?.setNeutralButton(getString(R.string.decline_granting_permission)) { _, _ -> }
                builder?.create()?.show()
            }
        }

    /** Request the permission to read media files in order to select an existing photo on the
     * device or if it is already granted then launch the intent to choose a photo. This is based
     * on the tutorial at
     * https://developer.android.com/training/permissions/requesting#allow-system-manage-request-code */
    private val requestFileReadPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                chooseGalleryImage()
            } else {

                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied.
                val builder: AlertDialog.Builder? = activity?.let {
                    AlertDialog.Builder(it)
                }
                builder?.setMessage(R.string.dialog_read_permission_needed)
                    ?.setTitle(R.string.dialog_read_permission_needed_title)
                    ?.setPositiveButton(getString(R.string.settings)) { _, _ ->

                        val permissionsIntent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts(settingsScheme, BuildConfig.APPLICATION_ID, null)
                        )
                        changeFileReadPermissionsLauncher.launch(permissionsIntent)
                    }
                    ?.setNeutralButton(getString(R.string.decline_granting_permission)) { _, _ -> }
                builder?.create()?.show()
            }
        }

    private val settingsScheme = "package"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SheetDialogAddPhotoBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this

        // Expand the sheet by default. This is based on the logic at
        // https://stackoverflow.com/a/35976745
        val bottomSheet: FrameLayout = binding.standardBottomSheet
        val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        packageManager = requireContext().packageManager

        // If the device has a camera then enable the button to initiate capturing a new photo
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {

            // If the permissions have already been granted then launch the intent to take a photo
            // otherwise request the necessary permission
            binding.cameraButton.setOnClickListener {
                if (isCameraPermissionGranted()) {
                    dispatchTakePictureIntent()
                } else {
                    requestCameraPermission()
                }
            }
        } else {
            binding.cameraButton.isEnabled = false
        }

        // If the permissions have already been granted then launch the intent to select a photo
        // otherwise request the necessary permission
        binding.galleryButton.setOnClickListener {
            if (isFileReadPermissionGranted()) {
                chooseGalleryImage()
            } else {
                requestFileReadPermissions()
            }
        }

        // Whenever the path to the photo for this photo shoot location changes save the new path
        // and dismiss the modal sheet so the user can return to editing the photo shoot location
        viewModel.photoPath.observe(this) {
            if (it.isNotBlank()) {
                viewModel.changePhotoRequested()
                dismiss()
            }
        }

        return binding.root
    }

    /** Returns if the camera permission has been granted */
    private fun isCameraPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Returns if the file read permission has been granted */
    private fun isFileReadPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun chooseGalleryImage() {
        // Set the file type of what kind of file we want to allow the user to choose. This is based
        // on the tutorial at  https://www.geeksforgeeks.org/how-to-select-an-image-from-gallery-in-android/
        choosePictureIntent.type = "image/*"

        // Launch the image picker to allow the user to select a photo
        startImagePickerForResult.launch(choosePictureIntent)
    }

    /** Create an image file to store a copy of the selected or new photo. This is based on the
     * tutorial at https://developer.android.com/training/camera/photobasics#TaskPath
     */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name that is unique
        val dateFormat = "yyyyMMdd_HHmmss"
        val photoNamePrefix = "JPEG"
        val photoNameSuffix = ".jpg"

        val timeStamp: String = SimpleDateFormat(dateFormat).format(Date())
        val storageDir: File =
            requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "${photoNamePrefix}_${timeStamp}_",
            photoNameSuffix,
            storageDir
        ).apply {
            // Save the path to the file
            viewModel.currentAbsolutePhotoPath = absolutePath
        }
    }

    /** Copy the photo locally. This is based on the answer at https://stackoverflow.com/a/71309183 */
    private fun createFileFromContentUri(fileUri: Uri) {
        val selectedFileStream: InputStream? =
            requireActivity().contentResolver.openInputStream(fileUri)

        if (selectedFileStream != null) {
            copyStreamToFile(selectedFileStream, createImageFile())
        }

        selectedFileStream?.close()
    }

    // Copy the selected photos data to the app storage. This is based off of the logic in the
    // answer at https://stackoverflow.com/a/71309183
    private fun copyStreamToFile(inputStream: InputStream, outputFile: File) {
        val bufferSize = 6 * 1024

        inputStream.use { input ->
            val outputStream = FileOutputStream(outputFile)
            outputStream.use { output ->
                val buffer = ByteArray(bufferSize)
                while (true) {
                    val byteCount = input.read(buffer)
                    if (byteCount < 0) break
                    output.write(buffer, 0, byteCount)
                }
                output.flush()
            }
        }
    }

    /** launch the intent to take a photo and pass in the path to save the photo. This is based on
     * the tutorial at
     * https://developer.android.com/training/permissions/requesting#allow-system-manage-request-code */
    private fun dispatchTakePictureIntent() {
        val authority = "com.vcdaniel.photoprep.fileprovider"

        takePictureIntent.also {
            // Create the File where the photo should go
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                // Error occurred while creating the File
                Log.e(
                    TAG,
                    String.format(
                        getString(R.string.creating_temp_photo_file_error),
                        ex.message.toString()
                    )
                )
                null
            }

            // Continue only if the File was successfully created
            photoFile?.also {
                viewModel.photoURI = FileProvider.getUriForFile(
                    requireContext(),
                    authority,
                    it
                )

                // Launch the intent to take a picture and provide the path were it should be stored
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, viewModel.photoURI)
                startForResult.launch(takePictureIntent)
            }
        }
    }

    /** Request camera permissions if they are not already granted. */
    @Throws(SecurityException::class)
    private fun requestCameraPermission() {
        if (!isCameraPermissionGranted()) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /** Request file read permissions if they are not already granted. */
    private fun requestFileReadPermissions() {
        if (!isFileReadPermissionGranted()) {
            requestFileReadPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    companion object {
        const val TAG = "ModalAddPhotoSheet"
    }
}