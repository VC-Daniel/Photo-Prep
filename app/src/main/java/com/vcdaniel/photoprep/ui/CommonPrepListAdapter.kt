package com.vcdaniel.photoprep.ui

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vcdaniel.photoprep.CommonPrep
import com.vcdaniel.photoprep.databinding.CommonPrepListItemBinding
import kotlinx.android.synthetic.main.common_prep_list_item.view.*

/** Facilitates displaying all the common prep items in a recycler view. When the user clicks on a
 * common prep the supplied [onClickListener] is triggered. This logic is in part based on the
 * tutorial at https://www.techotopia.com/index.php/A_Kotlin_Android_RecyclerView_and_CardView_Tutorial*/
class CommonPrepListAdapter(private val onClickListener: OnClickListener) :
    ListAdapter<CommonPrep, CommonPrepListAdapter.ViewHolder>(DiffCallBack) {

    /** Define how to compare common prep */
    companion object DiffCallBack : DiffUtil.ItemCallback<CommonPrep>() {
        override fun areItemsTheSame(oldItem: CommonPrep, newItem: CommonPrep): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CommonPrep, newItem: CommonPrep): Boolean {
            return oldItem == newItem
        }
    }

    /** The view holder for a single common prep*/
    class ViewHolder(private var binding: CommonPrepListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // The common prep data is displayed from the commonPrep bound to the view
        fun bind(commonPrep: CommonPrep) {
            binding.prep = commonPrep
            binding.executePendingBindings()
        }
    }

    /** Create a view holder to display a common prep*/
    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        return ViewHolder(
            CommonPrepListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Used to determine if the user is scrolling the conditions or clicking on the common prep
        var mXOld = 0
        var mYOld = 0

        // Get the common prep that corresponds to this view holder
        val commonPrep = getItem(position)
        holder.itemView.setOnClickListener {
            onClickListener.onClick(commonPrep)
        }

        // Determine if the user is scrolling the conditions or if they are trying to click on the
        // common prep. This is in part based on the logic found in the answer at
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
                                onClickListener.onClick(commonPrep)
                                return false
                            }
                        }
                        return false
                    }
                })
            }

            // set the common prep to display an overview of
            bind(commonPrep)
        }
    }

    class OnClickListener(val clickListener: (commonPrep: CommonPrep) -> Unit) {
        fun onClick(commonPrep: CommonPrep) = clickListener(commonPrep)
    }
}