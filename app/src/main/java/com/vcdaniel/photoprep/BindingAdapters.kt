package com.vcdaniel.photoprep

import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.squareup.picasso.Picasso
import com.vcdaniel.photoprep.com.vcdaniel.photoprep.ui.PhotoShootLocationsAdapter
import com.vcdaniel.photoprep.ui.CommonPrepListAdapter
import com.vcdaniel.photoprep.ui.PhotoShootPrepListAdapter
import kotlinx.android.synthetic.main.common_prep_list_item.view.*
import java.io.File

/** Set if the provided [imageView] should be visible or not based on if something is being edited
 * or being created which is determined by if the [id] has been assigned. For example if the option
 * to delete should be shown. */
@BindingAdapter("editingVisibility")
fun bindEditingVisibility(imageView: ImageView, id: Long) {
    if (id == 0L) {
        imageView.visibility = View.GONE
    } else {
        imageView.visibility = View.VISIBLE
    }
}

/** Bind the common prep [prepData] to the [prepRecyclerView] */
@BindingAdapter("commonPrepListData")
fun bindCommonPrepRecyclerView(prepRecyclerView: RecyclerView, prepData: List<CommonPrep>?) {
    // Make sure the latest data is shown in the list. This is partially based off the logic in this
    // tutorial: https://peaku.co/questions/1033-listadapter-no-actualiza-el-elemento-en-recyclerview
    val adapter = prepRecyclerView.adapter as CommonPrepListAdapter
    adapter.submitList(prepData)
}

/** Bind the photo shoot prep [prepData] to the [prepRecyclerView] */
@BindingAdapter("photoShootPrepListData")
fun bindPhotoShootPrepRecyclerView(
    prepRecyclerView: RecyclerView,
    prepData: List<PhotoShootPrep>?
) {
    val adapter = prepRecyclerView.adapter as PhotoShootPrepListAdapter
    adapter.submitList(prepData?.toList())
}

/** bind the [photoShootData] to the [photoShootLocationsRecyclerView] */
@BindingAdapter("photoShootListData")
fun bindPhotoShootLocationRecyclerView(
    photoShootLocationsRecyclerView: RecyclerView,
    photoShootData: List<PhotoShootLocation>?
) {
    val adapter = photoShootLocationsRecyclerView.adapter as PhotoShootLocationsAdapter
    adapter.submitList(photoShootData)
}

/** Display the photo shoot image for the provided [photoShootOverview] in the
 * provided [thumbnailImageView].  */
@BindingAdapter("photoShootPhotoThumbnailImage")
fun bindPhotoShootPhotoThumbnailImage(
    thumbnailImageView: ImageView,
    photoShootOverview: PhotoShootOverview
) {
    // Load the image into the view once it has actually been laid out otherwise Picasso will not be
    // able to determine the dimensions of the ImageView
    if (thumbnailImageView.height > 0) {
        displayImageThumbnail(thumbnailImageView, photoShootOverview)
    } else {
        thumbnailImageView.doOnLayout {
            displayImageThumbnail(thumbnailImageView, photoShootOverview)
        }
    }
}

/** Display a static map image for the provided [photoShootOverview] in the
 * provided [thumbnailImageView].  */
@BindingAdapter("photoShootMapThumbnailImage")
fun bindPhotoShootMapThumbnailImage(
    thumbnailImageView: ImageView,
    photoShootOverview: PhotoShootOverview
) {
    if (thumbnailImageView.height > 0) {
        displayMapThumbnail(thumbnailImageView, photoShootOverview)
    } else {
        thumbnailImageView.doOnLayout {
            displayMapThumbnail(thumbnailImageView, photoShootOverview)
        }
    }
}

/** Display the main photo for the provided [photoShootOverview] in the [thumbnailImageView] */
private fun displayImageThumbnail(
    thumbnailImageView: ImageView,
    photoShootOverview: PhotoShootOverview
) {

    // Load the provided image using picasso and resize it the size appropriately to the size of
    // the ImageView it is displayed in. While the image is being loaded display a placeholder image.
    Picasso.get()
        .load(
            File(photoShootOverview.mainImagePath)
        )
        .placeholder(R.drawable.lighthouse_placeholder_low_quality)
        .resize(
            thumbnailImageView.width,
            thumbnailImageView.height
        )
        .centerCrop()
        .into(thumbnailImageView)

    // Set the content description to ensure those using a screen reader know what is being displayed
    thumbnailImageView.contentDescription = String.format(
        thumbnailImageView.context.getString(R.string.photo_desc_format),
        photoShootOverview.photoShootLocationName
    )
}

