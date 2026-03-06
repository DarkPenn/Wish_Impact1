package com.example.wishimpacttest

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

//  chuyển đổi dữ liệu từ danh sách thành các ô vuông
class InventoryAdapter(
    private val list: List<WishHistory>,
    private val onItemClick: (WishHistory) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.ViewHolder>() {

    // ViewHolder lưu trữ các View.
    // ánh xạ một lần duy nhất cho mỗi ô vuông để tái sử dụng khi cuộn
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutBgRarity: RelativeLayout = view.findViewById(R.id.layoutBgRarity) // Nền của ô vuông
        val tvGridItemName: TextView = view.findViewById(R.id.tvGridItemName)       // Tên vật phẩm
        val tvGridStars: TextView = view.findViewById(R.id.tvGridStars)             // Số sao
    }

    // Được hệ thống tự động gọi khi cần tạo ra giao diện cho một ô vuông mới
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // "Bơm" (inflate) file thiết kế XML (item_inventory.xml) thành một View thực sự trên code
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_inventory, parent, false)
        return ViewHolder(view)
    }

    // Được gọi để thêm vào holder tương ứng với vị trí trong danh sách.
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position] // Lấy món đồ ở đúng vị trí hiện tại

        holder.tvGridItemName.text = item.name
        holder.tvGridStars.text = "${item.rarity.stars} ★"

        // Tô màu nền của ô vuông và chữ sao theo độ hiếm
        holder.layoutBgRarity.setBackgroundColor(Color.parseColor(item.rarity.colorHex))
        holder.tvGridStars.setTextColor(Color.parseColor(item.rarity.colorHex))

        // Thiết lập sự kiện click cho mỗi ô vuông
        holder.itemView.setOnClickListener {
            onItemClick(item) // Truyền thẳng dữ liệu món đồ đó ra ngoài
        }
    }

    //hàm báo cáo số lượng items trong danh sách có tổng cộng bao nhiêu món
    override fun getItemCount() = list.size
}