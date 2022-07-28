package com.vcdaniel.photoprep.com.vcdaniel.photoprep.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vcdaniel.photoprep.PhotoShootLocation
import com.vcdaniel.photoprep.R
import com.vcdaniel.photoprep.com.vcdaniel.photoprep.ui.PhotoShootLocationsAdapter.OnLongClickListener
import com.vcdaniel.photoprep.databinding.PhotoShootLocationImageListItemBinding
import com.vcdaniel.photoprep.databinding.PhotoShootLocationMapListItemBinding

const val VIEW_TYPE_IMAGE = 1
const val VIEW_TYPE_MAP = 2

/** Facilitates displaying thumbnails for all the photo shoot locations in a recycler view. When
 * the user clicks on a photo shoot location the supplied [onClickListener] is triggered or when
 * they long press then the supplied [OnLongClickListener] is triggered. This logic is in part based on the
 * tutorial at https://stackoverflow.com/questions/26245139/how-to-create-recyclerview-with-multiple-view-types*/
class PhotoShootLocationsAdapter(
    private val onClickListener: OnClickListener,
    private val onLongClickListener: OnLongClickListener
) :
    ListAdapter<PhotoShootLocation, PhotoShootLocationsAdapter.ViewHolder>(DiffCallBack) {

    // Stores if the user prefers to always show map images for the thumbnail or if images should be
    // used when they are available
    var preferMapImage: Boolean = false

    /** Determine the proper view type that should be used depending on if it is displaying a map
     * thumbnail or image thumbnail */
    override fun getItemViewType(position: Int): Int {
        val photoShootLocation = getItem(position)
        // If an image has been provided for the photo shoot location then use it
        return if (photoShootLocation.photoShootOverview.mainImagePath.isNotBlank()) {
            VIEW_TYPE_IMAGE
        } else {
            VIEW_TYPE_MAP
        }
    }

    /** Define how to compare photo shoot Locations */
    companion object DiffCallBack : DiffUtil.ItemCallback<PhotoShootLocation>() {
        override fun areItemsTheSame(
            oldItem: PhotoShootLocation,
            newItem: PhotoShootLocation
        ): Boolean {
            return oldItem.photoShootOverview.photoShootLocationId == newItem.photoShootOverview.photoShootLocationId

        }

        override fun areContentsTheSame(
            oldItem: PhotoShootLocation,
            newItem: PhotoShootLocation
        ): Boolean {
            return oldItem == newItem
        }
    }

    /** The view holder for a single photo shoot location */
    class ViewHolder(private var binding: ViewDataBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // The photo shoot location thumbnail and name are displayed from the photo shoot location
        // bound to the view
        fun bind(photoShootLocation: PhotoShootLocation, viewType: Int, preferMapImage: Boolean) {
            // Use the proper binding depending on if the user always prefers map thumbnails and if
            // an image has been provided for the photo shoot location
            if (preferMapImage || viewType == VIEW_TYPE_MAP) {
                (binding as PhotoShootLocationMapListItemBinding).photoShootOverview =
                    photoShootLocation.photoShootOverview
            } else {
                (binding as PhotoShootLocationImageListItemBinding).photoShootOverview =
                    photoShootLocation.photoShootOverview
            }

            binding.executePendingBindings()
        }

    }

    /** Create a view holder to display an overview of a photo shoot location */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(parent.context)

        // Get the users thumbnail image preference
        // This is based on the tutorial at https://developer.android.com/guide/topics/ui/settings/use-saved-values
        preferMapImage = sharedPreferences.getBoolean(
            parent.context.getString(R.string.use_map_preference_key),
            false
        )

        // Use the proper binding depending on if the user always prefers map thumbnails and if
        // an image has been provided for the photo shoot location
        if (preferMapImage || viewType == VIEW_TYPE_MAP) {
            return ViewHolder(
                PhotoShootLocationMapListItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        } else {
            return ViewHolder(
                PhotoShootLocationImageListItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // get the photo shoot location that corresponds to this view holder
        val photoShootLocation = getItem(position)

        // Set the onClickListener and onLongClickListener to be triggered and pass in the selected
        // photo shoot location
        holder.itemView.setOnClickListener {
            onClickListener.onClick(photoShootLocation)
        }

        holder.itemView.setOnLongClickListener {
            onLongClickListener.onLongClick(photoShootLocation)
            true

        }
        // set the photo shoot location to display an overview of
        holder.bind(photoShootLocation, holder.itemViewType, preferMapImage)
    }

    class OnClickListener(val clickListener: (photoShootLocation: PhotoShootLocation) -> Unit) {
        fun onClick(photoShootLocation: PhotoShootLocation) = clickListener(photoShootLocation)
    }

    class OnLongClickListener(val longClickListener: (photoShootLocation: PhotoShootLocation) -> Unit) {
        fun onLongClick(photoShootLocation: PhotoShootLocation) =
            longClickListener(photoShootLocation)
    }
}