package com.example.wishimpacttest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(private var list: List<WishHistory>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSTT: TextView = view.findViewById(R.id.tvSTT)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvRarity: TextView = view.findViewById(R.id.tvRarity)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Đã sửa lại thành item_history chuẩn xác
        val view = LayoutInflater.from(parent.context).inflate(R.layout.items_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvSTT.text = item.stt.toString()
        holder.tvName.text = item.name
        holder.tvRarity.text = item.rarity
        holder.tvTime.text = item.time
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<WishHistory>) {
        list = newList
        notifyDataSetChanged()
    }
}