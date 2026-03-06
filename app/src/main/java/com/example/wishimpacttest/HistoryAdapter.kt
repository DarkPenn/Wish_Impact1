package com.example.wishimpacttest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Lớp Adapter này dùng để hiển thị danh sách lịch sử quay gacha
class HistoryAdapter(private var list: List<WishHistory>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    // ViewHolder giúp giữ các View trong một dòng của danh sách
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {


        val tvSTT: TextView = view.findViewById(R.id.tvSTT)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvRarity: TextView = view.findViewById(R.id.tvRarity)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Tạo ra một dòng mới cho bảng lịch sử dựa trên bố cục XML đã thiết kế
        val view = LayoutInflater.from(parent.context).inflate(R.layout.items_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Đổ dữ liệu từ danh sách vào từng dòng tương ứng
        val item = list[position]

        holder.tvSTT.text = item.stt.toString()
        holder.tvName.text = item.name
        holder.tvTime.text = item.time

        // Lấy số sao từ Rarity ghép với ký tự ★ để hiển thị chữ
        holder.tvRarity.text = "${item.rarity.stars} ★"



        // Lấy mã màu từ Rarity để tô màu cho chữ
        holder.tvRarity.setTextColor(android.graphics.Color.parseColor(item.rarity.colorHex))
    }

    override fun getItemCount() = list.size

    // Hàm cập nhật lại danh sách khi có dữ liệu mới (ví dụ khi chuyển trang)
    fun updateData(newList: List<WishHistory>) {
        list = newList
        notifyDataSetChanged()
    }
}