/** Display the static map for the provided [photoShootOverview] in the [thumbnailImageView] */
private fun displayMapThumbnail(
    thumbnailImageView: ImageView,
    photoShootOverview: PhotoShootOverview
) {
    Picasso.get()
        .load(photoShootOverview.getStaticMapUrl(thumbnailImageView.context.getString(R.string.GOOGLE_API_KEY)))
        .placeholder(R.drawable.lighthouse_place_holder_portrait_low)
        .resize(
            thumbnailImageView.width,
            thumbnailImageView.height
        )
        .centerCrop(Gravity.BOTTOM)
        .into(thumbnailImageView)

    thumbnailImageView.contentDescription = String.format(
        thumbnailImageView.context.getString(R.string.map_desc_format),
        photoShootOverview.photoShootLocationName
    )
}

// Using multiple [parameters in a binding adapter is in part based on the logic found in the
// tutorial at https://medium.com/@vlonjatgashi/bindingadapter-with-multiple-attributes-in-android-data-binding-3e872caf1ef8
/** Properly display the [chip]'s checked status. This is based on if the [conditionType]
 * represented by the chip has been specified as a condition to trigger the prep which is stored in
 * the list of [conditions]. */
@BindingAdapter("conditions", "conditionType")
fun bindConditionChipChecked(
    chip: Chip,
    conditions: HashSet<ConditionType>,
    conditionType: ConditionType
) {
    chip.isChecked = conditions.contains(conditionType)
}

/** Display or hide the [scrollView] that contains the conditions based on
 * the [numberOfConditions]. */
@BindingAdapter("shouldBeVisible")
fun shouldScrollBeVisible(scrollView: HorizontalScrollView, numberOfConditions: Int) {
    if (numberOfConditions == 0) {
        scrollView.visibility = View.GONE
    } else {
        scrollView.visibility = View.VISIBLE
    }
}

/** Display the [prepConditions] for the prep item in the [conditionsGroup] */
@BindingAdapter("prepConditions")
fun bindConditionalChips(conditionsGroup: ChipGroup, prepConditions: HashSet<ConditionType>) {
    // First remove all the chips in the group just in case this is a recycled view.
    conditionsGroup.removeAllViews()

    //  In order to maintain the same order as
    //  the dialog go through the enums rather then the prepConditions.
    ConditionType.values().forEach { conditionType ->
        if (prepConditions.contains(conditionType)) {
            val condition = Condition(conditionType, conditionsGroup.context)

            // Display the conditions assigned to this prep.
            addConditionChip(
                condition.conditionName,
                AppCompatResources.getDrawable(
                    conditionsGroup.context,
                    condition.iconInt
                ),
                conditionsGroup
            )
        }
    }
}

/** Add a chip that represents the [condition] using the [conditionIcon] and add it to the
 * ChipGroup within this items [holder].*/
private fun addConditionChip(condition: String, conditionIcon: Drawable?, holder: View) {
    // Set the style of the chip using the logic found in the answer at
    // https://stackoverflow.com/a/58247341
    val chip = Chip(
        holder.context,
        null,
        R.attr.Widget_MyApp_Chip_Choice
    )

    chip.id = ViewCompat.generateViewId()
    chip.contentDescription = condition
    chip.chipIcon = conditionIcon
    chip.iconEndPadding = holder.context.resources.getDimension(R.dimen.chip_margin)
    chip.iconStartPadding = holder.context.resources.getDimension(R.dimen.chip_margin)
    chip.textEndPadding =
        holder.context.resources.getDimension(R.dimen.chip_icon_only_text_margin)
    chip.textStartPadding =
        holder.context.resources.getDimension(R.dimen.chip_icon_only_text_margin)
    chip.isChipIconVisible = true
    chip.isChecked = true
    chip.isClickable = false

    holder.conditionChipGroup.addView(chip)
}