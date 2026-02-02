package com.transitolibre.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.transitolibre.R
import com.transitolibre.data.entity.Stop

class StopSearchAdapter(
    private val onStopClick: (Stop) -> Unit
) : ListAdapter<Stop, StopSearchAdapter.ViewHolder>(StopDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stop_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStopName: TextView = itemView.findViewById(R.id.tvStopName)
        private val tvStopCode: TextView = itemView.findViewById(R.id.tvStopCode)

        fun bind(stop: Stop) {
            tvStopName.text = stop.name
            stop.code?.let { code ->
                tvStopCode.visibility = View.VISIBLE
                tvStopCode.text = code
            } ?: run {
                tvStopCode.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onStopClick(stop)
            }
        }
    }

    class StopDiffCallback : DiffUtil.ItemCallback<Stop>() {
        override fun areItemsTheSame(oldItem: Stop, newItem: Stop): Boolean {
            return oldItem.stopId == newItem.stopId
        }

        override fun areContentsTheSame(oldItem: Stop, newItem: Stop): Boolean {
            return oldItem == newItem
        }
    }
}
