package com.transitolibre.ui.detail

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.transitolibre.R
import com.transitolibre.data.dao.DepartureInfo

class DepartureAdapter : ListAdapter<DepartureInfo, DepartureAdapter.ViewHolder>(DepartureDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_departure, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRouteBadge: TextView = itemView.findViewById(R.id.tvRouteBadge)
        private val tvRouteName: TextView = itemView.findViewById(R.id.tvRouteName)
        private val tvHeadsign: TextView = itemView.findViewById(R.id.tvHeadsign)
        private val tvArrivalTime: TextView = itemView.findViewById(R.id.tvArrivalTime)

        fun bind(departure: DepartureInfo) {
            // Route badge
            tvRouteBadge.text = departure.routeShortName ?: "?"

            // Set badge color
            departure.routeColor?.let { colorHex ->
                try {
                    val color = Color.parseColor("#$colorHex")
                    val background = tvRouteBadge.background as? GradientDrawable
                    background?.setColor(color)
                } catch (_: Exception) { }
            }

            // Route name
            tvRouteName.text = departure.routeLongName ?: departure.routeShortName ?: "Ligne inconnue"

            // Headsign
            tvHeadsign.text = departure.tripHeadsign ?: ""
            tvHeadsign.visibility = if (departure.tripHeadsign.isNullOrEmpty()) View.GONE else View.VISIBLE

            // Arrival time - format HH:MM
            val timeFormatted = formatTime(departure.arrivalTime)
            tvArrivalTime.text = timeFormatted
        }

        private fun formatTime(time: String): String {
            return try {
                // GTFS time format: HH:MM:SS
                val parts = time.split(":")
                if (parts.size >= 2) {
                    "${parts[0]}:${parts[1]}"
                } else {
                    time
                }
            } catch (_: Exception) {
                time
            }
        }
    }

    class DepartureDiffCallback : DiffUtil.ItemCallback<DepartureInfo>() {
        override fun areItemsTheSame(oldItem: DepartureInfo, newItem: DepartureInfo): Boolean {
            return oldItem.tripId == newItem.tripId && oldItem.stopSequence == newItem.stopSequence
        }

        override fun areContentsTheSame(oldItem: DepartureInfo, newItem: DepartureInfo): Boolean {
            return oldItem == newItem
        }
    }
}
