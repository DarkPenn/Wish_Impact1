package com.example.wishimpacttest

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter này giờ nhận đầu vào là InventoryGroup (Thùng hàng)
class InventoryAdapter(
    private val list: List<ItemsGroup>,
    private val onItemClick: (ItemsGroup) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutBgRarity: RelativeLayout = view.findViewById(R.id.layoutBgRarity)
        val tvGridItemName: TextView = view.findViewById(R.id.tvGridItemName)
        val tvGridStars: TextView = view.findViewById(R.id.tvGridStars)
        val tvItemQuantity: TextView = view.findViewById(R.id.tvItemQuantity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_inventory, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val group = list[position]
        val item = group.sampleItem

        holder.tvGridItemName.text = item.name
        holder.tvGridStars.text = "★".repeat(item.rarity.stars)

        holder.tvItemQuantity.text = "${group.totalCount}"
        if (group.totalCount > 1) {
            holder.tvItemQuantity.visibility = View.VISIBLE
        } else {
            holder.tvItemQuantity.visibility = View.INVISIBLE
        }

        holder.layoutBgRarity.setBackgroundColor(Color.parseColor(item.rarity.colorHex))

        holder.itemView.setOnClickListener {
            onItemClick(group) // Bắn cái Thùng hàng ra ngoài khi click
        }
    }
    override fun getItemCount() = list.size
}