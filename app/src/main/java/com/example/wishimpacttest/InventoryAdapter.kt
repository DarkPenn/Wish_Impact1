package com.example.wishimpacttest

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class InventoryAdapter(
    private val list: List<WishHistory>,
    private val onItemClick: (WishHistory) -> Unit // Đường dây truyền tín hiệu khi click
) : RecyclerView.Adapter<InventoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutBgRarity: RelativeLayout = view.findViewById(R.id.layoutBgRarity)
        val tvGridItemName: TextView = view.findViewById(R.id.tvGridItemName)
        val tvGridStars: TextView = view.findViewById(R.id.tvGridStars)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_inventory, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        // Gán chữ (Tên và số sao)
        holder.tvGridItemName.text = item.name
        holder.tvGridStars.text = "${item.rarity.stars} ★"

        // Tô màu nền của ô vuông và chữ sao theo độ hiếm
        holder.layoutBgRarity.setBackgroundColor(Color.parseColor(item.rarity.colorHex))
        holder.tvGridStars.setTextColor(Color.parseColor(item.rarity.colorHex))

        // BẮT SỰ KIỆN CLICK: Khi bấm vào ô này, truyền món đồ đó ra ngoài
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = list.size
}