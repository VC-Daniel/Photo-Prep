package com.vcdaniel.photoprep.ui

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vcdaniel.photoprep.PhotoShootPrep
import com.vcdaniel.photoprep.databinding.PhotoshootPrepListItemBinding
import kotlinx.android.synthetic.main.common_prep_list_item.view.*

/** Facilitates displaying all the photo shoot prep items for a photo shoot location in a recycler
 * view. When the user clicks or long clicks on a prep item the supplied [onClickListener] or
 * [onLongClickListener] is triggered. This logic is in part based on the tutorial at
 * https://www.techotopia.com/index.php/A_Kotlin_Android_RecyclerView_and_CardView_Tutorial*/
class PhotoShootPrepListAdapter(
    private val onClickListener: OnClickListener,
    private val onLongClickListener: OnLongClickListener
) :
    ListAdapter<PhotoShootPrep, PhotoShootPrepListAdapter.ViewHolder>(PhotoShootPrepDiffCallBack) {

    /** Define how to compare photo shoot prep */
    companion object PhotoShootPrepDiffCallBack : DiffUtil.ItemCallback<PhotoShootPrep>() {
        override fun areItemsTheSame(oldItem: PhotoShootPrep, newItem: PhotoShootPrep): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PhotoShootPrep, newItem: PhotoShootPrep): Boolean {
            return oldItem == newItem
        }
    }

    /** The view holder for a single photo shoot prep */
    class ViewHolder(private var binding: PhotoshootPrepListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // The photo shoot prep data is displayed from the photoShootPrep bound to the view
        fun bind(photoShootPrep: PhotoShootPrep) {
            binding.prep = photoShootPrep
            binding.executePendingBindings()
        }
    }

    /** Create a view holder to display a photo shoot prep */
    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        return ViewHolder(
            PhotoshootPrepListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Used to determine if the user is scrolling the conditions or clicking on the
        // photo shoot prep
        var mXOld = 0
        var mYOld = 0

        // Get the photo shoot prep that corresponds to this view holder
        val photoShootPrep = getItem(position)

        // Set the onClick and onLongClick to be triggered when the item is clicked
        holder.itemView.setOnClickListener {
            onClickListener.onClick(photoShootPrep)
        }

        holder.itemView.setOnLongClickListener {
            onLongClickListener.onLongClick(photoShootPrep)
            true
        }

        // Determine if the user is scrolling the conditions or if they are trying to click on the
        // photo shoot prep. This is in part based on the logic found in the answer at
        // https://stackoverflow.com/questions/16776687/onclicklistener-on-scrollview
        holder.run {
            with(itemView) {
                conditionScroll.setOnTouchListener(object : View.OnTouchListener {
                    override fun onTouch(v: View?, event: MotionEvent): Boolean {
                        val x = event.x.toInt()
                        val y = event.y.toInt()
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            mXOld = x
                            mYOld = y
                        } else if (event.action == MotionEvent.ACTION_UP) {
                            if (x == mXOld || y == mYOld) {
                                // If the user is clicking on a condition then open the prep editor
                                // rather then changing the checked status of the prep
                                onLongClickListener.onLongClick(photoShootPrep)
                                return true
                            }
                        }
                        return false
                    }
                })
            }

            // set the photo shoot prep to display
            bind(photoShootPrep)
        }
    }

    class OnClickListener(val clickListener: (photoShootPrep: PhotoShootPrep) -> Unit) {
        fun onClick(photoShootPrep: PhotoShootPrep) = clickListener(photoShootPrep)
    }

    class OnLongClickListener(val longClickListener: (photoShootPrep: PhotoShootPrep) -> Unit) {
        fun onLongClick(photoShootPrep: PhotoShootPrep) =
            longClickListener(photoShootPrep)
    }
}